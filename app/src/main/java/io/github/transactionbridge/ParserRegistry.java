package io.github.transactionbridge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Fixed package-to-parser mapping for the sources shipped by the bridge. */
public final class ParserRegistry {
    public static final String CRYPTO_COM_PACKAGE = "co.mona.android";
    public static final String ISYBANK_PACKAGE = "com.intesasanpaolo.isybank.mobile";
    public static final String ING_PACKAGE = "it.ing.banking";
    public static final String GOOGLE_WALLET_PACKAGE = "com.google.android.apps.walletnfcrel";
    public static final String REVOLUT_PACKAGE = "com.revolut.revolut";
    public static final String COVERFLEX_PACKAGE = "com.coverflex";

    private final Map<String, NotificationParser> parsers = new LinkedHashMap<>();

    public ParserRegistry() {}

    public static ParserRegistry defaultRegistry(Map<String, String> walletCards) {
        ParserRegistry registry = new ParserRegistry();
        registry.register(CRYPTO_COM_PACKAGE, new CryptoComNotificationParser());
        registry.register(ISYBANK_PACKAGE, new IsyBankNotificationParser());
        registry.register(ING_PACKAGE, new IngNotificationParser());
        registry.register(GOOGLE_WALLET_PACKAGE, new GoogleWalletNotificationParser(walletCards));
        registry.register(REVOLUT_PACKAGE, new RevolutNotificationParser());
        registry.register(COVERFLEX_PACKAGE, new CoverflexNotificationParser());
        return registry;
    }

    public static ParserRegistry defaults(Map<String, String> walletCards) {
        return defaultRegistry(walletCards);
    }

    public ParserRegistry register(String packageName, NotificationParser parser) {
        if (packageName == null || packageName.trim().isEmpty()) throw new IllegalArgumentException("packageName is required");
        if (parser == null) throw new IllegalArgumentException("parser is required");
        parsers.put(packageName, parser);
        return this;
    }

    public NotificationParser parserFor(String packageName) {
        return parsers.get(packageName);
    }

    public Transaction parse(String packageName, long occurredAt, String rawText) {
        NotificationParser parser = parserFor(packageName);
        return parser == null ? null : parser.parse(occurredAt, rawText);
    }

    public boolean supports(String packageName) { return parsers.containsKey(packageName); }

    public Set<String> supportedPackages() {
        return Collections.unmodifiableSet(parsers.keySet());
    }
}
