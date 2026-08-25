package io.github.transactionbridge;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class NotificationParserTest {
    private static final long TIME = 1_786_000_000_000L;

    @Test public void parsesSupportedPurchasesAndRejectsUnrelatedText() {
        Transaction ing = new IngNotificationParser().parse(TIME,
                "Operazione autorizzata: 24.61 euro, Example Market. Non sei stato tu? Blocca subito la carta.");
        assertEquals("24.61", ing.amount.toPlainString());
        assertEquals("Example Market", ing.merchant);

        Transaction directDebit = new IngNotificationParser().parse(TIME,
                "Addebito diretto di 7.99 euro richiesto da Creditor id. IT00ZZZ0000000000000000 "
                        + "EXAMPLE MOBILE: pagato! Non ti risulta? Contattaci subito.");
        assertEquals("7.99", directDebit.amount.toPlainString());
        assertEquals("EXAMPLE MOBILE", directDebit.merchant);
        assertEquals("ing-notification", directDebit.source);
        assertNull(new IngNotificationParser().parse(TIME,
                "Addebito diretto di 7.99 euro richiesto da EXAMPLE MOBILE: in elaborazione."));
        assertNull(new IngNotificationParser().parse(TIME, "Saldo disponibile: 24.61 euro"));

        Transaction crypto = new CryptoComNotificationParser().parse(TIME,
                "€93.30 EUR spent at Example Shop\nYou earned €2.74 EUR of rewards");
        assertEquals("93.30", crypto.amount.toPlainString());
        assertEquals("EUR", crypto.currency);
        assertNull(new CryptoComNotificationParser().parse(TIME, "You received 17.45 EUR"));

        Transaction revolut = new RevolutNotificationParser().parse(TIME,
                "Example Petrol ⛽ Hai speso 28,80 € Saldo di EUR: 669,78 €");
        assertEquals("28.80", revolut.amount.toPlainString());
        assertNull(new RevolutNotificationParser().parse(TIME,
                "Hai ricevuto 28,80 € da Another Person"));
    }

    @Test public void parsesIsyBankDateAndMaskedTransferWithoutOwnerNames() {
        Transaction debit = new IsyBankNotificationParser().parse(TIME,
                "E' stato addebitato il pagamento di una domiciliazione di 28,89 € da parte di EXAMPLE PROVIDER "
                        + "sul conto xxx421 in data 10.08.2026");
        assertEquals("28.89", debit.amount.toPlainString());
        assertEquals("EXAMPLE PROVIDER", debit.merchant);

        Transaction transfer = new IsyBankNotificationParser().parse(TIME,
                "È stato inserito un bonifico istantaneo di 30,00 € dal conto xxx421 in favore dell'IBAN DE*** "
                        + "in data 13.08.2026 alle ore 17:14.");
        assertEquals("Bonifico DE***", transfer.merchant);
    }

    @Test public void parsesWalletOnlyWhenConfiguredCardMatches() {
        Map<String, String> cards = new HashMap<>();
        cards.put("1501", "Example Card");
        GoogleWalletNotificationParser parser = new GoogleWalletNotificationParser(cards);
        Transaction transaction = parser.parse(TIME, "Example Market 24,61 € con Carta Visa ••1501");
        assertEquals("24.61", transaction.amount.toPlainString());
        assertEquals("google-wallet-example-card-notification", transaction.source);
        assertNull(parser.parse(TIME, "Example Market 24,61 € con Carta Visa ••9999"));

        Map<String, String> cryptoCard = new HashMap<>();
        cryptoCard.put("1352", "Crypto.com");
        assertEquals("google-wallet-crypto-notification",
                new GoogleWalletNotificationParser(cryptoCard)
                        .parse(TIME, "Example Market 24,61 € con Carta Visa ••1352").source);
    }

    @Test public void registryMapsOnlyKnownAndroidPackages() {
        Map<String, String> cards = new HashMap<>();
        cards.put("1501", "Example Card");
        ParserRegistry registry = ParserRegistry.defaultRegistry(cards);
        ParserRegistry.Provider ing = registry.providerFor(ParserRegistry.ING_PACKAGE);
        assertEquals("ING", ing.label);
        assertEquals("ing", ing.settingKey);
        assertEquals("ing-notification", ing.parser.parse(TIME,
                "Operazione autorizzata: 1,25 euro, Example Market.").source);
        assertNull(registry.providerFor("com.example.other"));
    }

    @Test public void walletCardsUseSeparateValidatedDigitsAndNames() {
        Map<String, String> cards = new java.util.LinkedHashMap<>();
        Settings.putWalletCard(cards, " 1501 ", " Personal card ");
        assertEquals("Personal card", cards.get("1501"));
        Settings.putWalletCard(cards, "1501", "Business card");
        assertEquals("Business card", cards.get("1501"));
        assertEquals(cards, Settings.parseWalletCards(Settings.walletCardsText(cards)));
        cards.remove("1501");
        assertTrue(cards.isEmpty());

        try {
            Settings.putWalletCard(cards, "501", "Invalid");
            org.junit.Assert.fail("Expected invalid suffix");
        } catch (IllegalArgumentException expected) {
            assertEquals("Enter exactly the last 4 card digits", expected.getMessage());
        }
    }
}
