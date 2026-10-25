/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.setupwizard;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

public final class PetalViews {
    private PetalViews() {}

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static RadioButton radio(Context context, int id, int title) {
        RadioButton button = new RadioButton(context);
        button.setId(id);
        button.setText(title);
        button.setTextSize(15);
        button.setTextColor(context.getColor(R.color.petal_setup_text));
        button.setButtonTintList(android.content.res.ColorStateList.valueOf(
                context.getColor(R.color.petal_setup_accent)));
        button.setPadding(dp(context, 12), dp(context, 16), dp(context, 12), dp(context, 16));
        button.setMinHeight(dp(context, 64));
        button.setLayoutParams(new android.widget.RadioGroup.LayoutParams(-1, -2));
        return button;
    }

    public static TextView text(Context context, CharSequence text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(context.getColor(R.color.petal_setup_secondary));
        view.setPadding(0, dp(context, 16), 0, dp(context, 16));
        return view;
    }

    public static void divider(LinearLayout parent) {
        View line = new View(parent.getContext());
        line.setBackgroundColor(parent.getContext().getColor(R.color.petal_setup_line));
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(parent.getContext(), 1)));
    }
}
