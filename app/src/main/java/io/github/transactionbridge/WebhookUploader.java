package io.github.transactionbridge;

import java.io.IOException;
import java.util.function.BiConsumer;

/** Processes only the FIFO head; callers can schedule another pass after the result. */
public final class WebhookUploader {
    private final PersistentDeliveryQueue queue;
    private final WebhookClient client;
    private final BiConsumer<DeliveryRecord, String> attentionLog;

    public WebhookUploader(PersistentDeliveryQueue queue, WebhookClient client, BiConsumer<DeliveryRecord, String> attentionLog) {
        if (queue == null || client == null || attentionLog == null) {
            throw new IllegalArgumentException("queue, client and attention log are required");
        }
        this.queue = queue;
        this.client = client;
        this.attentionLog = attentionLog;
    }

    public DeliveryOutcome deliverNext(String endpoint, String bearerToken, long nowMillis) {
        DeliveryRecord item = queue.peek();
        if (item == null) return DeliveryOutcome.of(DeliveryOutcome.State.EMPTY, 0);
        if (!queue.ready(nowMillis)) return DeliveryOutcome.of(DeliveryOutcome.State.RETRY_SCHEDULED, item.nextAttemptAt - nowMillis);
        try {
            WebhookClient.Response response = client.post(endpoint, item.payload, bearerToken);
            RetryPolicy.Action action = RetryPolicy.classify(response.statusCode);
            if (action == RetryPolicy.Action.SUCCESS) {
                queue.removeFirst();
                return DeliveryOutcome.of(DeliveryOutcome.State.DELIVERED, 0);
            }
            if (action == RetryPolicy.Action.SUSPEND) {
                return DeliveryOutcome.of(DeliveryOutcome.State.SUSPENDED, 0);
            }
            if (action == RetryPolicy.Action.ATTENTION) {
                attentionLog.accept(item, "HTTP " + response.statusCode);
                queue.removeFirst();
                return DeliveryOutcome.of(DeliveryOutcome.State.NEEDS_ATTENTION, 0);
            }
            long delay = queue.deferFirst(nowMillis, response.retryAfter);
            return DeliveryOutcome.of(DeliveryOutcome.State.RETRY_SCHEDULED, delay);
        } catch (IOException | RuntimeException error) {
            long delay = queue.deferFirst(nowMillis, null);
            return DeliveryOutcome.of(DeliveryOutcome.State.RETRY_SCHEDULED, delay);
        }
    }
}
