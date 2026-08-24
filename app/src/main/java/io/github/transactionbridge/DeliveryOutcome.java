package io.github.transactionbridge;

public final class DeliveryOutcome {
    public enum State { EMPTY, DELIVERED, RETRY_SCHEDULED, SUSPENDED, NEEDS_ATTENTION }

    public final State state;
    public final long retryDelayMillis;
    public final String message;

    private DeliveryOutcome(State state, long retryDelayMillis, String message) {
        this.state = state;
        this.retryDelayMillis = retryDelayMillis;
        this.message = message;
    }

    static DeliveryOutcome of(State state, long retryDelayMillis, String message) {
        return new DeliveryOutcome(state, retryDelayMillis, message);
    }
}
