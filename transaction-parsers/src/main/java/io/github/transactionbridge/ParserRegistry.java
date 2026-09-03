package io.github.transactionbridge;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fixed package-to-parser mapping for the sources shipped by the bridge. */
public final class ParserRegistry {
    public static final String CRYPTO_COM_PACKAGE = "co.mona.android";
    public static final String ISYBANK_PACKAGE = "com.intesasanpaolo.isybank.mobile";
    public static final String ING_PACKAGE = "it.ing.banking";
    public static final String GOOGLE_WALLET_PACKAGE = "com.google.android.apps.walletnfcrel";
    public static final String REVOLUT_PACKAGE = "com.revolut.revolut";
    public static final String BBVA_PACKAGE = "com.bbva.italy";
    public static final String HYPE_PACKAGE = "it.hype.app";
    public static final String AMEX_PACKAGE = "com.americanexpress.android.acctsvcs.it";

    public static final class Provider {
        public final String packageName;
        public final String settingKey;
        public final String label;
        public final NotificationParser parser;

        Provider(String packageName, String settingKey, String label, NotificationParser parser) {
            this.packageName = packageName;
            this.settingKey = settingKey;
            this.label = label;
            this.parser = parser;
        }
    }

    private final Map<String, Provider> providers = new LinkedHashMap<>();

    public ParserRegistry() {}

    public static ParserRegistry defaultRegistry(Map<String, String> walletCards) {
        ParserRegistry registry = new ParserRegistry();
        registry.register(CRYPTO_COM_PACKAGE, "crypto.com", "Crypto.com", new CryptoComNotificationParser());
        registry.register(ISYBANK_PACKAGE, "isybank", "IsyBank", new IsyBankNotificationParser());
        registry.register(ING_PACKAGE, "ing", "ING", new IngNotificationParser());
        registry.register(GOOGLE_WALLET_PACKAGE, "google_wallet", "Google Wallet", new GoogleWalletNotificationParser(walletCards));
        registry.register(REVOLUT_PACKAGE, "revolut", "Revolut", new RevolutNotificationParser());
        registry.register(BBVA_PACKAGE, "bbva", "BBVA", new BbvaNotificationParser());
        registry.register(HYPE_PACKAGE, "hype", "HYPE", new HypeNotificationParser());
        registry.register(AMEX_PACKAGE, "amex", "American Express", new AmexNotificationParser());
        return registry;
    }

    public ParserRegistry register(String packageName, String settingKey, String label, NotificationParser parser) {
        if (packageName == null || packageName.trim().isEmpty()) throw new IllegalArgumentException("packageName is required");
        if (settingKey == null || settingKey.trim().isEmpty() || label == null || label.trim().isEmpty() || parser == null) {
            throw new IllegalArgumentException("settingKey, label and parser are required");
        }
        providers.put(packageName, new Provider(packageName, settingKey, label, parser));
        return this;
    }

    public Provider providerFor(String packageName) { return providers.get(packageName); }

    public Collection<Provider> providers() {
        return List.copyOf(providers.values());
    }
}
