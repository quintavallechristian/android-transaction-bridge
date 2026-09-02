package io.github.transactionbridge;

import android.app.Notification;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.LinkedHashSet;
import java.util.Set;

/** Android-only adapter: extracts notification text, then hands pure data to the bridge core. */
public final class NotificationBridgeListener extends NotificationListenerService {
    private static volatile NotificationBridgeListener instance;
    private ConnectivityManager connectivity;
    private ParserRegistry registry;

    public static void refreshConfiguration() {
        NotificationBridgeListener value = instance;
        if (value != null) value.registry = ParserRegistry.defaultRegistry(Settings.walletCards(value));
    }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        DeliveryRunner.start(this);
        registry = ParserRegistry.defaultRegistry(Settings.walletCards(this));
        connectivity = getSystemService(ConnectivityManager.class);
        if (connectivity != null) connectivity.registerDefaultNetworkCallback(networkCallback);
    }

    @Override public void onListenerConnected() {
        registry = ParserRegistry.defaultRegistry(Settings.walletCards(this));
        DeliveryRunner.start(this);
    }

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        if (connectivity != null) connectivity.unregisterNetworkCallback(networkCallback);
        super.onDestroy();
    }

    @Override public void onNotificationPosted(StatusBarNotification notification) {
        if (notification == null || registry == null) return;
        String packageName = notification.getPackageName();
        ParserRegistry.Provider provider = registry.providerFor(packageName);
        if (provider == null || !Settings.sourceEnabled(this, provider.settingKey)) return;

        Bundle extras = notification.getNotification().extras;
        Set<String> parts = new LinkedHashSet<>();
        add(parts, extras.getCharSequence(Notification.EXTRA_TITLE));
        add(parts, extras.getCharSequence(Notification.EXTRA_TEXT));
        add(parts, extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (parts.isEmpty()) return;

        String rawText = String.join(" ", parts);
        Transaction transaction = null;
        for (String part : parts) {
            transaction = provider.parser.parse(notification.getPostTime(), part);
            if (transaction != null) break;
        }
        if (transaction == null) transaction = provider.parser.parse(notification.getPostTime(), rawText);
        if (transaction == null) return;

        String payload;
        try {
            PayloadMode mode = Settings.PAYLOAD_FULL.equals(Settings.payloadMode(this)) ? PayloadMode.FULL : PayloadMode.MINIMAL;
            payload = WebhookPayload.from(transaction, mode, rawText);
        } catch (IllegalArgumentException invalidParserOutput) {
            return;
        }
        PersistentDeliveryQueue queue = new PersistentDeliveryQueue(
                PersistentDeliveryQueue.preferences(this, "delivery_queue"));
        if (queue.enqueue(transaction.id, payload)) {
            try {
                new RecentTransactionLog(RecentTransactionLog.preferences(this)).record(transaction, provider.label);
            } catch (RuntimeException ignored) {
                // Observability must never block webhook delivery.
            }
            DeliveryRunner.start(this);
        }
    }

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network network) {
            DeliveryRunner.start(NotificationBridgeListener.this);
        }
    };

    private static void add(Set<String> parts, CharSequence value) {
        if (value != null && !value.toString().trim().isEmpty()) parts.add(value.toString().trim());
    }
}
