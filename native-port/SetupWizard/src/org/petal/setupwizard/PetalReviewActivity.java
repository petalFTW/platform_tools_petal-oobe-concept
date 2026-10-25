/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.setupwizard;

import android.app.KeyguardManager;
import android.app.UiModeManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.Locale;
import org.petal.setupwizard.backup.RestoreIntroActivity;

public final class PetalReviewActivity extends BaseSetupWizardActivity {
    private ActivityResultLauncher<Intent> mEditor;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getGlifLayout().setDescriptionText(getString(R.string.petal_review_description));
        getNavigationBar().getSkipButton().setVisibility(View.GONE);
        setNextText(R.string.petal_review_finish);
        mEditor = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> showChoices());
    }

    @Override protected void onResume() { super.onResume(); showChoices(); }

    private void row(int title, String detail, Class<?> activity) {
        LinearLayout options = findViewById(R.id.petal_options);
        TextView row = PetalViews.text(this, getString(title) + "\n" + detail + "  ›");
        row.setMinHeight(PetalViews.dp(this, 72));
        row.setTextColor(getColor(R.color.petal_setup_text));
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setOnClickListener(view -> mEditor.launch(new Intent(this, activity)
                .putExtra(EXTRA_PETAL_REVIEW_EDIT, true)));
        options.addView(row);
        PetalViews.divider(options);
    }

    private void showChoices() {
        if (isDestroyed()) return;
        ((LinearLayout) findViewById(R.id.petal_options)).removeAllViews();
        row(R.string.petal_review_language, Locale.getDefault().getDisplayName(), LocaleActivity.class);
        row(R.string.petal_network_title, getString(R.string.petal_network_more), NetworkSetupActivity.class);
        row(R.string.intro_restore_title, getString(R.string.petal_restore_choice), RestoreIntroActivity.class);
        row(R.string.petal_review_security, getString(getSystemService(KeyguardManager.class).isDeviceSecure()
                ? R.string.petal_review_security_set : R.string.petal_review_security_none), ScreenLockActivity.class);
        int mode = getSystemService(UiModeManager.class).getNightMode();
        row(R.string.setup_theme, getString(mode == UiModeManager.MODE_NIGHT_YES ? R.string.dark
                : mode == UiModeManager.MODE_NIGHT_NO ? R.string.light : R.string.petal_automatic), ThemeSettingsActivity.class);
        row(R.string.setup_navigation, SetupWizardApp.getSettingsBundle().getString(
                SetupWizardApp.NAVIGATION_OPTION_KEY, "").contains("gestural")
                ? getString(R.string.gesture_navigation) : getString(R.string.navbar_navigation), NavigationSettingsActivity.class);
        row(R.string.petal_review_diagnostics, getString(SetupWizardApp.getSettingsBundle().getBoolean(
                SetupWizardApp.KEY_SEND_METRICS, false) ? R.string.petal_review_on : R.string.petal_review_off), LineageSettingsActivity.class);
        row(R.string.petal_apps_title, getString(R.string.petal_apps_description), PetalAppsActivity.class);
    }

    @Override protected int getLayoutResId() { return R.layout.petal_choice_page; }
    @Override protected int getTitleResId() { return R.string.petal_review_title; }
}
