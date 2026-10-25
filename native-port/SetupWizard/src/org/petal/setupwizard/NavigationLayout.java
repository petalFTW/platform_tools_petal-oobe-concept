/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.petal.setupwizard;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

public class NavigationLayout extends RelativeLayout {
    // The same controls on every page, finally.
    public interface NavigationBarListener {
        void onNavigateNext();

        void onSkip();
        void onNavigateBack();
    }

    private final Button mNextButton;
    private final Button mSkipButton;
    private final Button mBackButton;

    public NavigationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        View.inflate(context, R.layout.navigation_layout, this);
        mNextButton = findViewById(R.id.navbar_next);
        mSkipButton = findViewById(R.id.navbar_skip);
        mBackButton = findViewById(R.id.navbar_back);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.NavigationLayout, 0, 0);
        final boolean showSkipButton;
        try {
            showSkipButton = a.getBoolean(
                    R.styleable.NavigationLayout_showSkipButton, false);
        } finally {
            a.recycle();
        }

        if (showSkipButton) {
            mSkipButton.setVisibility(View.VISIBLE);
        }
    }

    public Button getSkipButton() {
        return mSkipButton;
    }

    public Button getNextButton() {
        return mNextButton;
    }

    public Button getBackButton() {
        return mBackButton;
    }

    public void setNavigationBarListener(NavigationBarListener listener) {
        mSkipButton.setOnClickListener(view -> listener.onSkip());
        mNextButton.setOnClickListener(view -> listener.onNavigateNext());
        mBackButton.setOnClickListener(view -> listener.onNavigateBack());
    }
}
