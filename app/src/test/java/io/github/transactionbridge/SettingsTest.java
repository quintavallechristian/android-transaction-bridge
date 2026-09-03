package io.github.transactionbridge;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class SettingsTest {
    @Test public void walletCardsUseSeparateValidatedDigitsAndNames() {
        Map<String, String> cards = new LinkedHashMap<>();
        Settings.putWalletCard(cards, " 1501 ", " Personal card ");
        assertEquals("Personal card", cards.get("1501"));
        Settings.putWalletCard(cards, "1501", "Business card");
        assertEquals(cards, Settings.parseWalletCards(Settings.walletCardsText(cards)));
        cards.remove("1501");
        assertTrue(cards.isEmpty());

        try {
            Settings.putWalletCard(cards, "501", "Invalid");
            fail("Expected invalid suffix");
        } catch (IllegalArgumentException expected) {
            assertEquals("Enter exactly the last 4 card digits", expected.getMessage());
        }
    }
}
