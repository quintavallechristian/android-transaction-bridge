# Transaction Bridge

Transaction Bridge is a small Android app that turns supported payment notifications into versioned JSON and delivers them to a webhook you control.

## Install and configure

1. Build and install the debug APK, or install a release from GitHub Releases.
2. Open **Settings** and enter a complete `https://` webhook URL. A Bearer token is optional.
3. Choose `minimal` (the default) or `full` payload mode. Full mode includes the original notification text, which may contain personal information.
4. Select the notification sources to parse.
5. For Google Wallet, enter the last four card digits and a local card name, then select **Add or update card**.
6. Open **Notification access** and enable Transaction Bridge.

Pending deliveries are stored locally in FIFO order. Supported HTTP `2xx` responses remove an item; temporary failures are retried with bounded backoff. The app has no account, analytics, crash reporting, or telemetry service.

## Google Wallet

Google Wallet notifications identify the payment card by its last four digits. Transaction Bridge compares those digits with the cards configured on the device and uses the corresponding name in the webhook `source`, for example `google-wallet-personal-ing-notification`.

Only the last four digits and the name chosen by the user are stored. Full card numbers are never requested. A Wallet notification from an unknown card is ignored. Card mappings remain on the device, and the default `minimal` payload does not include the original notification text.

## Build

```sh
./gradlew testDebugUnitTest assembleDebug
```

The project requires Android SDK 36 and Java 17. The package name is `io.github.transactionbridge`.

## Webhook payload

The default payload contains `version`, `id`, `source`, `occurredAt`, `amount`, `currency`, and `merchant`. The ID is deterministic so webhook consumers can safely deduplicate deliveries. `rawText` is added only in `full` mode.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), and [PRIVACY.md](PRIVACY.md).
