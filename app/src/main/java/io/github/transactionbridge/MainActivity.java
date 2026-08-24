package io.github.transactionbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Minimal setup/status/settings screen; delivery remains owned by the bridge core. */
public final class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(49, 94, 251);
    private static final int INK = Color.rgb(15, 23, 42);
    private static final int MUTED = Color.rgb(71, 85, 105);
    private LinearLayout root;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showHome();
    }

    private void showHome() {
        root = column();
        root.setPadding(dp(24), dp(32), dp(24), dp(24));
        root.addView(title("Transaction Bridge"), margin(0, 0, 0, 8));
        root.addView(text("A local Android notification bridge for HTTPS webhooks.", MUTED, 16), margin(0, 0, 0, 24));

        if (!io.github.transactionbridge.Settings.isConfigured(this)) {
            root.addView(text("Set an HTTPS webhook to begin. Bearer authentication is optional.", INK, 16), margin(0, 0, 0, 16));
            root.addView(button("Open settings", v -> showSettings()), margin(0, 0, 0, 12));
        } else {
            root.addView(card("Ready", "Webhook configured. The bridge keeps pending deliveries locally and sends only supported notifications."), margin(0, 0, 0, 12));
            root.addView(button("Notification access", v -> openNotificationSettings()), margin(0, 0, 0, 12));
            root.addView(button("Settings", v -> showSettings()), margin(0, 0, 0, 12));
            PersistentDeliveryQueue queue = new PersistentDeliveryQueue(PersistentDeliveryQueue.preferences(this, "delivery_queue"));
            root.addView(text("Pending deliveries: " + queue.size(), MUTED, 14), margin(0, 12, 0, 0));
            if (queue.size() > 0) root.addView(button("Retry pending delivery", v -> {
                queue.retryFirstNow();
                DeliveryRunner.install(this);
                showHome();
            }), margin(0, 8, 0, 0));
            AttentionLog attention = new AttentionLog(AttentionLog.preferences(this, "delivery_attention"));
            root.addView(text("Needs attention: " + attention.count(), MUTED, 14), margin(0, 12, 0, 0));
            if (attention.count() > 0) {
                root.addView(button("Retry first rejected delivery", v -> {
                    attention.requeueFirst(queue);
                    DeliveryRunner.install(this);
                    showHome();
                }), margin(0, 8, 0, 0));
                root.addView(button("Delete first rejected delivery", v -> {
                    attention.deleteFirst();
                    showHome();
                }), margin(0, 8, 0, 0));
            }
        }
        setContentView(scroll(root));
    }

    private void showSettings() {
        root = column();
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.addView(title("Settings"), margin(0, 0, 0, 20));

        EditText endpoint = field("HTTPS webhook URL", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        endpoint.setText(io.github.transactionbridge.Settings.endpoint(this));
        root.addView(endpoint, margin(0, 0, 0, 12));
        EditText token = field("Bearer token (optional; blank keeps current)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(token, margin(0, 0, 0, 8));
        root.addView(text("The token is encrypted with Android Keystore. It is never displayed after saving.", MUTED, 13), margin(0, 0, 0, 20));

        root.addView(text("Payload", INK, 16), margin(0, 0, 0, 4));
        RadioGroup payload = new RadioGroup(this);
        RadioButton minimal = radio("Minimal (recommended)", "minimal".equals(io.github.transactionbridge.Settings.payloadMode(this)));
        RadioButton full = radio("Full (includes notification text)", "full".equals(io.github.transactionbridge.Settings.payloadMode(this)));
        payload.addView(minimal);
        payload.addView(full);
        root.addView(payload, margin(0, 0, 0, 16));

        root.addView(text("Sources", INK, 16), margin(0, 0, 0, 4));
        Map<String, CheckBox> checks = new LinkedHashMap<>();
        addSource(checks, "ING", io.github.transactionbridge.Settings.SOURCE_ING);
        addSource(checks, "IsyBank", io.github.transactionbridge.Settings.SOURCE_ISYBANK);
        addSource(checks, "Revolut", io.github.transactionbridge.Settings.SOURCE_REVOLUT);
        addSource(checks, "Crypto.com", io.github.transactionbridge.Settings.SOURCE_CRYPTO_COM);
        addSource(checks, "Coverflex", io.github.transactionbridge.Settings.SOURCE_COVERFLEX);
        addSource(checks, "Google Wallet", io.github.transactionbridge.Settings.SOURCE_GOOGLE_WALLET);

        EditText cards = field("Wallet cards: 1234=source (one per line)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        cards.setSingleLine(false);
        cards.setMinLines(3);
        cards.setText(formatCards(io.github.transactionbridge.Settings.walletCards(this)));
        root.addView(cards, margin(0, 8, 0, 20));

        root.addView(button("Save", v -> {
            try {
                io.github.transactionbridge.Settings.save(this, endpoint.getText().toString(), token.getText().toString());
                Set<String> sources = new LinkedHashSet<>();
                for (Map.Entry<String, CheckBox> item : checks.entrySet()) if (item.getValue().isChecked()) sources.add(item.getKey());
                io.github.transactionbridge.Settings.saveSources(this, sources);
                io.github.transactionbridge.Settings.savePayloadMode(this, full.isChecked() ? io.github.transactionbridge.Settings.PAYLOAD_FULL : io.github.transactionbridge.Settings.PAYLOAD_MINIMAL);
                io.github.transactionbridge.Settings.saveWalletCards(this, cards.getText().toString());
                DeliveryRunner.install(this);
                showHome();
            } catch (IllegalArgumentException | IllegalStateException error) {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }), margin(0, 0, 0, 12));
        root.addView(button("Notification access", v -> openNotificationSettings()), margin(0, 0, 0, 12));
        root.addView(button("Back", v -> showHome()), margin(0, 0, 0, 12));
        setContentView(scroll(root));
    }

    private void addSource(Map<String, CheckBox> checks, String label, String source) {
        CheckBox check = new CheckBox(this);
        check.setText(label);
        check.setTextColor(INK);
        check.setChecked(io.github.transactionbridge.Settings.sourceEnabled(this, source));
        checks.put(source, check);
        root.addView(check, margin(0, 0, 0, 0));
    }

    private void openNotificationSettings() {
        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    private LinearLayout column() {
        LinearLayout value = new LinearLayout(this);
        value.setOrientation(LinearLayout.VERTICAL);
        value.setBackgroundColor(Color.rgb(248, 250, 252));
        return value;
    }

    private ScrollView scroll(View content) {
        ScrollView value = new ScrollView(this);
        value.addView(content);
        return value;
    }

    private TextView title(String value) {
        TextView text = text(value, INK, 28);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        return text;
    }

    private TextView text(String value, int color, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(size);
        return text;
    }

    private EditText field(String hint, int inputType) {
        EditText value = new EditText(this);
        value.setHint(hint);
        value.setTextColor(INK);
        value.setHintTextColor(MUTED);
        value.setInputType(inputType);
        value.setPadding(dp(12), 0, dp(12), 0);
        return value;
    }

    private RadioButton radio(String label, boolean checked) {
        RadioButton value = new RadioButton(this);
        value.setText(label);
        value.setTextColor(INK);
        value.setChecked(checked);
        return value;
    }

    private Button button(String label, View.OnClickListener action) {
        Button value = new Button(this);
        value.setText(label);
        value.setTextColor(Color.WHITE);
        value.setAllCaps(false);
        value.setBackgroundColor(BLUE);
        value.setOnClickListener(action);
        return value;
    }

    private LinearLayout card(String heading, String body) {
        LinearLayout value = column();
        value.setPadding(dp(16), dp(16), dp(16), dp(16));
        value.addView(text(heading, INK, 18), margin(0, 0, 0, 6));
        value.addView(text(body, MUTED, 14), margin(0, 0, 0, 0));
        return value;
    }

    private LinearLayout.LayoutParams margin(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams value = new LinearLayout.LayoutParams(-1, -2);
        value.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return value;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static String formatCards(Map<String, String> cards) {
        StringBuilder value = new StringBuilder();
        for (Map.Entry<String, String> item : cards.entrySet()) {
            if (value.length() > 0) value.append('\n');
            value.append(item.getKey()).append('=').append(item.getValue());
        }
        return value.toString();
    }
}
