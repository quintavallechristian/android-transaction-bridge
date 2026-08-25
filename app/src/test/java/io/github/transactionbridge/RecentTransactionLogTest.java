package io.github.transactionbridge;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class RecentTransactionLogTest {
    @Test public void keepsOnlyTheTwentyMostRecentUniqueTransactions() {
        MemoryStore store = new MemoryStore();
        RecentTransactionLog log = new RecentTransactionLog(store);
        List<Transaction> transactions = new ArrayList<>();
        for (int index = 0; index < 22; index++) {
            Transaction transaction = transaction(index);
            transactions.add(transaction);
            log.record(transaction, "Provider");
        }
        log.record(transactions.get(10), "Updated provider");

        assertEquals(20, log.entries().size());
        assertEquals(transactions.get(10).id, log.entries().get(0).id);
        assertEquals("Updated provider", log.entries().get(0).provider);
        assertEquals("11.00", log.entries().get(0).amount);
        assertEquals(transactions.get(2).id, log.entries().get(19).id);
    }

    private static Transaction transaction(int amount) {
        int positiveAmount = amount + 1;
        return new Transaction(1_786_000_000_000L + amount, new BigDecimal(positiveAmount + ".00"),
                "EUR", "Example merchant " + amount, "private notification", "test-notification");
    }

    private static final class MemoryStore implements RecentTransactionLog.Store {
        private String value = "";
        @Override public String read() { return value; }
        @Override public void write(String value) { this.value = value; }
    }
}
