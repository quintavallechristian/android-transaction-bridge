package io.github.transactionbridge;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Last recognized transactions, stored locally without notification text. */
public final class RecentTransactionLog {
    private static final int LIMIT = 20;

    public interface Store {
        String read();
        void write(String value);
    }

    public static final class Entry {
        public final String id;
        public final long occurredAt;
        public final String amount;
        public final String currency;
        public final String merchant;
        public final String provider;

        Entry(String id, long occurredAt, String amount, String currency, String merchant, String provider) {
            this.id = id;
            this.occurredAt = occurredAt;
            this.amount = amount;
            this.currency = currency;
            this.merchant = merchant;
            this.provider = provider;
        }
    }

    public static Store preferences(Context context) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences("recent_transactions", Context.MODE_PRIVATE);
        return new Store() {
            @Override public String read() { return preferences.getString("items", ""); }
            @Override public void write(String value) {
                if (!preferences.edit().putString("items", value).commit()) {
                    throw new IllegalStateException("recent transactions could not be persisted");
                }
            }
        };
    }

    private final Store store;

    public RecentTransactionLog(Store store) {
        if (store == null) throw new IllegalArgumentException("store is required");
        this.store = store;
    }

    public synchronized void record(Transaction transaction, String provider) {
        if (transaction == null || provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("transaction and provider are required");
        }
        List<Entry> entries = read();
        entries.removeIf(entry -> entry.id.equals(transaction.id));
        entries.add(0, new Entry(transaction.id, transaction.occurredAt,
                transaction.amount.toPlainString(), transaction.currency, transaction.merchant, provider.trim()));
        if (entries.size() > LIMIT) entries.subList(LIMIT, entries.size()).clear();
        write(entries);
    }

    public synchronized List<Entry> entries() { return List.copyOf(read()); }

    private List<Entry> read() {
        List<Entry> entries = new ArrayList<>();
        String value = store.read();
        if (value == null || value.isEmpty()) return entries;
        for (String line : value.split("\\n", -1)) {
            if (line.isBlank()) continue;
            String[] fields = RecordCodec.fields(line, 6, "recent transactions are corrupt");
            try {
                entries.add(new Entry(RecordCodec.decode(fields[0]), Long.parseLong(fields[1]), RecordCodec.decode(fields[2]),
                        RecordCodec.decode(fields[3]), RecordCodec.decode(fields[4]), RecordCodec.decode(fields[5])));
            } catch (IllegalArgumentException error) {
                throw new IllegalStateException("recent transactions are corrupt", error);
            }
        }
        return entries;
    }

    private void write(List<Entry> entries) {
        StringBuilder value = new StringBuilder();
        for (Entry entry : entries) {
            value.append(RecordCodec.encode(entry.id)).append('|').append(entry.occurredAt).append('|')
                    .append(RecordCodec.encode(entry.amount)).append('|').append(RecordCodec.encode(entry.currency)).append('|')
                    .append(RecordCodec.encode(entry.merchant)).append('|').append(RecordCodec.encode(entry.provider)).append('\n');
        }
        store.write(value.toString());
    }
}
