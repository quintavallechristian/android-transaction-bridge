package io.github.transactionbridge;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IsyBankNotificationParser implements NotificationParser {
    private static final Pattern DIRECT_DEBIT = Pattern.compile(
            "(?:E'|È) stato addebitato il pagamento di una domiciliazione di ([0-9][0-9.,]*) € da parte di (.+?) sul conto .+? in data (\\d{2}\\.\\d{2}\\.\\d{4})",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TRANSFER = Pattern.compile(
            "(?:E'|È) stato inserito (?:il pagamento di )?un bonifico (?:istantaneo|europeo) di ([0-9][0-9.,]*) € dal conto .+? in favore dell'IBAN (\\S+) (?:in data|il) (\\d{2}\\.\\d{2}\\.\\d{4}) alle ore (\\d{2}:\\d{2})(?: con data di addebito il (\\d{2}\\.\\d{2}\\.\\d{4}))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm");

    @Override public Transaction parse(long notificationOccurredAt, String rawText) {
        String text = ParserSupport.normalize(rawText);
        Matcher matcher = DIRECT_DEBIT.matcher(text);
        boolean instantTransfer = false;
        if (!matcher.find()) {
            matcher = TRANSFER.matcher(text);
            if (!matcher.find()) return null;
            instantTransfer = true;
        }
        try {
            BigDecimal amount = ParserSupport.amount(matcher.group(1));
            if (amount.signum() <= 0) return null;
            long occurredAt = instantTransfer
                    ? LocalDateTime.parse((matcher.group(5) == null ? matcher.group(3) : matcher.group(5)) + " " + matcher.group(4), DATE_TIME)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : LocalDate.parse(matcher.group(3), DATE)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            String merchant = instantTransfer ? "Bonifico " + matcher.group(2) : matcher.group(2).trim();
            return new Transaction(occurredAt, amount, "EUR", merchant, text,
                    "isybank-notification", Long.toString(notificationOccurredAt));
        } catch (NumberFormatException | DateTimeParseException invalidNotification) {
            return null;
        }
    }
}
