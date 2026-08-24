# Transaction Bridge

Transaction Bridge is a small Android app that turns supported payment notifications into versioned JSON and delivers them to a webhook you control.

## Install and configure

1. Build and install the debug APK, or install a release from GitHub Releases.
2. Open **Settings** and enter a complete `https://` webhook URL. A Bearer token is optional.
3. Choose `minimal` (the default) or `full` payload mode. Full mode includes the original notification text, which may contain personal information.
4. Select the notification sources to parse and, when needed, map Google Wallet card suffixes as `1234=source`.
5. Open **Notification access** and enable Transaction Bridge.

Pending deliveries are stored locally in FIFO order. Supported HTTP `2xx` responses remove an item; temporary failures are retried with bounded backoff. The app has no account, analytics, crash reporting, or telemetry service.

## Build

```sh
./gradlew testDebugUnitTest assembleDebug
```

The project requires Android SDK 36 and Java 17. The package name is `io.github.transactionbridge`.

## Webhook payload

The default payload contains `version`, `id`, `source`, `occurredAt`, `amount`, `currency`, and `merchant`. The ID is deterministic so webhook consumers can safely deduplicate deliveries. `rawText` is added only in `full` mode.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), and [PRIVACY.md](PRIVACY.md).
