/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.petal.setupwizard;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP;

import android.content.Intent;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.provider.Settings;

import org.petal.setupwizard.util.SetupWizardUtils;

public class BiometricActivity extends SubBaseActivity {

    @Override
    protected void onStartSubactivity() {
        if (!SetupWizardUtils.hasBiometric(this)
                || !getSystemService(KeyguardManager.class).isDeviceSecure()) {
            finishAction(RESULT_SKIP);
            return;
        }
        setNextAllowed(true);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        getGlifLayout().setDescriptionText(getString(R.string.petal_biometric_description));
        setNextText(R.string.petal_biometric_add);
    }

    @Override
    protected void onNextPressed() {
        Intent intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
        startSubactivity(intent);
    }

    @Override
    protected void onSubactivityResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() != RESULT_CANCELED) {
            nextAction(result.getResultCode(), result.getData());
        }
    }

    @Override
    protected int getLayoutResId() { return R.layout.petal_choice_page; }

    @Override
    protected int getTitleResId() { return R.string.petal_biometric_title; }
}
