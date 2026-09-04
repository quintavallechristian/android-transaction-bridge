# Transaction Bridge

Transaction Bridge is a small Android app that turns supported payment notifications into versioned JSON and delivers them to a webhook you control.

## Install and configure

1. Build and install the debug APK, or install a release from GitHub Releases.
2. Create an HTTPS receiver by following the [webhook guide](docs/WEBHOOK.md), then open **Settings** and enter its complete URL. A Bearer token is optional.
3. Choose `minimal` (the default) or `full` payload mode. Full mode includes the original notification text, which may contain personal information.
4. Select the notification sources to parse.
5. For Google Wallet, enter the last four card digits and a local card name, then select **Add or update card**.
6. Open **Notification access** and enable Transaction Bridge.

Pending deliveries are stored locally in FIFO order. Supported HTTP `2xx` responses remove an item; temporary failures are retried with bounded backoff. The app has no account, analytics, crash reporting, or telemetry service.

## Supported notification sources

Transaction Bridge supports the notification sources provided by [transaction-parsers](https://github.com/quintavallechristian/transaction-parsers), where the current list, exact formats, and parser contribution instructions live.

## Google Wallet configuration

Google Wallet parsing is provided by `transaction-parsers`. Transaction Bridge supplies it with the cards configured on the device so the last four digits can be mapped to the corresponding name in the webhook `source`, for example `google-wallet-personal-ing-notification`.

Only the last four digits and the name chosen by the user are stored. Full card numbers are never requested. A Wallet notification from an unknown card is ignored. Card mappings remain on the device, and the default `minimal` payload does not include the original notification text.

## Build

```sh
./gradlew testDebugUnitTest assembleDebug
```

The project requires Android SDK 36 and Java 17. The package name is `io.github.transactionbridge`.

## Parser library

Transaction Bridge is an example app showing how to integrate and use the Android-independent [transaction-parsers](https://github.com/quintavallechristian/transaction-parsers) library:

```kotlin
implementation("io.github.quintavallechristian:transaction-parsers:0.1.2")
```

GitHub Packages requires a GitHub username and a token with `read:packages` in the consuming project's Maven repository credentials.

## Webhook payload

The default payload contains `version`, `id`, `source`, `occurredAt`, `amount`, `currency`, and `merchant`. The ID is stable across delivery retries so webhook consumers can safely deduplicate them. A notification reposted by Android may have a new ID and should be treated as a separate observation. `rawText` is added only in `full` mode. See [Receiving webhook data](docs/WEBHOOK.md) for the request contract, response handling, retries, and a receiver outline.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), and [PRIVACY.md](PRIVACY.md). Parser contributions belong in [transaction-parsers](https://github.com/quintavallechristian/transaction-parsers).
