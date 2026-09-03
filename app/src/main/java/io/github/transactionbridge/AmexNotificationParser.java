package io.github.transactionbridge;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AmexNotificationParser implements NotificationParser {
    private static final Pattern PAYMENT = Pattern.compile("^(.+?) ([0-9][0-9.,]*) €$");

    @Override public Transaction parse(long occurredAt, String rawText) {
        String text = ParserSupport.normalize(rawText);
        Matcher matcher = PAYMENT.matcher(text);
        if (!matcher.find()) return null;
        try {
            BigDecimal amount = ParserSupport.amount(matcher.group(2));
            String merchant = matcher.group(1).trim();
            return amount.signum() > 0 && !merchant.isEmpty()
                    ? new Transaction(occurredAt, amount, "EUR", merchant, text, "amex-notification")
                    : null;
        } catch (NumberFormatException invalidAmount) {
            return null;
        }
    }
}
