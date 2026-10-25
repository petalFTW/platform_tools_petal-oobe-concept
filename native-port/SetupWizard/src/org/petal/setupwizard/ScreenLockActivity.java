/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.petal.setupwizard;

import android.content.Intent;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import androidx.activity.result.ActivityResult;
import com.android.internal.widget.LockPatternUtils;

public class ScreenLockActivity extends SubBaseActivity {

    private static final String ACTION_SETUP_LOCKSCREEN = "com.android.settings.SETUP_LOCK_SCREEN";
    private static final int PIN = 2101;
    private static final int PATTERN = 2102;
    private int mQuality = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
    private boolean mChanged;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getGlifLayout().setDescriptionText(getString(R.string.petal_lock_description));
        if (state != null) {
            mQuality = state.getInt("quality", mQuality);
            mChanged = state.getBoolean("changed");
        } else if (getSystemService(KeyguardManager.class).isDeviceSecure()) {
            int quality = new LockPatternUtils(this).getKeyguardStoredPasswordQuality(
                    UserHandle.myUserId());
            mQuality = quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
                    ? quality : DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
        }
        LinearLayout options = findViewById(R.id.petal_options);
        RadioGroup group = new RadioGroup(this);
        group.addView(PetalViews.radio(this, PIN, R.string.petal_lock_pin));
        group.addView(PetalViews.radio(this, PATTERN, R.string.petal_lock_pattern));
        group.check(mQuality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING ? PATTERN : PIN);
        group.setOnCheckedChangeListener((view, id) -> {
            mChanged = true;
            mQuality = id == PATTERN ? DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
                    : DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
        });
        options.addView(group);
        if (getSystemService(KeyguardManager.class).isDeviceSecure()) {
            options.addView(PetalViews.text(this, getString(R.string.petal_lock_existing)));
        }
    }

    @Override
    protected void onStartSubactivity() {
        setNextAllowed(true);
    }

    @Override
    protected void onNextPressed() {
        if (!mChanged && getSystemService(KeyguardManager.class).isDeviceSecure()) {
            nextAction(RESULT_OK);
            return;
        }
        startSubactivity(new Intent(ACTION_SETUP_LOCKSCREEN)
                .putExtra(LockPatternUtils.PASSWORD_TYPE_KEY, mQuality));
    }

    @Override
    protected void onSubactivityResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_CANCELED
                && getSystemService(KeyguardManager.class).isDeviceSecure()) {
            nextAction(RESULT_OK, result.getData());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt("quality", mQuality);
        state.putBoolean("changed", mChanged);
        super.onSaveInstanceState(state);
    }

    @Override
    protected int getLayoutResId() { return R.layout.petal_choice_page; }

    @Override
    protected int getTitleResId() { return R.string.petal_lock_title; }
}
