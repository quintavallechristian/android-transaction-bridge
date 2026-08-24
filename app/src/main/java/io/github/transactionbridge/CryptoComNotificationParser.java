package io.github.transactionbridge;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CryptoComNotificationParser implements NotificationParser {
    private static final Pattern PURCHASE = Pattern.compile(
            "(?:€\\s*)?([0-9][0-9.,]*)\\s+(EUR)\\s+spent\\s+at\\s+(.+?)(?=\\s+You\\s+earned\\b|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DEPOSIT = Pattern.compile(
            "You successfully deposited EUR ([0-9][0-9.,]*) into your EUR Account",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override public Transaction parse(long occurredAt, String rawText) {
        String text = ParserSupport.normalize(rawText);
        Matcher purchase = PURCHASE.matcher(text);
        if (purchase.find()) {
            try {
                BigDecimal amount = ParserSupport.amount(purchase.group(1));
                String merchant = purchase.group(3).trim();
                return amount.signum() > 0 && !merchant.isEmpty()
                        ? new Transaction(occurredAt, amount, purchase.group(2).toUpperCase(Locale.ROOT), merchant,
                        text, "crypto.com-notification") : null;
            } catch (NumberFormatException invalidAmount) {
                return null;
            }
        }
        Matcher deposit = DEPOSIT.matcher(text);
        if (!deposit.find()) return null;
        try {
            BigDecimal amount = ParserSupport.amount(deposit.group(1));
            return amount.signum() > 0
                    ? new Transaction(occurredAt, amount, "EUR", "Crypto.com", text,
                    "crypto-deposit-transfer-notification") : null;
        } catch (NumberFormatException invalidAmount) {
            return null;
        }
    }
}
