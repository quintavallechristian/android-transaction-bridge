# Adding a notification provider

This guide covers Android apps that publish payment notifications with a stable text format. If the payment is already reported by Google Wallet, do not add a provider: configure the card's last four digits and local name in Transaction Bridge settings.

## Before writing code

Collect:

- the provider's Android package name, such as `com.example.bank`;
- the notification language and app version observed;
- one payment notification that should match;
- one similar notification that must not match.

Anonymize every fixture before committing it. Replace names, merchants, IBANs, account identifiers, card digits, and other personal data while preserving punctuation and structure. Never commit credentials or a real notification dump.

## 1. Add the parser

Create:

```text
app/src/main/java/io/github/transactionbridge/ExampleBankNotificationParser.java
```

Implement the pure `NotificationParser` interface:

```java
package io.github.transactionbridge;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExampleBankNotificationParser implements NotificationParser {
    private static final Pattern PAYMENT = Pattern.compile(
            "Payment of ([0-9][0-9.,]*) EUR at (.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override public Transaction parse(long occurredAt, String rawText) {
        String text = ParserSupport.normalize(rawText);
        Matcher match = PAYMENT.matcher(text);
        if (!match.find()) return null;
        try {
            BigDecimal amount = ParserSupport.amount(match.group(1));
            String merchant = match.group(2).trim();
            return amount.signum() > 0 && !merchant.isEmpty()
                    ? new Transaction(occurredAt, amount, "EUR", merchant, text,
                    "example-bank-notification")
                    : null;
        } catch (NumberFormatException invalidAmount) {
            return null;
        }
    }
}
```

Parser rules:

- return `null` for unrelated, incomplete, or invalid notifications;
- do not depend on Android classes, storage, network, or settings;
- use `BigDecimal` and the existing `ParserSupport` helpers for amounts;
- require a positive amount and non-empty merchant;
- use the Android notification timestamp unless the notification contains a more authoritative transaction time;
- choose a stable lowercase `source`, normally `<provider>-notification`.

Do not change `Transaction`, `WebhookPayload`, the queue, or delivery code to add a provider.

## 2. Register the Android package

Edit `ParserRegistry.java`:

```java
public static final String EXAMPLE_BANK_PACKAGE = "com.example.bank";
```

Then register one parser instance in `defaultRegistry()`:

```java
registry.register(EXAMPLE_BANK_PACKAGE, new ExampleBankNotificationParser());
```

No `AndroidManifest.xml` change is required. `NotificationListenerService` receives notifications system-wide after the user grants access; `ParserRegistry` decides which packages are supported.

## 3. Add the provider setting

Edit `Settings.java`:

```java
public static final String SOURCE_EXAMPLE_BANK = "example_bank";
```

Add it to `DEFAULT_SOURCES` if it should be enabled on a fresh installation.

The settings source identifies the on/off switch. It is separate from the transaction's webhook `source`: `example_bank` may control a parser that emits `example-bank-notification`.

## 4. Connect package and setting

Edit `NotificationBridgeListener.sourceForPackage()`:

```java
if (ParserRegistry.EXAMPLE_BANK_PACKAGE.equals(packageName)) {
    return Settings.SOURCE_EXAMPLE_BANK;
}
```

This check happens before parsing. Unknown or disabled packages are ignored without storing their notification text.

## 5. Expose the switch

Edit `MainActivity.showSettings()`:

```java
addSource(checks, "Example Bank", Settings.SOURCE_EXAMPLE_BANK);
```

No provider-specific UI should be added unless the parser genuinely needs user configuration. Prefer a parser derived only from the notification text.

## 6. Add tests

Edit:

```text
app/src/test/java/io/github/transactionbridge/NotificationParserTest.java
```

Add at least:

```java
Transaction payment = new ExampleBankNotificationParser().parse(TIME,
        "Payment of 12,50 EUR at Example Market");
assertEquals("12.50", payment.amount.toPlainString());
assertEquals("Example Market", payment.merchant);
assertEquals("example-bank-notification", payment.source);

assertNull(new ExampleBankNotificationParser().parse(TIME,
        "Your available balance is 12,50 EUR"));
```

Also verify the registry mapping:

```java
assertTrue(registry.supports(ParserRegistry.EXAMPLE_BANK_PACKAGE));
```

Use more fixtures only when they represent real format variants. Do not add speculative regex alternatives.

## 7. Document and verify

Update the supported-provider list or README when the user-visible support changes. Record the notification language and app version observed in the pull request.

Run:

```sh
./gradlew testDebugUnitTest assembleDebug
```

Before opening the pull request, search the diff for personal information and confirm that the parser still rejects the negative fixture.

## Files changed for a normal provider

| File | Change |
|---|---|
| `ExampleBankNotificationParser.java` | New pure parser |
| `ParserRegistry.java` | Package constant and parser registration |
| `Settings.java` | Source setting and default |
| `NotificationBridgeListener.java` | Package-to-setting mapping |
| `MainActivity.java` | Provider checkbox |
| `NotificationParserTest.java` | Positive, negative, and registry checks |
| `README.md` | User-visible support, if listed |

That is the complete path. Delivery, retry, privacy modes, deterministic IDs, and webhook serialization are shared automatically.
