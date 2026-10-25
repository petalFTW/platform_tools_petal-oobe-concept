/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.petal.setupwizard;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP;

import android.annotation.Nullable;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.euicc.EuiccManager;
import android.widget.Button;
import android.widget.FrameLayout;

import org.petal.setupwizard.util.SetupWizardUtils;

public class SimMissingActivity extends BaseSetupWizardActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SetupWizardUtils.simMissing(this) || !SetupWizardUtils.hasTelephony(this)) {
            // Skipped pages still need to let the back result travel through.
            finishAction(RESULT_SKIP, new Intent().putExtra("onBackPressed", true));
            return;
        }
        getGlifLayout().setDescriptionText(getString(R.string.sim_missing_summary));
        EuiccManager euicc = getSystemService(EuiccManager.class);
        Intent provision = new Intent(EuiccManager.ACTION_PROVISION_EMBEDDED_SUBSCRIPTION);
        if (euicc != null && euicc.isEnabled()
                && provision.resolveActivity(getPackageManager()) != null) {
            Button button = new Button(this, null, 0, R.style.PetalSecondaryButton);
            button.setText(R.string.petal_esim);
            button.setOnClickListener(view -> startActivity(provision));
            FrameLayout body = findViewById(R.id.page);
            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(-1, PetalViews.dp(this, 50));
            params.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
            params.topMargin = PetalViews.dp(this, 24);
            body.addView(button, params);
        }
        setNextAllowed(true);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.sim_missing_page;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_sim_missing;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_sim;
    }

}
