/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.setupwizard;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.setupdesign.GlifLayout;

// Keep the wizard mixins, ditch the old header furniture.
public final class PetalSetupLayout extends GlifLayout {
    public PetalSetupLayout(Context context) {
        super(context, R.layout.petal_setup_template, R.id.petal_content);
    }

    public PetalSetupLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PetalSetupLayout(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
    }

    @Override
    protected View onInflateTemplate(LayoutInflater inflater, int template) {
        return super.onInflateTemplate(inflater, R.layout.petal_setup_template);
    }

    @Override
    protected ViewGroup findContainer(int id) {
        return super.findContainer(R.id.petal_content);
    }

    // Template children only show up once inflation is done, so chrome waits.
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initPetalChrome();
    }

    private void initPetalChrome() {
        setBackgroundPatterned(false);
        View accessibility = findViewById(R.id.petal_accessibility);
        if (accessibility == null) {
            return;
        }
        accessibility.setOnClickListener(view -> {
            Context owner = getContext();
            while (owner instanceof ContextWrapper && !(owner instanceof Activity)) {
                owner = ((ContextWrapper) owner).getBaseContext();
            }
            if (owner instanceof Activity) {
                ((Activity) owner).startActivity(new Intent(
                        "android.settings.ACCESSIBILITY_SETTINGS_FOR_SUW"));
            }
        });
    }

    @Override
    public boolean shouldApplyPartnerResource() {
        return false;
    }

    public void setWelcomePage() {
        findViewById(R.id.petal_header).setVisibility(View.GONE);
        findViewById(R.id.petal_chrome).setVisibility(View.GONE);
        findViewById(R.id.petal_content).setPadding(0, 0, 0, 0);
    }
}
