# Contributing

Keep changes small and focused. New notification sources should include:

- a pure Java parser;
- one anonymized positive fixture and one negative fixture;
- the Android package mapping;
- a note describing the language and notification version observed.

Do not add credentials, real card numbers, IBANs, names, endpoints, or raw notification text from identifiable people. Run `./gradlew testDebugUnitTest assembleDebug` before opening a pull request.

Follow the complete [provider contribution guide](docs/ADDING_A_PROVIDER.md) for the exact files, parser contract, registration steps, and anonymized test requirements.
