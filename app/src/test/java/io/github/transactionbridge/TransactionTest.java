package io.github.transactionbridge;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class TransactionTest {
    @Test public void idIsDeterministicAndIgnoresRawTextForReplayDeduplication() {
        Transaction first = new Transaction(1_786_000_000_000L, new BigDecimal("12.50"),
                "eur", "Example Market", "title", "ing-notification");
        Transaction replay = new Transaction(1_786_000_000_000L, new BigDecimal("12.50"),
                "EUR", "Example Market", "title with body", "ing-notification");
        assertEquals(first.id, replay.id);
        assertEquals("EUR", first.currency);
        assertEquals(64, first.id.length());
    }

    @Test public void rejectsInvalidTransactionValues() {
        try {
            new Transaction(1, BigDecimal.ZERO, "EUR", "Merchant", "", "source");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("positive"));
            return;
        }
        fail("invalid transaction accepted");
    }

}
