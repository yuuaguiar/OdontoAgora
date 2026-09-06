package br.edu.odontoagora;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Reusable native views; shared card and page structures faithful to Figma. */
final class Ui {
    final Activity activity;
    final int text, muted, brand, green, yellow, red, cardColor;

    Ui(Activity activity) {
        this.activity = activity;
        text = activity.getColor(R.color.text);
        muted = activity.getColor(R.color.muted);
        brand = activity.getColor(R.color.brand);
        green = activity.getColor(R.color.success);
        yellow = activity.getColor(R.color.warning);
        red = activity.getColor(R.color.danger);
        cardColor = activity.getColor(R.color.card);
    }

    int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    LinearLayout.LayoutParams params(int height) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, height < 0 ? height : dp(height));
        p.bottomMargin = dp(12);
        return p;
    }

    TextView text(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView v = new TextView(activity);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(size);
        if (bold) v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        v.setLineSpacing(dp(3), 1);
        v.setLayoutParams(params(-2));
        parent.addView(v);
        return v;
    }

    TextView title(LinearLayout parent, String value) {
        return text(parent, value, 25, text, true);
    }

    TextView sub(LinearLayout parent, String value) {
        return text(parent, value, 14, muted, false);
    }

    Button button(LinearLayout parent, String value, boolean primary, Runnable action) {
        Button v = new Button(activity);
        v.setText(value);
        v.setTextSize(15);
        v.setAllCaps(false);
        v.setTextColor(text);
        v.setTypeface(null, Typeface.BOLD);
        v.setBackgroundResource(primary ? R.drawable.button_background : R.drawable.card_background);
        v.setMinHeight(dp(56));
        v.setPadding(dp(16), dp(14), dp(16), dp(14));
        v.setLayoutParams(params(-2));
        v.setOnClickListener(w -> action.run());
        parent.addView(v);
        return v;
    }

    EditText input(LinearLayout parent, String label, String value, int type, int id) {
        TextView caption = text(parent, label, 12, muted, true);
        EditText v = new EditText(activity);
        v.setId(id);
        caption.setLabelFor(id);
        v.setText(value);
        v.setTextColor(text);
        v.setHintTextColor(muted);
        v.setInputType(type);
        v.setTextSize(16);
        v.setPadding(dp(14), dp(12), dp(14), dp(12));
        v.setMinHeight(dp(54));
        v.setBackgroundResource(R.drawable.card_background);
        v.setLayoutParams(params(-2));
        parent.addView(v);
        return v;
    }

    LinearLayout card(LinearLayout parent, String label, String value, String detail, int color) {
        LinearLayout v = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.item_card, parent, false);
        ((TextView) v.findViewById(R.id.card_label)).setText(label);
        TextView main = v.findViewById(R.id.card_value);
        main.setText(value);
        main.setTextColor(color);
        TextView extra = v.findViewById(R.id.card_detail);
        extra.setText(detail);
        if (detail == null || detail.isEmpty()) extra.setVisibility(View.GONE);
        parent.addView(v);
        return v;
    }

    TextView pill(LinearLayout parent, String label, int bgColor, int textColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(bgColor);
        shape.setCornerRadius(dp(20));
        return pill(parent, label, shape, textColor);
    }

    TextView pill(LinearLayout parent, String label, GradientDrawable bgDrawable, int textColor) {
        TextView v = new TextView(activity);
        v.setText(label);
        v.setTextColor(textColor);
        v.setTextSize(11);
        v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        v.setPadding(dp(12), dp(6), dp(12), dp(6));
        v.setBackground(bgDrawable);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.bottomMargin = dp(10);
        v.setLayoutParams(p);
        parent.addView(v);
        return v;
    }

    LinearLayout rowCards(LinearLayout parent, String label1, String value1, int color1, String label2, String value2, int color2) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(12);
        row.setLayoutParams(p);

        LinearLayout c1 = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.item_card, row, false);
        ((TextView) c1.findViewById(R.id.card_label)).setText(label1);
        TextView m1 = c1.findViewById(R.id.card_value);
        m1.setText(value1);
        m1.setTextColor(color1);
        m1.setTextSize(17);
        c1.findViewById(R.id.card_detail).setVisibility(View.GONE);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, -2, 1f);
        lp1.rightMargin = dp(6);
        c1.setLayoutParams(lp1);
        row.addView(c1);

        LinearLayout c2 = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.item_card, row, false);
        ((TextView) c2.findViewById(R.id.card_label)).setText(label2);
        TextView m2 = c2.findViewById(R.id.card_value);
        m2.setText(value2);
        m2.setTextColor(color2);
        m2.setTextSize(17);
        c2.findViewById(R.id.card_detail).setVisibility(View.GONE);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, -2, 1f);
        lp2.leftMargin = dp(6);
        c2.setLayoutParams(lp2);
        row.addView(c2);

        parent.addView(row);
        return row;
    }

    LinearLayout stepper(LinearLayout parent, String[] steps, int activeIndex) {
        LinearLayout card = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.item_card, parent, false);
        ((TextView) card.findViewById(R.id.card_label)).setText("STATUS DO ATENDIMENTO");
        card.findViewById(R.id.card_value).setVisibility(View.GONE);
        card.findViewById(R.id.card_detail).setVisibility(View.GONE);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(6));
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        for (int i = 0; i < steps.length; i++) {
            boolean done = i <= activeIndex;
            TextView node = new TextView(activity);
            node.setText(done ? "✓" : String.valueOf(i + 1));
            node.setTextSize(11);
            node.setTypeface(null, Typeface.BOLD);
            node.setGravity(Gravity.CENTER);
            node.setTextColor(done ? Color.WHITE : muted);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(done ? green : cardColor);
            circle.setStroke(dp(1), done ? green : muted);
            circle.setSize(dp(22), dp(22));
            node.setBackground(circle);
            node.setWidth(dp(22));
            node.setHeight(dp(22));

            LinearLayout stepCol = new LinearLayout(activity);
            stepCol.setOrientation(LinearLayout.VERTICAL);
            stepCol.setGravity(Gravity.CENTER_HORIZONTAL);
            stepCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            stepCol.addView(node);

            TextView stepText = new TextView(activity);
            stepText.setText(steps[i]);
            stepText.setTextSize(10);
            stepText.setTextColor(done ? text : muted);
            stepText.setTypeface(null, done ? Typeface.BOLD : Typeface.NORMAL);
            stepText.setPadding(0, dp(4), 0, 0);
            stepCol.addView(stepText);

            row.addView(stepCol);
        }
        card.addView(row);
        parent.addView(card);
        return card;
    }

    LinearLayout ratingStars(LinearLayout parent, int initialStars, java.util.function.IntConsumer onSelect) {
        LinearLayout card = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.item_card, parent, false);
        ((TextView) card.findViewById(R.id.card_label)).setText("AVALIAÇÃO DO ATENDIMENTO");
        TextView val = card.findViewById(R.id.card_value);
        val.setText("Como foi o atendimento?");
        val.setTextColor(text);
        val.setTextSize(16);
        card.findViewById(R.id.card_detail).setVisibility(View.GONE);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(12), 0, dp(6));
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView[] stars = new TextView[5];
        final int[] current = {initialStars};

        for (int i = 0; i < 5; i++) {
            final int starIdx = i + 1;
            TextView star = new TextView(activity);
            star.setText("★");
            star.setTextSize(34);
            star.setTextColor(i < initialStars ? yellow : muted);
            star.setPadding(dp(8), dp(4), dp(8), dp(4));
            star.setOnClickListener(v -> {
                current[0] = starIdx;
                for (int j = 0; j < 5; j++) {
                    stars[j].setTextColor(j < starIdx ? yellow : muted);
                }
                if (onSelect != null) onSelect.accept(starIdx);
            });
            stars[i] = star;
            row.addView(star);
        }
        card.addView(row);
        parent.addView(card);
        return card;
    }

    void space(LinearLayout parent, int height) {
        View v = new View(activity);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        parent.addView(v);
    }

    ImageView image(LinearLayout parent, int resource, int height, String description) {
        ImageView v = new ImageView(activity);
        v.setImageResource(resource);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setContentDescription(description);
        v.setLayoutParams(params(height));
        parent.addView(v);
        return v;
    }

    void center(TextView view) {
        view.setGravity(Gravity.CENTER);
    }

    GradientDrawable shape(int color, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(16));
        shape.setStroke(dp(1), stroke);
        return shape;
    }
}
