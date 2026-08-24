package io.github.transactionbridge;

import org.json.JSONException;
import org.json.JSONObject;

/** Builds the versioned wire object without leaking queue metadata. */
public final class WebhookPayload {
    private WebhookPayload() {}

    public static JSONObject from(Transaction transaction, PayloadMode mode) throws JSONException {
        if (transaction == null) throw new IllegalArgumentException("transaction is required");
        return create(transaction.id, transaction.source, transaction.occurredAtIso(),
                transaction.amount.toPlainString(), transaction.currency, transaction.merchant,
                transaction.rawText, mode);
    }

    public static String createJson(
            String id,
            String source,
            String occurredAt,
            String amount,
            String currency,
            String merchant,
            String rawText,
            PayloadMode mode) {
        if (mode == null) throw new IllegalArgumentException("payload mode is required");
        StringBuilder json = new StringBuilder(160)
                .append("{\"version\":1")
                .append(",\"id\":").append(quoted(required(id, "id")))
                .append(",\"source\":").append(quoted(required(source, "source")))
                .append(",\"occurredAt\":").append(quoted(required(occurredAt, "occurredAt")))
                .append(",\"amount\":").append(quoted(required(amount, "amount")))
                .append(",\"currency\":").append(quoted(required(currency, "currency")))
                .append(",\"merchant\":").append(quoted(required(merchant, "merchant")));
        if (mode == PayloadMode.FULL) json.append(",\"rawText\":").append(quoted(rawText == null ? "" : rawText));
        return json.append('}').toString();
    }

    public static JSONObject create(
            String id,
            String source,
            String occurredAt,
            String amount,
            String currency,
            String merchant,
            String rawText,
            PayloadMode mode) throws JSONException {
        if (mode == null) throw new IllegalArgumentException("payload mode is required");
        JSONObject payload = new JSONObject()
                .put("version", 1)
                .put("id", required(id, "id"))
                .put("source", required(source, "source"))
                .put("occurredAt", required(occurredAt, "occurredAt"))
                .put("amount", required(amount, "amount"))
                .put("currency", required(currency, "currency"))
                .put("merchant", required(merchant, "merchant"));
        if (mode == PayloadMode.FULL) payload.put("rawText", rawText == null ? "" : rawText);
        return payload;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String quoted(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"': result.append("\\\""); break;
                case '\\': result.append("\\\\"); break;
                case '\b': result.append("\\b"); break;
                case '\f': result.append("\\f"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\t': result.append("\\t"); break;
                default:
                    if (character < 0x20) result.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) character));
                    else result.append(character);
            }
        }
        return result.append('"').toString();
    }
}
