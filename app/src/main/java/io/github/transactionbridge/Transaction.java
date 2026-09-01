package io.github.transactionbridge;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;

/** A validated, immutable transaction produced by a notification parser. */
public final class Transaction {
    public final String id;
    public final long occurredAt;
    public final BigDecimal amount;
    public final String currency;
    public final String merchant;
    public final String rawText;
    public final String source;

    public Transaction(long occurredAt, BigDecimal amount, String currency,
                       String merchant, String rawText, String source) {
        this(occurredAt, amount, currency, merchant, rawText, source, Long.toString(occurredAt));
    }

    Transaction(long occurredAt, BigDecimal amount, String currency,
                String merchant, String rawText, String source, String identityDiscriminator) {
        if (occurredAt < 0) throw new IllegalArgumentException("occurredAt must not be negative");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        if (currency == null || currency.trim().isEmpty()) throw new IllegalArgumentException("currency is required");
        if (merchant == null || merchant.trim().isEmpty()) throw new IllegalArgumentException("merchant is required");
        if (source == null || source.trim().isEmpty()) throw new IllegalArgumentException("source is required");
        if (identityDiscriminator == null || identityDiscriminator.isEmpty()) throw new IllegalArgumentException("identity discriminator is required");
        this.occurredAt = occurredAt;
        this.amount = amount;
        this.currency = currency.trim().toUpperCase(Locale.ROOT);
        this.merchant = merchant.trim();
        this.rawText = rawText == null ? "" : rawText.trim();
        this.source = source.trim();
        // rawText is intentionally excluded because minimal payloads never contain it.
        this.id = sha256(identityDiscriminator + "|" + this.amount.toPlainString() + "|"
                + this.currency + "|" + this.merchant + "|" + this.source);
    }

    public String occurredAtIso() {
        return Instant.ofEpochMilli(occurredAt).toString();
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) result.append(String.format(Locale.ROOT, "%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
