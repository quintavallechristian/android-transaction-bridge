package io.github.transactionbridge;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IngNotificationParser implements NotificationParser {
    private static final Pattern PAYMENT = Pattern.compile(
            "Operazione autorizzata:\\s*([0-9][0-9.,]*) euro,\\s*(.+?)(?=\\. Non sei stato tu\\?|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DIRECT_DEBIT = Pattern.compile(
            "Addebito diretto di\\s*([0-9][0-9.,]*) euro richiesto da\\s+Creditor id\\.\\s+\\S+\\s+"
                    + "(.+?):\\s*pagato!(?=\\s|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override public Transaction parse(long occurredAt, String rawText) {
        String text = ParserSupport.normalize(rawText);
        Matcher matcher = PAYMENT.matcher(text);
        if (!matcher.find()) {
            matcher = DIRECT_DEBIT.matcher(text);
            if (!matcher.find()) return null;
        }
        try {
            BigDecimal amount = ParserSupport.amount(matcher.group(1));
            String merchant = matcher.group(2).trim();
            return amount.signum() > 0 && !merchant.isEmpty()
                    ? new Transaction(occurredAt, amount, "EUR", merchant, text, "ing-notification")
                    : null;
        } catch (NumberFormatException invalidAmount) {
            return null;
        }
    }
}
