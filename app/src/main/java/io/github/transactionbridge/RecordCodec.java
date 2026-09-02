package io.github.transactionbridge;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Shared encoding for the small line-based records kept in SharedPreferences. */
final class RecordCodec {
    private RecordCodec() {}

    static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    static String[] fields(String line, int count, String errorMessage) {
        String[] fields = line.split("\\|", count);
        if (fields.length != count) throw new IllegalStateException(errorMessage);
        return fields;
    }
}
