package io.github.transactionbridge;

/** Pure parser seam; implementations do not depend on Android classes. */
public interface NotificationParser {
    Transaction parse(long occurredAt, String rawText);
}
