package io.github.transactionbridge;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class NotificationParserTest {
    private static final long TIME = 1_786_000_000_000L;

    @Test public void parsesSupportedPurchasesAndRejectsUnrelatedText() {
        Transaction ing = new IngNotificationParser().parse(TIME,
                "Operazione autorizzata: 24.61 euro, Example Market. Non sei stato tu? Blocca subito la carta.");
        assertEquals("24.61", ing.amount.toPlainString());
        assertEquals("Example Market", ing.merchant);
        assertNull(new IngNotificationParser().parse(TIME, "Saldo disponibile: 24.61 euro"));

        Transaction crypto = new CryptoComNotificationParser().parse(TIME,
                "€93.30 EUR spent at Example Shop\nYou earned €2.74 EUR of rewards");
        assertEquals("93.30", crypto.amount.toPlainString());
        assertEquals("EUR", crypto.currency);
        assertNull(new CryptoComNotificationParser().parse(TIME, "You received 17.45 EUR"));

        Transaction coverflex = new CoverflexNotificationParser().parse(TIME,
                "Pagamento di €44.80 confermato Hai speso €44.80 da Sumup *Example Shop");
        assertEquals("Example Shop", coverflex.merchant);
        assertNull(new CoverflexNotificationParser().parse(TIME,
                "Pagamento di €44.80 rifiutato Hai speso €44.80 da Example Shop"));

        Transaction revolut = new RevolutNotificationParser().parse(TIME,
                "Example Petrol ⛽ Hai speso 28,80 € Saldo di EUR: 669,78 €");
        assertEquals("28.80", revolut.amount.toPlainString());
        assertNull(new RevolutNotificationParser().parse(TIME,
                "Hai ricevuto 28,80 € da Another Person"));
    }

    @Test public void parsesIsyBankDateAndMaskedTransferWithoutOwnerNames() {
        Transaction debit = IsyBankNotificationParser.parse(
                "E' stato addebitato il pagamento di una domiciliazione di 28,89 € da parte di EXAMPLE PROVIDER "
                        + "sul conto xxx421 in data 10.08.2026");
        assertEquals("28.89", debit.amount.toPlainString());
        assertEquals("EXAMPLE PROVIDER", debit.merchant);

        Transaction transfer = IsyBankNotificationParser.parse(
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
        assertTrue(registry.supports(ParserRegistry.ING_PACKAGE));
        assertTrue(registry.supports(ParserRegistry.COVERFLEX_PACKAGE));
        assertFalse(registry.supports("com.example.other"));
        assertEquals("ing-notification", registry.parse(ParserRegistry.ING_PACKAGE, TIME,
                "Operazione autorizzata: 1,25 euro, Example Market.").source);
        assertNull(registry.parse("com.example.other", TIME, "Example Market 1,25 EUR"));
    }
}
