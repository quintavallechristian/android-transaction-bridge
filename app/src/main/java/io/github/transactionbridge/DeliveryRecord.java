package io.github.transactionbridge;

public final class DeliveryRecord {
    public final String id;
    public final String payload;
    public final int attempts;
    public final long nextAttemptAt;

    DeliveryRecord(String id, String payload, int attempts, long nextAttemptAt) {
        this.id = id;
        this.payload = payload;
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
    }
}
