package io.github.transactionbridge;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.window.OnBackInvokedDispatcher;
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

/** Setup, status, and settings UI; delivery remains owned by the bridge core. */
public final class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(49, 94, 251);
    private static final int INK = Color.rgb(15, 23, 42);
    private static final int MUTED = Color.rgb(71, 85, 105);
    private static final int SURFACE = Color.rgb(248, 250, 252);
    private static final int OUTLINE = Color.rgb(203, 213, 225);
    private static final ColorStateList CONTROL_TINT = new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{BLUE, MUTED});
    private LinearLayout content;
    private boolean settingsVisible;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        content = contentColumn();
        content.addView(text("A private bridge from payment notifications to your webhook.", MUTED, 16), margin(0, 0, 0, 28));

        if (!Settings.isConfigured(this)) {
            content.addView(sectionTitle("Get started"), margin(0, 0, 0, 8));
            content.addView(text("Add an HTTPS webhook. Bearer authentication is optional.", MUTED, 14), margin(0, 0, 0, 16));
            content.addView(primaryButton("Configure bridge", v -> showSettings()), margin(0, 0, 0, 0));
        } else {
            content.addView(statusPanel("Ready", "Webhook configured. Supported payments are queued locally and delivered securely."), margin(0, 0, 0, 28));
            content.addView(sectionTitle("Delivery"), margin(0, 0, 0, 12));

            PersistentDeliveryQueue queue = new PersistentDeliveryQueue(PersistentDeliveryQueue.preferences(this, "delivery_queue"));
            int pending = queue.size();
            content.addView(metric("Pending deliveries", pending), margin(0, 0, 0, 8));
            if (pending > 0) content.addView(secondaryButton("Retry pending delivery", v -> {
                queue.retryFirstNow();
                DeliveryRunner.start(this);
                showHome();
            }), margin(0, 4, 0, 12));

            AttentionLog attention = new AttentionLog(AttentionLog.preferences(this, "delivery_attention"));
            int rejected = attention.count();
            content.addView(metric("Needs attention", rejected), margin(0, 0, 0, 8));
            if (rejected > 0) {
                content.addView(secondaryButton("Retry first rejected delivery", v -> {
                    attention.requeueFirst(queue);
                    DeliveryRunner.start(this);
                    showHome();
                }), margin(0, 4, 0, 8));
                content.addView(textButton("Delete first rejected delivery", v -> {
                    attention.deleteFirst();
                    showHome();
                }), margin(0, 0, 0, 0));
            }
        }

        LinearLayout actions = actionBar();
        if (Settings.isConfigured(this)) {
            actions.addView(primaryButton("Settings", v -> showSettings()), margin(0, 0, 0, 8));
            actions.addView(secondaryButton("Notification access", v -> openNotificationSettings()), margin(0, 0, 0, 0));
        }
        setScreen("Transaction Bridge", content, actions.getChildCount() == 0 ? null : actions);
    }

    private void showSettings() {
        settingsVisible = true;
        content = contentColumn();

        content.addView(sectionTitle("Connection"), margin(0, 0, 0, 6));
        content.addView(text("Where approved transactions are delivered.", MUTED, 14), margin(0, 0, 0, 16));
        content.addView(label("HTTPS webhook URL"), margin(0, 0, 0, 6));
        EditText endpoint = field("https://example.com/webhook", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        endpoint.setText(Settings.endpoint(this));
        content.addView(endpoint, margin(0, 0, 0, 14));
        content.addView(label("Bearer token"), margin(0, 0, 0, 6));
        EditText token = field("Optional · leave blank to keep", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(token, margin(0, 0, 0, 8));
        content.addView(text("Encrypted with Android Keystore and never displayed after saving.", MUTED, 13), margin(0, 0, 0, 32));

        content.addView(sectionTitle("Payload"), margin(0, 0, 0, 6));
        content.addView(text("Choose how much notification data leaves this device.", MUTED, 14), margin(0, 0, 0, 10));
        RadioGroup payload = new RadioGroup(this);
        RadioButton minimal = radio("Minimal · recommended", Settings.PAYLOAD_MINIMAL.equals(Settings.payloadMode(this)));
        RadioButton full = radio("Full · includes notification text", Settings.PAYLOAD_FULL.equals(Settings.payloadMode(this)));
        payload.addView(minimal, controlParams());
        payload.addView(full, controlParams());
        content.addView(payload, margin(0, 0, 0, 28));

        content.addView(sectionTitle("Payment sources"), margin(0, 0, 0, 6));
        content.addView(text("Only enabled apps are inspected for supported payment notifications.", MUTED, 14), margin(0, 0, 0, 10));
        Map<String, CheckBox> checks = new LinkedHashMap<>();
        Map<String, String> walletCards = new LinkedHashMap<>(Settings.walletCards(this));
        ParserRegistry registry = ParserRegistry.defaultRegistry(walletCards);
        for (ParserRegistry.Provider provider : registry.providers()) {
            addSource(checks, provider.label, provider.settingKey);
        }

        LinearLayout walletSection = column();
        walletSection.addView(sectionTitle("Google Wallet cards"), margin(0, 28, 0, 6));
        walletSection.addView(text("Wallet reveals only the last four digits. Give each card a local name; unknown cards are ignored.", MUTED, 14), margin(0, 0, 0, 16));

        LinearLayout walletForm = panel();
        walletForm.addView(label("Last 4 digits"), margin(0, 0, 0, 6));
        EditText cardDigits = field("1234", InputType.TYPE_CLASS_NUMBER);
        cardDigits.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        walletForm.addView(cardDigits, margin(0, 0, 0, 14));
        walletForm.addView(label("Card name"), margin(0, 0, 0, 6));
        EditText cardName = field("For example: Personal ING", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        walletForm.addView(cardName, margin(0, 0, 0, 14));
        LinearLayout configuredCards = column();
        walletForm.addView(primaryButton("Add or update card", v -> {
            try {
                Settings.putWalletCard(walletCards, cardDigits.getText().toString(), cardName.getText().toString());
                cardDigits.setText("");
                cardName.setText("");
                renderWalletCards(configuredCards, walletCards);
            } catch (IllegalArgumentException error) {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }), margin(0, 0, 0, 12));
        walletForm.addView(configuredCards, margin(0, 0, 0, 0));
        renderWalletCards(configuredCards, walletCards);
        walletSection.addView(walletForm, margin(0, 0, 0, 24));
        content.addView(walletSection);
        CheckBox walletCheck = checks.get(registry.providerFor(ParserRegistry.GOOGLE_WALLET_PACKAGE).settingKey);
        walletSection.setVisibility(walletCheck.isChecked() ? View.VISIBLE : View.GONE);
        walletCheck.setOnCheckedChangeListener((button, checked) ->
                walletSection.setVisibility(checked ? View.VISIBLE : View.GONE));

        LinearLayout actions = actionBar();
        actions.addView(primaryButton("Save changes", v -> {
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
        }), margin(0, 0, 0, 8));

        LinearLayout secondaryActions = new LinearLayout(this);
        secondaryActions.setOrientation(LinearLayout.HORIZONTAL);
        secondaryActions.addView(textButton("Notification access", v -> openNotificationSettings()), new LinearLayout.LayoutParams(0, dp(48), 1));
        secondaryActions.addView(textButton("Back", v -> showHome()), new LinearLayout.LayoutParams(0, dp(48), 1));
        actions.addView(secondaryActions, margin(0, 0, 0, 0));
        setScreen("Settings", content, actions);
    }

    private void setScreen(String title, View body, View actions) {
        LinearLayout screen = column();
        screen.setBackgroundColor(SURFACE);

        TextView appBar = text(title, INK, 28);
        appBar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setPadding(dp(20), dp(8), dp(20), dp(8));
        screen.addView(appBar, new LinearLayout.LayoutParams(-1, dp(64)));

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
        CheckBox check = new CheckBox(this);
        check.setText(label);
        check.setTextColor(INK);
        check.setTextSize(16);
        check.setChecked(Settings.sourceEnabled(this, source));
        check.setButtonTintList(CONTROL_TINT);
        check.setMinHeight(dp(48));
        checks.put(source, check);
        content.addView(check, margin(0, 0, 0, 0));
    }

    private void openNotificationSettings() {
        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    private LinearLayout column() {
        LinearLayout value = new LinearLayout(this);
        value.setOrientation(LinearLayout.VERTICAL);
        return value;
    }

    private LinearLayout contentColumn() {
        LinearLayout value = column();
        value.setPadding(dp(20), dp(16), dp(20), dp(32));
        return value;
    }

    private LinearLayout actionBar() {
        LinearLayout value = column();
        value.setPadding(dp(20), dp(12), dp(20), dp(12));
        value.setBackgroundColor(Color.WHITE);
        value.setElevation(dp(8));
        return value;
    }

    private TextView sectionTitle(String value) {
        TextView title = text(value, INK, 20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return title;
    }

    private TextView label(String value) {
        TextView label = text(value, INK, 13);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    private TextView text(String value, int color, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(size);
        text.setLineSpacing(0, 1.08f);
        return text;
    }

    private EditText field(String hint, int inputType) {
        EditText value = new EditText(this);
        value.setHint(hint);
        value.setTextColor(INK);
        value.setHintTextColor(MUTED);
        value.setTextSize(16);
        value.setSingleLine(true);
        value.setInputType(inputType);
        value.setTypeface(Typeface.DEFAULT);
        value.setPadding(dp(16), 0, dp(16), 0);
        value.setMinHeight(dp(56));
        value.setBackground(shape(Color.WHITE, OUTLINE, 12));
        return value;
    }

    private RadioButton radio(String label, boolean checked) {
        RadioButton value = new RadioButton(this);
        value.setText(label);
        value.setTextColor(INK);
        value.setTextSize(16);
        value.setId(View.generateViewId());
        value.setChecked(checked);
        value.setButtonTintList(CONTROL_TINT);
        value.setMinHeight(dp(48));
        return value;
    }

    private Button primaryButton(String label, View.OnClickListener action) {
        Button value = button(label, action);
        value.setTextColor(Color.WHITE);
        value.setBackground(shape(BLUE, BLUE, 12));
        return value;
    }

    private Button secondaryButton(String label, View.OnClickListener action) {
        Button value = button(label, action);
        value.setTextColor(BLUE);
        value.setBackground(shape(Color.WHITE, OUTLINE, 12));
        return value;
    }

    private Button textButton(String label, View.OnClickListener action) {
        Button value = button(label, action);
        value.setTextColor(BLUE);
        value.setBackgroundColor(Color.TRANSPARENT);
        return value;
    }

    private Button button(String label, View.OnClickListener action) {
        Button value = new Button(this);
        value.setText(label);
        value.setTextSize(15);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setAllCaps(false);
        value.setMinHeight(dp(52));
        value.setOnClickListener(action);
        return value;
    }

    private LinearLayout statusPanel(String heading, String body) {
        LinearLayout value = panel();
        TextView title = text(heading, INK, 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.addView(title, margin(0, 0, 0, 6));
        value.addView(text(body, MUTED, 14), margin(0, 0, 0, 0));
        return value;
    }

    private LinearLayout panel() {
        LinearLayout value = column();
        value.setPadding(dp(16), dp(16), dp(16), dp(16));
        value.setBackground(shape(Color.WHITE, OUTLINE, 16));
        return value;
    }

    private LinearLayout metric(String label, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(48));
        row.addView(text(label, INK, 16), new LinearLayout.LayoutParams(0, -2, 1));
        TextView value = text(String.valueOf(count), MUTED, 16);
        value.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        row.addView(value);
        return row;
    }

    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable value = new GradientDrawable();
        value.setColor(fill);
        value.setCornerRadius(dp(radius));
        value.setStroke(dp(1), stroke);
        return value;
    }

    private LinearLayout.LayoutParams controlParams() {
        return new LinearLayout.LayoutParams(-1, dp(48));
    }

    private LinearLayout.LayoutParams margin(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams value = new LinearLayout.LayoutParams(-1, -2);
        value.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void renderWalletCards(LinearLayout container, Map<String, String> cards) {
        container.removeAllViews();
        if (cards.isEmpty()) {
            container.addView(text("No cards configured yet.", MUTED, 13), margin(0, 0, 0, 0));
            return;
        }
        for (Map.Entry<String, String> card : new LinkedHashMap<>(cards).entrySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(52));
            TextView cardLabel = text(card.getValue() + "  ·  •••• " + card.getKey(), INK, 14);
            row.addView(cardLabel, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(textButton("Remove", v -> {
                cards.remove(card.getKey());
                renderWalletCards(container, cards);
            }), new LinearLayout.LayoutParams(-2, dp(48)));
            container.addView(row, margin(0, 4, 0, 0));
        }
    }
}
