package io.github.transactionbridge;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CoverflexNotificationParser implements NotificationParser {
    private static final Pattern PAYMENT = Pattern.compile(
            "Pagamento di €([0-9][0-9.,]*) confermato.*Hai speso €([0-9][0-9.,]*) da (?:Sumup\\s*\\*\\s*)?(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override public Transaction parse(long occurredAt, String rawText) {
        String text = ParserSupport.normalize(rawText);
        Matcher matcher = PAYMENT.matcher(text);
        if (!matcher.find()) return null;
        try {
            BigDecimal confirmed = ParserSupport.amount(matcher.group(1));
            BigDecimal spent = ParserSupport.amount(matcher.group(2));
            String merchant = matcher.group(3).trim();
            return spent.signum() > 0 && spent.compareTo(confirmed) == 0 && !merchant.isEmpty()
                    ? new Transaction(occurredAt, spent, "EUR", merchant, text, "coverflex-notification")
                    : null;
        } catch (NumberFormatException invalidAmount) {
            return null;
        }
    }
}
