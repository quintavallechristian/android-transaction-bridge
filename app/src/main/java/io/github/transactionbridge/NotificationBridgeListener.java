package io.github.transactionbridge;

import android.app.Notification;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONException;

import java.util.LinkedHashSet;
import java.util.Set;

/** Android-only adapter: extracts notification text, then hands pure data to the bridge core. */
public final class NotificationBridgeListener extends NotificationListenerService {
    public interface Handler {
        void onQueued(Context context, Transaction transaction, String payload);
        void onNetworkAvailable(Context context);
    }

    private static volatile Handler handler;
    private static volatile NotificationBridgeListener connected;
    private ConnectivityManager connectivity;
    private ParserRegistry registry;

    public static void setHandler(Handler value) {
        handler = value;
    }

    public static void refreshConfiguration() {
        NotificationBridgeListener value = connected;
        if (value != null) value.registry = ParserRegistry.defaultRegistry(Settings.walletCards(value));
    }

    @Override public void onCreate() {
        super.onCreate();
        DeliveryRunner.install(this);
        registry = ParserRegistry.defaultRegistry(Settings.walletCards(this));
        connectivity = getSystemService(ConnectivityManager.class);
        if (connectivity != null) connectivity.registerDefaultNetworkCallback(networkCallback);
    }

    @Override public void onListenerConnected() {
        connected = this;
        registry = ParserRegistry.defaultRegistry(Settings.walletCards(this));
        Handler value = handler;
        if (value != null) value.onNetworkAvailable(this);
    }

    @Override public void onListenerDisconnected() {
        if (connected == this) connected = null;
    }

    @Override public void onDestroy() {
        if (connected == this) connected = null;
        if (connectivity != null) connectivity.unregisterNetworkCallback(networkCallback);
        super.onDestroy();
    }

    @Override public void onNotificationPosted(StatusBarNotification notification) {
        if (notification == null || registry == null) return;
        String packageName = notification.getPackageName();
        String source = sourceForPackage(packageName);
        if (source == null || !Settings.sourceEnabled(this, source)) return;

        Bundle extras = notification.getNotification().extras;
        Set<String> parts = new LinkedHashSet<>();
        add(parts, extras.getCharSequence(Notification.EXTRA_TITLE));
        add(parts, extras.getCharSequence(Notification.EXTRA_TEXT));
        add(parts, extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (parts.isEmpty()) return;

        String rawText = String.join(" ", parts);
        Transaction transaction = null;
        NotificationParser parser = registry.parserFor(packageName);
        for (String part : parts) {
            transaction = parser.parse(notification.getPostTime(), part);
            if (transaction != null) break;
        }
        if (transaction == null) transaction = parser.parse(notification.getPostTime(), rawText);
        if (transaction == null) return;

        // Keep the complete notification only for the opt-in full payload mode.
        if (Settings.PAYLOAD_FULL.equals(Settings.payloadMode(this))) {
            transaction = new Transaction(transaction.occurredAt, transaction.amount,
                    transaction.currency, transaction.merchant, rawText, transaction.source);
        }
        try {
            PayloadMode mode = Settings.PAYLOAD_FULL.equals(Settings.payloadMode(this))
                    ? PayloadMode.FULL : PayloadMode.MINIMAL;
            String payload = WebhookPayload.from(transaction, mode).toString();
            PersistentDeliveryQueue queue = new PersistentDeliveryQueue(
                    PersistentDeliveryQueue.preferences(this, "delivery_queue"));
            if (queue.enqueue(transaction.id, payload)) {
                Handler value = handler;
                if (value != null) value.onQueued(this, transaction, payload);
            }
        } catch (JSONException | RuntimeException ignored) {
            // Invalid parser output is ignored; it must not poison the durable queue.
        }
    }

    public static int replayActiveNotifications() {
        NotificationBridgeListener value = connected;
        if (value == null) return -1;
        StatusBarNotification[] notifications = value.getActiveNotifications();
        if (notifications == null) return 0;
        int replayed = 0;
        for (StatusBarNotification notification : notifications) {
            if (value.registry != null && value.registry.supports(notification.getPackageName())) {
                value.onNotificationPosted(notification);
                replayed++;
            }
        }
        return replayed;
    }

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network network) {
            Handler value = handler;
            if (value != null) value.onNetworkAvailable(NotificationBridgeListener.this);
        }
    };

    private static String sourceForPackage(String packageName) {
        if (ParserRegistry.ING_PACKAGE.equals(packageName)) return Settings.SOURCE_ING;
        if (ParserRegistry.ISYBANK_PACKAGE.equals(packageName)) return Settings.SOURCE_ISYBANK;
        if (ParserRegistry.REVOLUT_PACKAGE.equals(packageName)) return Settings.SOURCE_REVOLUT;
        if (ParserRegistry.CRYPTO_COM_PACKAGE.equals(packageName)) return Settings.SOURCE_CRYPTO_COM;
        if (ParserRegistry.GOOGLE_WALLET_PACKAGE.equals(packageName)) return Settings.SOURCE_GOOGLE_WALLET;
        return null;
    }

    private static void add(Set<String> parts, CharSequence value) {
        if (value != null && !value.toString().trim().isEmpty()) parts.add(value.toString().trim());
    }
}
