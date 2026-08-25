package io.github.transactionbridge;

import org.junit.Test;

import static org.junit.Assert.*;

public final class DeliveryEngineTest {
    @Test public void queueIsFifoAndDeduplicatesAcrossInstances() {
        MemoryStore store = new MemoryStore();
        PersistentDeliveryQueue first = new PersistentDeliveryQueue(store);
        assertTrue(first.enqueue("a", "{\"id\":\"a\"}"));
        assertFalse(first.enqueue("a", "different"));
        assertTrue(first.enqueue("b", "{\"id\":\"b\"}"));

        PersistentDeliveryQueue reopened = new PersistentDeliveryQueue(store);
        assertEquals("a", reopened.peek().id);
        assertEquals("{\"id\":\"a\"}", reopened.removeFirst().payload);
        assertEquals("b", reopened.peek().id);
    }

    @Test public void retryDefersOnlyHeadAndUsesBoundedRetryAfter() {
        MemoryStore store = new MemoryStore();
        PersistentDeliveryQueue queue = new PersistentDeliveryQueue(store);
        queue.enqueue("a", "{}");
        queue.enqueue("b", "{}");
        assertEquals(7_200_000L, queue.deferFirst(10_000L, "999999"));
        assertFalse(queue.ready(10_000L));
        queue.removeFirst();
        assertEquals("b", queue.peek().id);
        assertEquals(RetryPolicy.Action.SUSPEND, RetryPolicy.classify(403));
        assertEquals(RetryPolicy.Action.ATTENTION, RetryPolicy.classify(422));
        assertEquals(RetryPolicy.Action.RETRY, RetryPolicy.classify(503));
    }

    @Test public void minimalPayloadOmitsOriginalNotification() throws Exception {
        String minimal = WebhookPayload.createJson("id", "source", "1970-01-01T00:00:00Z", "12.50", "EUR", "Shop", "private text", PayloadMode.MINIMAL);
        String full = WebhookPayload.createJson("id", "source", "1970-01-01T00:00:00Z", "12.50", "EUR", "Shop", "private text", PayloadMode.FULL);
        assertFalse(minimal.contains("rawText"));
        assertTrue(full.contains("\"rawText\":\"private text\""));
        assertTrue(minimal.startsWith("{\"version\":1"));
    }

    private static final class MemoryStore implements PersistentDeliveryQueue.Store {
        private String value = "[]";
        @Override public String read() { return value; }
        @Override public void write(String value) { this.value = value; }
    }
}
