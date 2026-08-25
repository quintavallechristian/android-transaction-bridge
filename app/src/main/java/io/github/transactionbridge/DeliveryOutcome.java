package io.github.transactionbridge;

public final class DeliveryOutcome {
    public enum State { EMPTY, DELIVERED, RETRY_SCHEDULED, SUSPENDED, NEEDS_ATTENTION }

    public final State state;
    public final long retryDelayMillis;

    private DeliveryOutcome(State state, long retryDelayMillis) {
        this.state = state;
        this.retryDelayMillis = retryDelayMillis;
    }

    static DeliveryOutcome of(State state, long retryDelayMillis) {
        return new DeliveryOutcome(state, retryDelayMillis);
    }
}
