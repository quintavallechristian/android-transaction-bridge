package io.github.transactionbridge;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.window.OnBackInvokedDispatcher;
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

import static io.github.transactionbridge.Ui.*;

/** Setup, status, and settings UI; delivery remains owned by the bridge core. */
public final class MainActivity extends Activity {
    private LinearLayout content;
    private boolean settingsVisible;
    private Ui ui;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ui = new Ui(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
        showHome();
    }

    @SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() {
        handleBack();
    }

    private void handleBack() {
        if (settingsVisible) showHome();
        else finishAfterTransition();
    }

    private void showHome() {
        settingsVisible = false;
        content = ui.contentColumn();
        content.addView(ui.text("A private bridge from payment notifications to your webhook.", MUTED, 16), ui.margin(0, 0, 0, 28));

        if (!Settings.isConfigured(this)) {
            content.addView(ui.sectionTitle("Get started"), ui.margin(0, 0, 0, 8));
            content.addView(ui.text("Add an HTTPS webhook. Bearer authentication is optional.", MUTED, 14), ui.margin(0, 0, 0, 16));
            content.addView(ui.primaryButton("Configure bridge", v -> showSettings()), ui.margin(0, 0, 0, 0));
        } else {
            content.addView(ui.statusPanel("Ready", "Webhook configured. Supported payments are queued locally and delivered securely."), ui.margin(0, 0, 0, 28));
            content.addView(ui.sectionTitle("Delivery"), ui.margin(0, 0, 0, 12));

            PersistentDeliveryQueue queue = new PersistentDeliveryQueue(PersistentDeliveryQueue.preferences(this, "delivery_queue"));
            int pending = queue.size();
            content.addView(ui.metric("Pending deliveries", pending), ui.margin(0, 0, 0, 8));
            if (pending > 0) content.addView(ui.secondaryButton("Retry pending delivery", v -> {
                queue.retryFirstNow();
                DeliveryRunner.start(this);
                showHome();
            }), ui.margin(0, 4, 0, 12));

            AttentionLog attention = new AttentionLog(AttentionLog.preferences(this, "delivery_attention"));
            int rejected = attention.count();
            content.addView(ui.metric("Needs attention", rejected), ui.margin(0, 0, 0, 8));
            if (rejected > 0) {
                content.addView(ui.secondaryButton("Retry first rejected delivery", v -> {
                    attention.requeueFirst(queue);
                    DeliveryRunner.start(this);
                    showHome();
                }), ui.margin(0, 4, 0, 8));
                content.addView(ui.textButton("Delete first rejected delivery", v -> {
                    attention.deleteFirst();
                    showHome();
                }), ui.margin(0, 0, 0, 0));
            }

            content.addView(ui.sectionTitle("Recent notifications"), ui.margin(0, 28, 0, 6));
            content.addView(ui.text("The latest supported payments recognized on this device. Notification text is not stored here.",
                    MUTED, 14), ui.margin(0, 0, 0, 14));
            Set<String> pendingIds = queue.ids();
            Set<String> attentionIds = new LinkedHashSet<>();
            for (AttentionLog.Entry entry : attention.entries()) attentionIds.add(entry.id);
            java.util.List<RecentTransactionLog.Entry> recent =
                    new RecentTransactionLog(RecentTransactionLog.preferences(this)).entries();
            if (recent.isEmpty()) {
                content.addView(ui.text("No supported notifications recognized yet.", MUTED, 14), ui.margin(0, 0, 0, 0));
            } else {
                for (RecentTransactionLog.Entry entry : recent) {
                    String status = "Delivered";
                    int statusColor = SUCCESS;
                    if (attentionIds.contains(entry.id)) {
                        status = "Needs attention";
                        statusColor = ERROR;
                    } else if (pendingIds.contains(entry.id)) {
                        status = "Queued";
                        statusColor = WARNING;
                    }
                    content.addView(ui.recentItem(entry, status, statusColor), ui.margin(0, 0, 0, 10));
                }
            }
        }

        LinearLayout actions = ui.actionBar();
        if (Settings.isConfigured(this)) {
            actions.addView(ui.primaryButton("Settings", v -> showSettings()), ui.margin(0, 0, 0, 8));
            actions.addView(ui.secondaryButton("Notification access", v -> openNotificationSettings()), ui.margin(0, 0, 0, 0));
        }
        setScreen("Transaction Bridge", content, actions.getChildCount() == 0 ? null : actions);
    }

    private void showSettings() {
        settingsVisible = true;
        content = ui.contentColumn();

        content.addView(ui.sectionTitle("Connection"), ui.margin(0, 0, 0, 6));
        content.addView(ui.text("Where approved transactions are delivered.", MUTED, 14), ui.margin(0, 0, 0, 16));
        content.addView(ui.label("HTTPS webhook URL"), ui.margin(0, 0, 0, 6));
        EditText endpoint = ui.field("https://example.com/webhook", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        endpoint.setText(Settings.endpoint(this));
        content.addView(endpoint, ui.margin(0, 0, 0, 14));
        content.addView(ui.label("Bearer token"), ui.margin(0, 0, 0, 6));
        EditText token = ui.field("Optional · leave blank to keep", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(token, ui.margin(0, 0, 0, 8));
        content.addView(ui.text("Encrypted with Android Keystore and never displayed after saving.", MUTED, 13), ui.margin(0, 0, 0, 32));

        content.addView(ui.sectionTitle("Payload"), ui.margin(0, 0, 0, 6));
        content.addView(ui.text("Choose how much notification data leaves this device.", MUTED, 14), ui.margin(0, 0, 0, 10));
        RadioGroup payload = new RadioGroup(this);
        RadioButton minimal = ui.radio("Minimal · recommended", Settings.PAYLOAD_MINIMAL.equals(Settings.payloadMode(this)));
        RadioButton full = ui.radio("Full · includes notification text", Settings.PAYLOAD_FULL.equals(Settings.payloadMode(this)));
        payload.addView(minimal, ui.controlParams());
        payload.addView(full, ui.controlParams());
        content.addView(payload, ui.margin(0, 0, 0, 28));

        content.addView(ui.sectionTitle("Payment sources"), ui.margin(0, 0, 0, 6));
        content.addView(ui.text("Only enabled apps are inspected for supported payment notifications.", MUTED, 14), ui.margin(0, 0, 0, 10));
        Map<String, CheckBox> checks = new LinkedHashMap<>();
        Map<String, String> walletCards = new LinkedHashMap<>(Settings.walletCards(this));
        ParserRegistry registry = ParserRegistry.defaultRegistry(walletCards);
        for (ParserRegistry.Provider provider : registry.providers()) {
            addSource(checks, provider.label, provider.settingKey);
        }

        LinearLayout walletSection = ui.column();
        walletSection.addView(ui.sectionTitle("Google Wallet cards"), ui.margin(0, 28, 0, 6));
        walletSection.addView(ui.text("Wallet reveals only the last four digits. Give each card a local name; unknown cards are ignored.", MUTED, 14), ui.margin(0, 0, 0, 16));

        LinearLayout walletForm = ui.panel();
        walletForm.addView(ui.label("Last 4 digits"), ui.margin(0, 0, 0, 6));
        EditText cardDigits = ui.field("1234", InputType.TYPE_CLASS_NUMBER);
        cardDigits.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        walletForm.addView(cardDigits, ui.margin(0, 0, 0, 14));
        walletForm.addView(ui.label("Card name"), ui.margin(0, 0, 0, 6));
        EditText cardName = ui.field("For example: Personal ING", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        walletForm.addView(cardName, ui.margin(0, 0, 0, 14));
        LinearLayout configuredCards = ui.column();
        walletForm.addView(ui.primaryButton("Add or update card", v -> {
            try {
                Settings.putWalletCard(walletCards, cardDigits.getText().toString(), cardName.getText().toString());
                cardDigits.setText("");
                cardName.setText("");
                renderWalletCards(configuredCards, walletCards);
            } catch (IllegalArgumentException error) {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }), ui.margin(0, 0, 0, 12));
        walletForm.addView(configuredCards, ui.margin(0, 0, 0, 0));
        renderWalletCards(configuredCards, walletCards);
        walletSection.addView(walletForm, ui.margin(0, 0, 0, 24));
        content.addView(walletSection);
        CheckBox walletCheck = checks.get(registry.providerFor(ParserRegistry.GOOGLE_WALLET_PACKAGE).settingKey);
        walletSection.setVisibility(walletCheck.isChecked() ? View.VISIBLE : View.GONE);
        walletCheck.setOnCheckedChangeListener((button, checked) ->
                walletSection.setVisibility(checked ? View.VISIBLE : View.GONE));

        LinearLayout actions = ui.actionBar();
        actions.addView(ui.primaryButton("Save changes", v -> {
            try {
                Settings.save(this, endpoint.getText().toString(), token.getText().toString());
                Set<String> sources = new LinkedHashSet<>();
                for (Map.Entry<String, CheckBox> item : checks.entrySet()) if (item.getValue().isChecked()) sources.add(item.getKey());
                Settings.saveSources(this, sources);
                Settings.savePayloadMode(this, full.isChecked() ? Settings.PAYLOAD_FULL : Settings.PAYLOAD_MINIMAL);
                Settings.saveWalletCards(this, walletCards);
                NotificationBridgeListener.refreshConfiguration();
                DeliveryRunner.start(this);
                showHome();
            } catch (IllegalArgumentException | IllegalStateException error) {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }), ui.margin(0, 0, 0, 8));

        LinearLayout secondaryActions = new LinearLayout(this);
        secondaryActions.setOrientation(LinearLayout.HORIZONTAL);
        secondaryActions.addView(ui.textButton("Notification access", v -> openNotificationSettings()), new LinearLayout.LayoutParams(0, ui.dp(48), 1));
        secondaryActions.addView(ui.textButton("Back", v -> showHome()), new LinearLayout.LayoutParams(0, ui.dp(48), 1));
        actions.addView(secondaryActions, ui.margin(0, 0, 0, 0));
        setScreen("Settings", content, actions);
    }

    private void setScreen(String title, View body, View actions) {
        LinearLayout screen = ui.column();
        screen.setBackgroundColor(SURFACE);

        TextView appBar = ui.text(title, INK, 28);
        appBar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(8));
        screen.addView(appBar, new LinearLayout.LayoutParams(-1, ui.dp(64)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(body);
        screen.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (actions != null) screen.addView(actions, new LinearLayout.LayoutParams(-1, -2));
        setContentView(screen);
        applySystemInsets(screen);
    }

    private void applySystemInsets(View view) {
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            target.setPadding(left, top, right, bottom);
            return insets;
        });
        view.requestApplyInsets();
    }

    private void addSource(Map<String, CheckBox> checks, String label, String source) {
        CheckBox check = ui.sourceCheck(label, Settings.sourceEnabled(this, source));
        checks.put(source, check);
        content.addView(check, ui.margin(0, 0, 0, 0));
    }

    private void openNotificationSettings() {
        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    private void renderWalletCards(LinearLayout container, Map<String, String> cards) {
        container.removeAllViews();
        if (cards.isEmpty()) {
            container.addView(ui.text("No cards configured yet.", MUTED, 13), ui.margin(0, 0, 0, 0));
            return;
        }
        for (Map.Entry<String, String> card : new LinkedHashMap<>(cards).entrySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(ui.dp(52));
            TextView cardLabel = ui.text(card.getValue() + "  ·  •••• " + card.getKey(), INK, 14);
            row.addView(cardLabel, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(ui.textButton("Remove", v -> {
                cards.remove(card.getKey());
                renderWalletCards(container, cards);
            }), new LinearLayout.LayoutParams(-2, ui.dp(48)));
            container.addView(row, ui.margin(0, 4, 0, 0));
        }
    }
}
