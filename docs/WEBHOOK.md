# Receiving webhook data

Transaction Bridge sends each supported payment notification as an HTTPS `POST` request to the URL configured in the app.

## Configure the endpoint

1. Create a publicly reachable HTTPS endpoint that accepts `POST` requests.
2. In Transaction Bridge, open **Settings** and paste its complete URL, including any path and query string.
3. Optionally enter a Bearer token. The app will send it as `Authorization: Bearer <token>`.
4. Keep the payload mode set to `minimal` unless the receiver needs the original notification text.
5. Save the settings, then enable Transaction Bridge under **Notification access**.

The request has these headers:

```http
Accept: application/json
Content-Type: application/json; charset=utf-8
Authorization: Bearer your-token
```

`Authorization` is omitted when no token is configured. Use a long, random token and compare it before processing the body. Never put credentials in the webhook URL.

## Payload

A `minimal` payload looks like this:

```json
{
  "version": 1,
  "id": "7ba31b8a...",
  "source": "ing-notification",
  "occurredAt": "2026-08-31T10:30:00Z",
  "amount": "12.50",
  "currency": "EUR",
  "merchant": "Example Market"
}
```

| Field | Meaning |
| --- | --- |
| `version` | Integer version of the webhook format. Reject or quarantine unsupported versions. |
| `id` | Identifier stable across delivery retries. Store it with a unique constraint to make processing idempotent. A separate Android notification post may have a new ID. |
| `source` | Parser and account source, such as `ing-notification` or `google-wallet-personal-ing-notification`. |
| `occurredAt` | Transaction time as an ISO 8601 UTC timestamp. |
| `amount` | Positive decimal amount encoded as a string to preserve decimal precision. |
| `currency` | Uppercase currency code, currently normally `EUR`. |
| `merchant` | Parsed merchant or transaction description. |
| `rawText` | Original normalized notification text, present only in `full` mode. It may contain personal information. |

Treat `id` as opaque. A receiver should validate the fields it uses and preserve `amount` as a decimal value rather than converting it through a binary floating-point number.

## Acknowledge delivery

Return any HTTP `2xx` status only after the payload has been stored durably. A short response is enough:

```http
HTTP/1.1 204 No Content
```

The app handles other results as follows:

- `401` or `403`: suspend delivery until the endpoint or token changes;
- `408`, `429`, `5xx`, network failures, and unexpected responses: retry with bounded backoff;
- other `4xx`: remove the item from the queue and show it in the app's attention log for manual retry.

`Retry-After` is honored for retryable responses, up to two hours. Delivery is FIFO, so one retrying item delays later items. The receiver must still deduplicate by `id`: a request can be delivered again if the receiver stores it but the network response does not reach the app.

## Receiver outline

The exact server framework does not matter. The handler needs only to authenticate, parse JSON, deduplicate, store, and acknowledge:

```js
async function receiveTransaction(request, database, expectedToken) {
  if (request.method !== "POST") return new Response(null, { status: 405 });
  if (request.headers.get("authorization") !== `Bearer ${expectedToken}`) {
    return new Response(null, { status: 401 });
  }

  const transaction = await request.json();
  if (transaction.version !== 1 || !transaction.id) {
    return new Response(null, { status: 400 });
  }

  await database.insertUnlessPresent(transaction.id, transaction);
  return new Response(null, { status: 204 });
}
```

`insertUnlessPresent` represents the database's native unique-key or upsert operation; it should not be implemented as a separate read followed by a write.

For a temporary inspection endpoint, use only anonymized test notifications and `minimal` mode. Third-party request inspectors receive everything sent to their URL.
