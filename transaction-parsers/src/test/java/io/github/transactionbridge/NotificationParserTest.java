package io.github.transactionbridge;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class NotificationParserTest {
    private static final long TIME = 1_786_000_000_000L;

    @Test public void parsesSupportedPurchasesAndRejectsUnrelatedText() {
        Transaction ing = new IngNotificationParser().parse(TIME,
                "Operazione autorizzata: 24.61 euro, Example Market. Non sei stato tu? Blocca subito la carta.");
        assertEquals("24.61", ing.amount.toPlainString());
        assertEquals("Example Market", ing.merchant);

        Transaction directDebit = new IngNotificationParser().parse(TIME,
                "Addebito diretto di 7.99 euro richiesto da Creditor id. IT00ZZZ**************** "
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

        Transaction bbva = new BbvaNotificationParser().parse(TIME,
                "Pagamento accettato 👍 💳 Il pagamento di 12,50 EUR in data EXAMPLE SHOP "
                        + "effettuato con la tua carta 1234 è stato accettato.");
        assertEquals("12.50", bbva.amount.toPlainString());
        assertEquals("EUR", bbva.currency);
        assertEquals("EXAMPLE SHOP", bbva.merchant);
        assertEquals("bbva-notification", bbva.source);
        assertNull(new BbvaNotificationParser().parse(TIME,
                "Il pagamento di 12,50 EUR in data EXAMPLE SHOP effettuato con la tua carta 1234 è stato rifiutato."));

        Transaction hype = new HypeNotificationParser().parse(TIME, "EXAMPLE SHOP, EXAMPLE CITY 12,50 €");
        assertEquals("12.50", hype.amount.toPlainString());
        assertEquals("EUR", hype.currency);
        assertEquals("EXAMPLE SHOP, EXAMPLE CITY", hype.merchant);
        assertEquals("hype-notification", hype.source);
        assertNull(new HypeNotificationParser().parse(TIME, "12,50 €"));

        Transaction amex = new AmexNotificationParser().parse(TIME, "EXAMPLE HOTEL 12,50 €");
        assertEquals("12.50", amex.amount.toPlainString());
        assertEquals("EUR", amex.currency);
        assertEquals("EXAMPLE HOTEL", amex.merchant);
        assertEquals("amex-notification", amex.source);
        assertNull(new AmexNotificationParser().parse(TIME, "12,50 €"));
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

        Transaction european = new IsyBankNotificationParser().parse(TIME,
                "E' stato inserito il pagamento di un bonifico europeo di 128,50 € dal conto xxx421 " +
                        "in favore dell'IBAN DE*** il 26.08.2026 alle ore 17:59 con data di addebito il 27.08.2026.");
        assertEquals("128.50", european.amount.toPlainString());
        assertEquals("Bonifico DE***", european.merchant);

        assertNull(new IsyBankNotificationParser().parse(TIME,
                "Il saldo del conto xxx421 e' 128,50 EUR"));
    }

    @Test public void isyBankKeepsDeliveryRetriesStableButTreatsNewPostsSeparately() {
        String text = "E' stato addebitato il pagamento di una domiciliazione di 28,89 € da parte di "
                + "EXAMPLE PROVIDER sul conto xxx421 in data 10.08.2026";
        IsyBankNotificationParser parser = new IsyBankNotificationParser();
        Transaction first = parser.parse(TIME, text);

        assertEquals(first.id, parser.parse(TIME, text).id);
        org.junit.Assert.assertNotEquals(first.id, parser.parse(TIME + 1, text).id);
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
        assertProvider(registry, ParserRegistry.CRYPTO_COM_PACKAGE, "crypto.com", "Crypto.com");
        assertProvider(registry, ParserRegistry.ISYBANK_PACKAGE, "isybank", "IsyBank");
        assertProvider(registry, ParserRegistry.GOOGLE_WALLET_PACKAGE, "google_wallet", "Google Wallet");
        assertProvider(registry, ParserRegistry.REVOLUT_PACKAGE, "revolut", "Revolut");
        assertProvider(registry, ParserRegistry.BBVA_PACKAGE, "bbva", "BBVA");
        assertProvider(registry, ParserRegistry.HYPE_PACKAGE, "hype", "HYPE");
        assertProvider(registry, ParserRegistry.AMEX_PACKAGE, "amex", "American Express");
        assertNull(registry.providerFor("com.example.other"));
    }

    private static void assertProvider(ParserRegistry registry, String packageName, String settingKey, String label) {
        ParserRegistry.Provider provider = registry.providerFor(packageName);
        assertEquals(packageName, provider.packageName);
        assertEquals(settingKey, provider.settingKey);
        assertEquals(label, provider.label);
    }

}
