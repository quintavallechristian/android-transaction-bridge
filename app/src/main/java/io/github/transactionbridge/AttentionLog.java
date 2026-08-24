package io.github.transactionbridge;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/** Durable local register for non-retryable webhook responses. */
public final class AttentionLog implements WebhookUploader.AttentionLog {
    public interface Store {
        String read();
        void write(String value);
    }

    public static final class Entry {
        public final String id;
        public final String payload;
        public final String reason;
        public final long recordedAt;

        Entry(String id, String payload, String reason, long recordedAt) {
            this.id = id;
            this.payload = payload;
            this.reason = reason;
            this.recordedAt = recordedAt;
        }
    }

    public static Store preferences(Context context, String name) {
        final SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
        return new Store() {
            @Override public String read() { return preferences.getString("items", ""); }
            @Override public void write(String value) { if (!preferences.edit().putString("items", value).commit()) throw new IllegalStateException("attention log could not be persisted"); }
        };
    }

    private final Store store;

    public AttentionLog(Store store) {
        if (store == null) throw new IllegalArgumentException("store is required");
        this.store = store;
    }

    @Override public synchronized void record(DeliveryRecord item, String reason) {
        if (item == null) throw new IllegalArgumentException("item is required");
        List<Entry> entries = read();
        entries.removeIf(existing -> existing.id.equals(item.id));
        entries.add(0, new Entry(item.id, item.payload, reason == null ? "" : reason, System.currentTimeMillis()));
        write(entries);
    }

    public synchronized int count() { return read().size(); }

    public synchronized List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(read()));
    }

    public synchronized boolean deleteFirst() {
        List<Entry> entries = read();
        if (entries.isEmpty()) return false;
        entries.remove(0);
        write(entries);
        return true;
    }

    public synchronized boolean requeueFirst(PersistentDeliveryQueue queue) {
        if (queue == null) throw new IllegalArgumentException("queue is required");
        List<Entry> entries = read();
        if (entries.isEmpty()) return false;
        Entry entry = entries.get(0);
        queue.enqueue(entry.id, entry.payload);
        entries.remove(0);
        write(entries);
        return true;
    }

    private List<Entry> read() {
        String value = store.read();
        List<Entry> result = new ArrayList<>();
        if (value == null || value.isEmpty()) return result;
        for (String line : value.split("\\n", -1)) {
            if (line.isEmpty()) continue;
            String[] fields = line.split("\\|", 4);
            if (fields.length != 4) throw new IllegalStateException("attention log is corrupt");
            try {
                result.add(new Entry(decode(fields[0]), decode(fields[1]), decode(fields[2]), Long.parseLong(fields[3])));
            } catch (IllegalArgumentException error) {
                throw new IllegalStateException("attention log is corrupt", error);
            }
        }
        return result;
    }

    private void write(List<Entry> entries) {
        StringBuilder value = new StringBuilder();
        for (Entry entry : entries) {
            value.append(encode(entry.id)).append('|').append(encode(entry.payload)).append('|')
                    .append(encode(entry.reason)).append('|').append(entry.recordedAt).append('\n');
        }
        store.write(value.toString());
    }

    private static String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String decode(String value) { return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8); }
}
