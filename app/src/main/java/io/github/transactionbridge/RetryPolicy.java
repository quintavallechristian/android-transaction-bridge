package io.github.transactionbridge;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class RetryPolicy {
    public enum Action { SUCCESS, RETRY, SUSPEND, ATTENTION }

    private static final long[] BACKOFF_MS = {15_000L, 60_000L, 300_000L, 1_800_000L, 7_200_000L};
    private static final long MAX_DELAY_MS = 7_200_000L;

    private RetryPolicy() {}

    public static Action classify(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return Action.SUCCESS;
        if (statusCode == 401 || statusCode == 403) return Action.SUSPEND;
        if (statusCode == 408 || statusCode == 429 || statusCode >= 500) return Action.RETRY;
        if (statusCode >= 400 && statusCode < 500) return Action.ATTENTION;
        return Action.RETRY;
    }

    public static long delayMillis(int attempt, String retryAfter, long nowMillis) {
        long normal = BACKOFF_MS[Math.min(Math.max(attempt, 0), BACKOFF_MS.length - 1)];
        long hinted = retryAfterMillis(retryAfter, nowMillis);
        return Math.min(MAX_DELAY_MS, Math.max(normal, hinted));
    }

    public static boolean ready(long nextAttemptAt, long nowMillis) {
        return nextAttemptAt <= nowMillis;
    }

    private static long retryAfterMillis(String value, long nowMillis) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds <= 0 ? 0 : seconds >= MAX_DELAY_MS / 1000L ? MAX_DELAY_MS : seconds * 1000L;
        } catch (NumberFormatException ignored) {
            for (String pattern : new String[]{"EEE, dd MMM yyyy HH:mm:ss z", "EEEE, dd-MMM-yy HH:mm:ss z", "EEE MMM d HH:mm:ss yyyy"}) {
                try {
                    SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                    format.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Date date = format.parse(value.trim());
                    if (date == null) return 0;
                    return Math.min(MAX_DELAY_MS, Math.max(0, date.getTime() - nowMillis));
                } catch (ParseException ignoredDate) {
                    // Try the next HTTP-date spelling.
                }
            }
            return 0;
        }
    }
}
