package io.github.transactionbridge;

import org.junit.Test;

import static org.junit.Assert.*;

public final class AttentionLogTest {
    @Test public void recordsCountsRequeuesAndDeletesDurably() {
        MemoryStore store = new MemoryStore();
        AttentionLog log = new AttentionLog(store);
        log.record(new DeliveryRecord("id", "{\"id\":\"id\"}", 0, 0), "HTTP 422");
        assertEquals(1, log.count());
        assertEquals("HTTP 422", log.entries().get(0).reason);

        PersistentDeliveryQueue queue = new PersistentDeliveryQueue(new MemoryStore());
        assertTrue(log.requeueFirst(queue));
        assertEquals(0, log.count());
        assertEquals("id", queue.peek().id);
        log.record(new DeliveryRecord("id", "{}", 0, 0), "bad");
        assertTrue(log.deleteFirst());
        assertEquals(0, log.count());
    }

    private static final class MemoryStore implements AttentionLog.Store, PersistentDeliveryQueue.Store {
        private String value = "";
        @Override public String read() { return value; }
        @Override public void write(String value) { this.value = value; }
    }
}
