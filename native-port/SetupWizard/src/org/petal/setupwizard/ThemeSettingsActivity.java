/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.petal.setupwizard;

import android.app.UiModeManager;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;

public class ThemeSettingsActivity extends BaseSetupWizardActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGlifLayout().setDescriptionText(getString(R.string.theme_summary));

        UiModeManager uiModeManager = getSystemService(UiModeManager.class);
        final RadioGroup radioGroup = findViewById(R.id.theme_radio_group);
        int mode = uiModeManager.getNightMode();
        radioGroup.check(mode == UiModeManager.MODE_NIGHT_YES ? R.id.radio_dark
                : mode == UiModeManager.MODE_NIGHT_NO ? R.id.radio_light : R.id.radio_automatic);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_dark) {
                uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
            } else if (checkedId == R.id.radio_light) {
                uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
            } else if (checkedId == R.id.radio_automatic) {
                uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_AUTO);
            }
        });
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_theme;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_theme;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_theme;
    }
}
