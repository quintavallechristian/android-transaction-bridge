package io.github.transactionbridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Small, local-only settings store. Secrets never go into SharedPreferences in plaintext. */
public final class Settings {
    public static final String PAYLOAD_MINIMAL = "minimal";
    public static final String PAYLOAD_FULL = "full";

    private static final String FILE = "transaction_bridge_settings";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_SOURCES = "sources";
    private static final String KEY_WALLET_CARDS = "wallet_cards";
    private static final String KEY_PAYLOAD_MODE = "payload_mode";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "transaction_bridge_token";
    private static final String TOKEN_SEPARATOR = ":";

    private Settings() {}

    public static boolean isConfigured(Context context) {
        return !endpoint(context).isEmpty();
    }

    public static String endpoint(Context context) {
        return preferences(context).getString(KEY_ENDPOINT, "");
    }

    public static String token(Context context) {
        String encrypted = preferences(context).getString(KEY_TOKEN, "");
        return encrypted.isEmpty() ? "" : decrypt(encrypted);
    }

    public static void save(Context context, String endpoint, String token) {
        String normalizedEndpoint = requireHttpsEndpoint(endpoint);
        SharedPreferences.Editor editor = preferences(context).edit().putString(KEY_ENDPOINT, normalizedEndpoint);
        String normalizedToken = token == null ? "" : token.trim();
        if (!normalizedToken.isEmpty()) editor.putString(KEY_TOKEN, encrypt(normalizedToken));
        editor.apply();
    }

    public static Set<String> enabledSources(Context context) {
        String encoded = preferences(context).getString(KEY_SOURCES, "");
        if (encoded.isEmpty()) {
            Set<String> defaults = new LinkedHashSet<>();
            for (ParserRegistry.Provider provider : ParserRegistry.defaultRegistry(Collections.emptyMap()).providers()) {
                defaults.add(provider.settingKey);
            }
            return defaults;
        }
        Set<String> result = new LinkedHashSet<>();
        for (String source : encoded.split(",")) {
            if (!source.trim().isEmpty()) result.add(source.trim());
        }
        return result;
    }

    public static boolean sourceEnabled(Context context, String source) {
        return enabledSources(context).contains(source);
    }

    public static void saveSources(Context context, Set<String> sources) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (sources != null) {
            for (String source : sources) {
                if (source != null && !source.trim().isEmpty()) normalized.add(source.trim());
            }
        }
        preferences(context).edit().putString(KEY_SOURCES, String.join(",", normalized)).apply();
    }

    public static String payloadMode(Context context) {
        String mode = preferences(context).getString(KEY_PAYLOAD_MODE, PAYLOAD_MINIMAL);
        return PAYLOAD_FULL.equals(mode) ? PAYLOAD_FULL : PAYLOAD_MINIMAL;
    }

    public static void savePayloadMode(Context context, String mode) {
        if (!PAYLOAD_FULL.equals(mode) && !PAYLOAD_MINIMAL.equals(mode)) {
            throw new IllegalArgumentException("Payload mode must be minimal or full");
        }
        preferences(context).edit().putString(KEY_PAYLOAD_MODE, mode).apply();
    }

    /** Last-four-digits to source alias mappings used by Google Wallet parsers. */
    public static Map<String, String> walletCards(Context context) {
        return parseWalletCards(preferences(context).getString(KEY_WALLET_CARDS, ""));
    }

    public static void saveWalletCards(Context context, Map<String, String> cards) {
        preferences(context).edit().putString(KEY_WALLET_CARDS, walletCardsText(cards)).apply();
    }

    public static void putWalletCard(Map<String, String> cards, String lastFour, String name) {
        if (cards == null) throw new IllegalArgumentException("Wallet cards are required");
        String digits = lastFour == null ? "" : lastFour.trim();
        String label = name == null ? "" : name.trim();
        if (!digits.matches("\\d{4}")) throw new IllegalArgumentException("Enter exactly the last 4 card digits");
        if (label.isEmpty()) throw new IllegalArgumentException("Enter a card name");
        cards.put(digits, label);
    }

    public static String walletCardsText(Map<String, String> cards) {
        StringBuilder result = new StringBuilder();
        if (cards == null) return "";
        for (Map.Entry<String, String> card : cards.entrySet()) {
            if (result.length() > 0) result.append('\n');
            result.append(card.getKey()).append('=').append(card.getValue());
        }
        return normalizeWalletCards(result.toString());
    }

    public static Map<String, String> parseWalletCards(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        String normalized = normalizeWalletCards(raw);
        if (normalized.isEmpty()) return result;
        for (String line : normalized.split("\\n")) {
            String[] pair = line.split("=", 2);
            result.put(pair[0], pair[1]);
        }
        return result;
    }

    private static String normalizeWalletCards(String raw) {
        LinkedHashSet<String> digits = new LinkedHashSet<>();
        StringBuilder result = new StringBuilder();
        if (raw == null) return "";
        for (String value : raw.split("\\R")) {
            String line = value.trim();
            if (line.isEmpty()) continue;
            String[] pair = line.split("=", 2);
            if (pair.length != 2 || !pair[0].trim().matches("\\d{4}") || pair[1].trim().isEmpty()) {
                throw new IllegalArgumentException("Use one card mapping per line: 1234=source");
            }
            String lastFour = pair[0].trim();
            if (!digits.add(lastFour)) throw new IllegalArgumentException("Card suffixes must be unique");
            if (result.length() > 0) result.append('\n');
            result.append(lastFour).append('=').append(pair[1].trim());
        }
        return result.toString();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static String requireHttpsEndpoint(String value) {
        String endpoint = value == null ? "" : value.trim();
        Uri uri = Uri.parse(endpoint);
        if (endpoint.isEmpty() || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("Webhook URL must use HTTPS");
        }
        return endpoint;
    }

    private static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
            String body = Base64.encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            return iv + TOKEN_SEPARATOR + body;
        } catch (Exception error) {
            throw new IllegalStateException("Unable to protect token", error);
        }
    }

    private static String decrypt(String value) {
        try {
            String[] parts = value.split(TOKEN_SEPARATOR, 2);
            if (parts.length != 2) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance(KEYSTORE);
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
