package io.github.transactionbridge;

import java.math.BigDecimal;

final class ParserSupport {
    private ParserSupport() {}

    static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    static BigDecimal amount(String value) {
        String decimal = value.trim();
        if (decimal.indexOf(',') >= 0 && decimal.indexOf('.') >= 0) {
            decimal = decimal.lastIndexOf(',') > decimal.lastIndexOf('.')
                    ? decimal.replace(".", "").replace(',', '.')
                    : decimal.replace(",", "");
        } else if (decimal.indexOf(',') >= 0) {
            decimal = decimal.replace(',', '.');
        }
        return new BigDecimal(decimal);
    }

    static BigDecimal amountWithDotThousands(String value) {
        String decimal = value.trim();
        if (decimal.indexOf(',') >= 0) return amount(decimal);
        // Revolut renders whole euro amounts as e.g. 2.000, while decimal cents have two digits.
        if (decimal.matches("[0-9]+(?:\\.[0-9]{3})+")) decimal = decimal.replace(".", "");
        return new BigDecimal(decimal);
    }
}
