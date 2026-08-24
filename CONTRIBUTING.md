# Contributing

Keep changes small and focused. New notification sources should include:

- a pure Java parser;
- one anonymized positive fixture and one negative fixture;
- the Android package mapping;
- a note describing the language and notification version observed.

Do not add credentials, real card numbers, IBANs, names, endpoints, or raw notification text from identifiable people. Run `./gradlew testDebugUnitTest assembleDebug` before opening a pull request.
