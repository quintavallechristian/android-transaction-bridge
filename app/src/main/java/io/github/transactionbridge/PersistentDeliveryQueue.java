package io.github.transactionbridge;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/** FIFO durable queue. A record is removed only after a 2xx response. */
public final class PersistentDeliveryQueue {
    public interface Store {
        String read();
        void write(String value);
    }

    public static Store preferences(Context context, String name) {
        final SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
        return new Store() {
            @Override public String read() { return preferences.getString("queue", "[]"); }
            @Override public void write(String value) { if (!preferences.edit().putString("queue", value).commit()) throw new IllegalStateException("queue could not be persisted"); }
        };
    }

    private final Store store;

    public PersistentDeliveryQueue(Store store) {
        if (store == null) throw new IllegalArgumentException("store is required");
        this.store = store;
    }

    public synchronized boolean enqueue(String id, String payload) {
        if (id == null || id.trim().isEmpty() || payload == null || payload.trim().isEmpty()) throw new IllegalArgumentException("id and payload are required");
        List<DeliveryRecord> items = read();
        for (DeliveryRecord item : items) if (id.equals(item.id)) return false;
        items.add(new DeliveryRecord(id, payload, 0, 0));
        write(items);
        return true;
    }

    public synchronized DeliveryRecord peek() {
        List<DeliveryRecord> items = read();
        return items.isEmpty() ? null : items.get(0);
    }

    public synchronized boolean ready(long nowMillis) {
        DeliveryRecord item = peek();
        return item != null && RetryPolicy.ready(item.nextAttemptAt, nowMillis);
    }

    public synchronized long deferFirst(long nowMillis, String retryAfter) {
        List<DeliveryRecord> items = read();
        if (items.isEmpty()) return 0;
        DeliveryRecord first = items.get(0);
        int attempts = first.attempts;
        long delay = RetryPolicy.delayMillis(attempts, retryAfter, nowMillis);
        items.set(0, new DeliveryRecord(first.id, first.payload, attempts + 1, nowMillis + delay));
        write(items);
        return delay;
    }

    public synchronized boolean retryFirstNow() {
        List<DeliveryRecord> items = read();
        if (items.isEmpty()) return false;
        DeliveryRecord first = items.get(0);
        items.set(0, new DeliveryRecord(first.id, first.payload, first.attempts, 0));
        write(items);
        return true;
    }

    public synchronized DeliveryRecord removeFirst() {
        List<DeliveryRecord> items = read();
        if (items.isEmpty()) return null;
        DeliveryRecord removed = items.remove(0);
        write(items);
        return removed;
    }

    public synchronized int size() { return read().size(); }

    public synchronized Set<String> ids() {
        Set<String> ids = new LinkedHashSet<>();
        for (DeliveryRecord item : read()) ids.add(item.id);
        return ids;
    }

    private List<DeliveryRecord> read() {
        String value = store.read();
        List<DeliveryRecord> result = new ArrayList<>();
        if (value == null || value.isEmpty() || "[]".equals(value)) return result;
        String[] lines = value.split("\\n", -1);
        for (String line : lines) {
            if (line.isEmpty()) continue;
            String[] fields = RecordCodec.fields(line, 4, "queue data is corrupt");
            try {
                result.add(new DeliveryRecord(RecordCodec.decode(fields[0]), RecordCodec.decode(fields[3]), Integer.parseInt(fields[1]), Long.parseLong(fields[2])));
            } catch (IllegalArgumentException error) {
                throw new IllegalStateException("queue data is corrupt", error);
            }
        }
        return result;
    }

    private void write(List<DeliveryRecord> items) {
        StringBuilder value = new StringBuilder();
        for (DeliveryRecord item : items) value.append(RecordCodec.encode(item.id)).append('|').append(item.attempts).append('|').append(item.nextAttemptAt).append('|').append(RecordCodec.encode(item.payload)).append('\n');
        store.write(value.toString());
    }
}
