package io.github.transactionbridge;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves a Wallet card's last four digits through user-provided local configuration. */
public final class GoogleWalletNotificationParser implements NotificationParser {
    private static final Pattern PAYMENT = Pattern.compile(
            "^(.+?)\\s+([0-9][0-9.,]*) € con .+?[•·]{2}\\s*(\\d{4})$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final Map<String, String> cards;

    public GoogleWalletNotificationParser(Map<String, String> cards) {
        this.cards = cards == null ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(cards));
    }

    @Override public Transaction parse(long occurredAt, String rawText) {
        String text = ParserSupport.normalize(rawText);
        Matcher matcher = PAYMENT.matcher(text);
        if (!matcher.find()) return null;
        String method = cards.get(matcher.group(3));
        if (method == null || method.trim().isEmpty()) return null;
        try {
            BigDecimal amount = ParserSupport.amount(matcher.group(2));
            String merchant = matcher.group(1).trim();
            String suffix = "crypto.com".equalsIgnoreCase(method.trim()) ? "crypto"
                    : method.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
            return amount.signum() > 0 && !merchant.isEmpty()
                    ? new Transaction(occurredAt, amount, "EUR", merchant, text,
                    "google-wallet-" + suffix + "-notification") : null;
        } catch (NumberFormatException invalidAmount) {
            return null;
        }
    }
}
