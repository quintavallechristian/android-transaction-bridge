package io.github.transactionbridge;

import java.io.IOException;

/** Processes only the FIFO head; callers can schedule another pass after the result. */
public final class WebhookUploader {
    public interface AttentionLog {
        void record(DeliveryRecord item, String reason);
    }

    private final PersistentDeliveryQueue queue;
    private final WebhookClient client;
    private final AttentionLog attentionLog;

    public WebhookUploader(PersistentDeliveryQueue queue, WebhookClient client, AttentionLog attentionLog) {
        if (queue == null || client == null) throw new IllegalArgumentException("queue and client are required");
        this.queue = queue;
        this.client = client;
        this.attentionLog = attentionLog;
    }

    public DeliveryOutcome deliverNext(String endpoint, String bearerToken, long nowMillis) {
        DeliveryRecord item = queue.peek();
        if (item == null) return DeliveryOutcome.of(DeliveryOutcome.State.EMPTY, 0, "queue is empty");
        if (!queue.ready(nowMillis)) return DeliveryOutcome.of(DeliveryOutcome.State.RETRY_SCHEDULED, item.nextAttemptAt - nowMillis, "not ready");
        try {
            WebhookClient.Response response = client.post(endpoint, item.payload, bearerToken);
            RetryPolicy.Action action = RetryPolicy.classify(response.statusCode);
            if (action == RetryPolicy.Action.SUCCESS) {
                queue.removeFirst();
                return DeliveryOutcome.of(DeliveryOutcome.State.DELIVERED, 0, "ok");
            }
            if (action == RetryPolicy.Action.SUSPEND) {
                return DeliveryOutcome.of(DeliveryOutcome.State.SUSPENDED, 0, "HTTP " + response.statusCode);
            }
            if (action == RetryPolicy.Action.ATTENTION) {
                DeliveryRecord removed = queue.removeFirst();
                if (attentionLog != null) attentionLog.record(removed, "HTTP " + response.statusCode);
                return DeliveryOutcome.of(DeliveryOutcome.State.NEEDS_ATTENTION, 0, "HTTP " + response.statusCode);
            }
            long delay = queue.deferFirst(nowMillis, response.retryAfter);
            return DeliveryOutcome.of(DeliveryOutcome.State.RETRY_SCHEDULED, delay, "HTTP " + response.statusCode);
        } catch (IOException | RuntimeException error) {
            long delay = queue.deferFirst(nowMillis, null);
            return DeliveryOutcome.of(DeliveryOutcome.State.RETRY_SCHEDULED, delay, error.getMessage() == null ? "temporary delivery failure" : error.getMessage());
        }
    }
}
