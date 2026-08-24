# Privacy

Transaction Bridge runs locally on the Android device. It reads only notifications from sources enabled by the user and the network state needed to retry delivery.

The default `minimal` payload omits the original notification text. `full` mode is opt-in and can include personal information present in a notification. The app has no analytics, advertising, account system, crash reporting, or hosted backend. Pending payloads and delivery records remain in local app storage until removed by successful delivery or user action.
