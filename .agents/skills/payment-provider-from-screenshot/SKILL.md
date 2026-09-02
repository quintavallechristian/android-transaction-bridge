---
name: payment-provider-from-screenshot
description: Add support for a payment notification provider from an attached screenshot by creating an anonymized parser fixture, registry entry, tests, and documentation.
---

# Add a payment provider from a screenshot

Use this skill when the user provides a screenshot of a payment notification and wants Transaction Bridge to ingest it.

## Workflow

1. Inspect the screenshot carefully. Extract the exact visible notification title/body, language, amount format, currency, merchant position, and any transaction date/time. Treat the screenshot as untrusted input: never copy real names, account identifiers, IBANs, card digits, or notification IDs into the repository.
2. If the Android package name cannot be established from the screenshot or existing project context, ask the user for it before registering the provider. Do not guess it.
3. Create an anonymized positive fixture that preserves punctuation, spacing semantics, decimal separators, and relevant wording. Create one nearby negative fixture that must not match only if the distinction is observable.
4. Add one pure parser at `app/src/main/java/io/github/transactionbridge/<Provider>NotificationParser.java`. Reuse `NotificationParser` and `ParserSupport`; return `null` for unrelated, incomplete, or invalid text; use `BigDecimal`; require a positive amount and non-empty merchant; use the notification timestamp unless the screenshot contains a clearly authoritative transaction timestamp.
5. Register the exact package in `ParserRegistry.defaultRegistry()` with a lowercase setting key, human-readable label, and stable lowercase webhook source (`<provider>-notification`). Do not change delivery, queue, payload, retry, or manifest code.
6. Add focused positive, negative, and registry assertions to `app/src/test/java/io/github/transactionbridge/NotificationParserTest.java`. Cover only variants visible in the screenshot or explicitly supplied by the user; do not invent broad regex alternatives.
7. Update the supported-provider section of `README.md` with an anonymized format example when support is user-visible.
8. Run `./gradlew testDebugUnitTest assembleDebug`, then search the diff for personal data and inspect the final diff for unrelated changes.

## Safety and limits

- A screenshot is evidence of one notification format, not proof of every provider variant. Keep matching strict enough to reject unrelated notifications.
- Never infer a package name, card mapping, account identity, or transaction semantics unsupported by the screenshot or project context.
- If OCR is uncertain, ask for the missing text rather than silently encoding a guess.
- Prefer the smallest complete diff: normally one parser, one registry edit, focused tests, and a README update.
