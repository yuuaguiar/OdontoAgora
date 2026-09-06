package br.edu.odontoagora;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Reusable native views; shared card and page structures come from XML layouts. */
final class Ui {
    final Activity activity;
    final int text, muted, brand, green, yellow, red, cardColor;
    Ui(Activity activity) {
        this.activity=activity;
        text=activity.getColor(R.color.text); muted=activity.getColor(R.color.muted);
        brand=activity.getColor(R.color.brand); green=activity.getColor(R.color.success);
        yellow=activity.getColor(R.color.warning); red=activity.getColor(R.color.danger); cardColor=activity.getColor(R.color.card);
    }
    int dp(float value) { return Math.round(value*activity.getResources().getDisplayMetrics().density); }
    LinearLayout.LayoutParams params(int height) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(height)); p.bottomMargin=dp(12); return p;
    }
    TextView text(LinearLayout parent,String value,int size,int color,boolean bold) {
        TextView v=new TextView(activity); v.setText(value); v.setTextColor(color); v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        v.setLineSpacing(dp(3),1); v.setLayoutParams(params(-2)); parent.addView(v); return v;
    }
    TextView title(LinearLayout parent,String value) { return text(parent,value,27,text,true); }
    TextView sub(LinearLayout parent,String value) { return text(parent,value,14,muted,false); }
    Button button(LinearLayout parent,String value,boolean primary,Runnable action) {
        Button v=new Button(activity); v.setText(value); v.setTextSize(14);v.setAllCaps(false); v.setTextColor(text);
        v.setTypeface(null,Typeface.BOLD); v.setBackgroundResource(primary?R.drawable.button_background:R.drawable.card_background);
        v.setMinHeight(dp(56)); v.setPadding(dp(12),dp(12),dp(12),dp(12)); v.setLayoutParams(params(-2));
        v.setOnClickListener(w->action.run()); parent.addView(v); return v;
    }
    EditText input(LinearLayout parent,String label,String value,int type,int id) {
        TextView caption=text(parent,label,12,muted,true);
        EditText v=new EditText(activity); v.setId(id); caption.setLabelFor(id);
        v.setText(value);v.setTextColor(text);v.setHintTextColor(muted);v.setInputType(type);v.setTextSize(16);
        v.setPadding(dp(14),dp(12),dp(14),dp(12));v.setMinHeight(dp(54));v.setBackgroundResource(R.drawable.card_background);
        v.setLayoutParams(params(-2)); parent.addView(v); return v;
    }
    LinearLayout card(LinearLayout parent,String label,String value,String detail,int color) {
        LinearLayout v=(LinearLayout)activity.getLayoutInflater().inflate(R.layout.item_card,parent,false);
        ((TextView)v.findViewById(R.id.card_label)).setText(label);
        TextView main=v.findViewById(R.id.card_value); main.setText(value); main.setTextColor(color);
        TextView extra=v.findViewById(R.id.card_detail);extra.setText(detail); if(detail.isEmpty())extra.setVisibility(View.GONE);
        parent.addView(v);return v;
    }
    void space(LinearLayout parent,int height) { View v=new View(activity);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(height)));parent.addView(v); }
    ImageView image(LinearLayout parent,int resource,int height,String description) {
        ImageView v=new ImageView(activity);v.setImageResource(resource);v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setContentDescription(description);v.setLayoutParams(params(height));parent.addView(v);return v;
    }
    void center(TextView view) { view.setGravity(Gravity.CENTER); }
    GradientDrawable shape(int color,int stroke) {
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(16));shape.setStroke(dp(1),stroke);return shape;
    }
}
