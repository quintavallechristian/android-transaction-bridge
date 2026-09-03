# Contributing

Transaction Bridge owns Android notification collection, settings, webhook delivery, retry, persistence, privacy modes, and UI. Keep changes small, focused, and free of credentials, endpoints, or identifiable notification data.

Parser formats, provider metadata, and parser tests belong in the standalone [transaction-parsers](https://github.com/quintavallechristian/transaction-parsers) repository. After its release, update this app's package version when required.

Run `./gradlew testDebugUnitTest assembleDebug` before opening a pull request and wait for the GitHub checks to pass.
