package io.github.transactionbridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bridges listener callbacks to one durable, FIFO delivery worker. */
public final class DeliveryRunner implements NotificationBridgeListener.Handler {
    private static final String QUEUE_FILE = "delivery_queue";
    private static final String ATTENTION_FILE = "delivery_attention";
    private static final String STATE_FILE = "delivery_state";
    private static final String SUSPENDED_CREDENTIALS = "suspended_credentials";
    private static final long MAX_SCHEDULE_MS = 7_200_000L;
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static final AtomicBoolean REQUESTED = new AtomicBoolean();
    private static final DeliveryRunner INSTANCE = new DeliveryRunner();

    private final Handler scheduler = new Handler(Looper.getMainLooper());

    private DeliveryRunner() {}

    public static void install(Context context) {
        NotificationBridgeListener.setHandler(INSTANCE);
        INSTANCE.request(context);
    }

    @Override public void onQueued(Context context, Transaction transaction, String payload) { request(context); }

    @Override public void onNetworkAvailable(Context context) { request(context); }

    private void request(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        String credentials = credentialFingerprint(Settings.endpoint(app), Settings.token(app));
        SharedPreferences state = app.getSharedPreferences(STATE_FILE, Context.MODE_PRIVATE);
        String blockedCredentials = state.getString(SUSPENDED_CREDENTIALS, "");
        if (!blockedCredentials.isEmpty()) {
            if (credentials.equals(blockedCredentials)) return;
            state.edit().remove(SUSPENDED_CREDENTIALS).apply();
        }
        REQUESTED.set(true);
        if (!Settings.isConfigured(app) || !RUNNING.compareAndSet(false, true)) return;
        new Thread(() -> run(app), "transaction-bridge-delivery").start();
    }

    private void run(Context context) {
        REQUESTED.set(false);
        try {
            PersistentDeliveryQueue queue = new PersistentDeliveryQueue(PersistentDeliveryQueue.preferences(context, QUEUE_FILE));
            AttentionLog attention = new AttentionLog(AttentionLog.preferences(context, ATTENTION_FILE));
            WebhookUploader uploader = new WebhookUploader(queue, new WebhookClient(), attention);
            while (true) {
                DeliveryRecord head = queue.peek();
                if (head == null) return;
                long now = System.currentTimeMillis();
                DeliveryOutcome outcome = uploader.deliverNext(Settings.endpoint(context), Settings.token(context), now);
                if (outcome.state == DeliveryOutcome.State.DELIVERED || outcome.state == DeliveryOutcome.State.NEEDS_ATTENTION) continue;
                if (outcome.state == DeliveryOutcome.State.RETRY_SCHEDULED) {
                    schedule(context, Math.min(MAX_SCHEDULE_MS, Math.max(0, outcome.retryDelayMillis)));
                }
                if (outcome.state == DeliveryOutcome.State.SUSPENDED) {
                    context.getSharedPreferences(STATE_FILE, Context.MODE_PRIVATE).edit()
                            .putString(SUSPENDED_CREDENTIALS, credentialFingerprint(Settings.endpoint(context), Settings.token(context)))
                            .apply();
                }
                return;
            }
        } finally {
            RUNNING.set(false);
            if (REQUESTED.get()) request(context);
        }
    }

    private void schedule(Context context, long delayMillis) {
        scheduler.postDelayed(() -> request(context), delayMillis);
    }

    private static String credentialFingerprint(String endpoint, String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest((endpoint + "\n" + token).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
