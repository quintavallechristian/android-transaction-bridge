package io.github.transactionbridge;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

/** Small view factory; MainActivity owns navigation and actions, not widget styling. */
final class Ui {
    static final int BLUE = Color.rgb(49, 94, 251);
    static final int INK = Color.rgb(15, 23, 42);
    static final int MUTED = Color.rgb(71, 85, 105);
    static final int SURFACE = Color.rgb(248, 250, 252);
    static final int OUTLINE = Color.rgb(203, 213, 225);
    static final int SUCCESS = Color.rgb(21, 128, 61);
    static final int WARNING = Color.rgb(161, 98, 7);
    static final int ERROR = Color.rgb(185, 28, 28);

    private static final ColorStateList CONTROL_TINT = new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{BLUE, MUTED});
    private final Context context;

    Ui(Context context) { this.context = context; }

    LinearLayout column() {
        LinearLayout value = new LinearLayout(context);
        value.setOrientation(LinearLayout.VERTICAL);
        return value;
    }

    LinearLayout contentColumn() {
        LinearLayout value = column();
        value.setPadding(dp(20), dp(16), dp(20), dp(32));
        return value;
    }

    LinearLayout actionBar() {
        LinearLayout value = column();
        value.setPadding(dp(20), dp(12), dp(20), dp(12));
        value.setBackgroundColor(Color.WHITE);
        value.setElevation(dp(8));
        return value;
    }

    TextView sectionTitle(String value) {
        TextView title = text(value, INK, 20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return title;
    }

    TextView label(String value) {
        TextView label = text(value, INK, 13);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    TextView text(String value, int color, int size) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(size);
        text.setLineSpacing(0, 1.08f);
        return text;
    }

    EditText field(String hint, int inputType) {
        EditText value = new EditText(context);
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

    RadioButton radio(String label, boolean checked) {
        RadioButton value = new RadioButton(context);
        value.setText(label);
        value.setTextColor(INK);
        value.setTextSize(16);
        value.setId(View.generateViewId());
        value.setChecked(checked);
        value.setButtonTintList(CONTROL_TINT);
        value.setMinHeight(dp(48));
        return value;
    }

    CheckBox sourceCheck(String label, boolean checked) {
        CheckBox value = new CheckBox(context);
        value.setText(label);
        value.setTextColor(INK);
        value.setTextSize(16);
        value.setChecked(checked);
        value.setButtonTintList(CONTROL_TINT);
        value.setMinHeight(dp(48));
        return value;
    }

    Button primaryButton(String label, View.OnClickListener action) {
        Button value = button(label, action);
        value.setTextColor(Color.WHITE);
        value.setBackground(shape(BLUE, BLUE, 12));
        return value;
    }

    Button secondaryButton(String label, View.OnClickListener action) {
        Button value = button(label, action);
        value.setTextColor(BLUE);
        value.setBackground(shape(Color.WHITE, OUTLINE, 12));
        return value;
    }

    Button textButton(String label, View.OnClickListener action) {
        Button value = button(label, action);
        value.setTextColor(BLUE);
        value.setBackgroundColor(Color.TRANSPARENT);
        return value;
    }

    LinearLayout statusPanel(String heading, String body) {
        LinearLayout value = panel();
        TextView title = text(heading, INK, 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.addView(title, margin(0, 0, 0, 6));
        value.addView(text(body, MUTED, 14), margin(0, 0, 0, 0));
        return value;
    }

    LinearLayout panel() {
        LinearLayout value = column();
        value.setPadding(dp(16), dp(16), dp(16), dp(16));
        value.setBackground(shape(Color.WHITE, OUTLINE, 16));
        return value;
    }

    LinearLayout metric(String label, int count) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(48));
        row.addView(text(label, INK, 16), new LinearLayout.LayoutParams(0, -2, 1));
        TextView value = text(String.valueOf(count), MUTED, 16);
        value.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        row.addView(value);
        return row;
    }

    LinearLayout recentItem(RecentTransactionLog.Entry entry, String status, int statusColor) {
        LinearLayout item = panel();
        LinearLayout summary = new LinearLayout(context);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.setGravity(Gravity.TOP);
        TextView merchant = text(entry.merchant, INK, 16);
        merchant.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        summary.addView(merchant, new LinearLayout.LayoutParams(0, -2, 1));
        TextView amount = text(entry.amount + " " + entry.currency, INK, 16);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        summary.addView(amount, new LinearLayout.LayoutParams(-2, -2));
        item.addView(summary, margin(0, 0, 0, 6));
        String occurredAt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(entry.occurredAt));
        item.addView(text(entry.provider + " · " + occurredAt, MUTED, 13), margin(0, 0, 0, 8));
        TextView state = text(status, statusColor, 13);
        state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        item.addView(state, margin(0, 0, 0, 0));
        return item;
    }

    LinearLayout.LayoutParams controlParams() { return new LinearLayout.LayoutParams(-1, dp(48)); }

    LinearLayout.LayoutParams margin(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams value = new LinearLayout.LayoutParams(-1, -2);
        value.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return value;
    }

    int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    private Button button(String label, View.OnClickListener action) {
        Button value = new Button(context);
        value.setText(label);
        value.setTextSize(15);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setAllCaps(false);
        value.setMinHeight(dp(52));
        value.setOnClickListener(action);
        return value;
    }

    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable value = new GradientDrawable();
        value.setColor(fill);
        value.setCornerRadius(dp(radius));
        value.setStroke(dp(1), stroke);
        return value;
    }
}
