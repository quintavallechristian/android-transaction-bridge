package io.github.transactionbridge;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses card payments and completed outgoing transfers. */
public final class RevolutNotificationParser implements NotificationParser {
    private static final Pattern CARD_PAYMENT = Pattern.compile(
            "^(.+?)\\s+(?:[^a-zA-Z0-9]*\\s*)?Hai speso ([0-9][0-9.,]*) €(?:\\s+Saldo di .+)?$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SENT_TRANSFER = Pattern.compile(
            "Hai inviato ([0-9][0-9.,]*) € a (.+?)\\. Arriverà in pochi secondi$",
            Pattern.CASE_INSENSITIVE);

    @Override public Transaction parse(long occurredAt, String rawText) {
        String text = ParserSupport.normalize(rawText);
        Matcher matcher = CARD_PAYMENT.matcher(text);
        boolean transfer = !matcher.find();
        if (transfer) {
            matcher = SENT_TRANSFER.matcher(text);
            if (!matcher.find()) return null;
        }
        try {
            BigDecimal amount = ParserSupport.amountWithDotThousands(matcher.group(transfer ? 1 : 2));
            String merchant = matcher.group(transfer ? 2 : 1).trim();
            return amount.signum() > 0 && !merchant.isEmpty()
                    ? new Transaction(occurredAt, amount, "EUR", merchant, text, "revolut-notification")
                    : null;
        } catch (NumberFormatException invalidAmount) {
            return null;
        }
    }
}
