# Transaction Bridge

Transaction Bridge is a small Android app that turns supported payment notifications into versioned JSON and delivers them to a webhook you control.

## Install and configure

1. Build and install the debug APK, or install a release from GitHub Releases.
2. Create an HTTPS receiver by following the [webhook guide](docs/WEBHOOK.md), then open **Settings** and enter its complete URL. A Bearer token is optional.
3. Choose `minimal` (the default) or `full` payload mode. Full mode includes the original notification text, which may contain personal information.
4. Select the notification sources to parse.
5. For Google Wallet, enter the last four card digits and a local card name, then select **Add or update card**.
6. Open **Notification access** and enable Transaction Bridge.

Pending deliveries are stored locally in FIFO order. Supported HTTP `2xx` responses remove an item; temporary failures are retried with bounded backoff. The app has no account, analytics, crash reporting, or telemetry service.

## Supported notification formats

Transaction Bridge currently supports the following notification sources. The parser reads the notification title, text, and expanded text, joins them, normalizes whitespace, and ignores notifications that do not match one of these formats.

### Crypto.com

- Card payments: `12.50 EUR spent at Merchant Name`
- EUR deposits: `You successfully deposited EUR 100.00 into your EUR Account`

### IsyBank

- Direct debits: `È stato addebitato il pagamento di una domiciliazione di 12,50 € da parte di Merchant Name sul conto ... in data 15.08.2026`
- Instant or European transfers: `È stato inserito un bonifico istantaneo di 250,00 € dal conto ... in favore dell'IBAN IT... in data 15.08.2026 alle ore 10:30`

For transfers, the merchant is recorded as `Bonifico` followed by the IBAN. The date and time in the notification are used as the transaction time.

### ING

- Authorized card payments: `Operazione autorizzata: 12,50 euro, Merchant Name. Non sei stato tu?`
- Direct debits: `Addebito diretto di 12,50 euro richiesto da Creditor id. ABC123 Merchant Name: pagato!`

### Revolut

- Card payments: `Merchant Name Hai speso 12,50 €`

An optional suffix such as `Saldo di ...` is accepted. Transfers are not currently supported by Transaction Bridge.

### BBVA

- Accepted card payments: `Il pagamento di 12,50 EUR in data Merchant Name effettuato con la tua carta 1234 è stato accettato.`

### HYPE

- Card payments: `Merchant Name, City 12,50 €`

### American Express

- Card payments: `Merchant Name 12,50 €`

### Google Wallet

- Card payments: `Merchant Name 12,50 € con ... •••• 1234`

The final four card digits must be associated with a local card name in Settings. Notifications from unknown cards are ignored.

The card name is used to disambiguate which local account or provider owns the card and becomes part of the webhook `source`, for example `google-wallet-personal-ing-notification` or `google-wallet-crypto-notification`. Only the last four digits and the chosen local name are stored; the full card number is never requested.

Google Wallet notifications are not reconciled with notifications from the underlying bank or card provider. If both sources report the same payment, they produce different `source` and transaction IDs and may therefore be delivered as two separate transactions.

## Google Wallet

Google Wallet notifications identify the payment card by its last four digits. Transaction Bridge compares those digits with the cards configured on the device and uses the corresponding name in the webhook `source`, for example `google-wallet-personal-ing-notification`.

Only the last four digits and the name chosen by the user are stored. Full card numbers are never requested. A Wallet notification from an unknown card is ignored. Card mappings remain on the device, and the default `minimal` payload does not include the original notification text.

## Build

```sh
./gradlew testDebugUnitTest assembleDebug
```

The project requires Android SDK 36 and Java 17. The package name is `io.github.transactionbridge`.

## Parser library

The Android-independent parsers live in [transaction-parsers](https://github.com/quintavallechristian/transaction-parsers):

```kotlin
implementation("io.github.quintavallechristian:transaction-parsers:0.1.0")
```

GitHub Packages requires a GitHub username and a token with `read:packages` in the consuming project's Maven repository credentials.

## Webhook payload

The default payload contains `version`, `id`, `source`, `occurredAt`, `amount`, `currency`, and `merchant`. The ID is stable across delivery retries so webhook consumers can safely deduplicate them. A notification reposted by Android may have a new ID and should be treated as a separate observation. `rawText` is added only in `full` mode. See [Receiving webhook data](docs/WEBHOOK.md) for the request contract, response handling, retries, and a receiver outline.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md), [Adding a notification provider](docs/ADDING_A_PROVIDER.md), [SECURITY.md](SECURITY.md), and [PRIVACY.md](PRIVACY.md).
