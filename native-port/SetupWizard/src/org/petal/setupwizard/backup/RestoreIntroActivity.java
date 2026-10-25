/*
 * SPDX-FileCopyrightText: 2019-2020 The Calyx Institute
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.petal.setupwizard.backup;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_ACTIVITY_NOT_FOUND;

import static org.petal.setupwizard.SetupWizardApp.ACTION_RESTORE_FROM_BACKUP;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.RadioGroup;
import org.petal.setupwizard.PetalViews;

import androidx.activity.result.ActivityResult;

import org.petal.setupwizard.R;
import org.petal.setupwizard.SubBaseActivity;

public class RestoreIntroActivity extends SubBaseActivity {
    private static final String PETAL_BACKUP_PACKAGE = "org.petal.backup";
    private static final String PETAL_BACKUP_ACTION = "org.petal.backup.action.RESTORE";
    private static final String PETAL_BACKUP_READY = "org.petal.backup.SETUP_UI_READY";
    private boolean mRestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGlifLayout().setDescriptionText(getString(R.string.petal_restore_description));
        mRestore = savedInstanceState != null && savedInstanceState.getBoolean("restore");
        RadioGroup choices = new RadioGroup(this);
        choices.addView(PetalViews.radio(this, 2201, R.string.petal_restore_choice));
        choices.addView(PetalViews.radio(this, 2202, R.string.petal_restore_fresh));
        choices.check(mRestore ? 2201 : 2202);
        choices.setOnCheckedChangeListener((group, id) -> {
            mRestore = id == 2201;
            setNextText(mRestore ? R.string.petal_restore_open : R.string.next);
        });
        ((android.widget.FrameLayout) findViewById(R.id.page)).addView(choices);
        getNavigationBar().getSkipButton().setVisibility(android.view.View.GONE);
        setNextText(mRestore ? R.string.petal_restore_open : R.string.next);
    }

    @Override
    protected void onSubactivityResult(ActivityResult activityResult) {
        int resultCode = activityResult.getResultCode();
        Intent data = activityResult.getData();
        if (resultCode != RESULT_CANCELED) {
            nextAction(resultCode, data);
        } else if (mIsSubactivityNotFound) {
            finishAction(RESULT_ACTIVITY_NOT_FOUND);
        } else if (data != null && data.getBooleanExtra("onBackPressed", false)) {
            // Back inside Petal Backup parks us on the restore choice again.
            setNextText(mRestore ? R.string.petal_restore_open : R.string.next);
            setNextAllowed(true);
        }
    }

    @Override
    protected void onStartSubactivity() {
        setNextAllowed(true);
    }

    @Override
    protected void onNextPressed() {
        if (mRestore) launchRestore();
        else nextAction(RESULT_OK);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("restore", mRestore);
        super.onSaveInstanceState(state);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.intro_restore_activity;
    }

    @Override
    protected int getTitleResId() {
        return R.string.intro_restore_title;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_restore;
    }

    private void launchRestore() {
        Intent petalBackup = new Intent(PETAL_BACKUP_ACTION)
                .setPackage(PETAL_BACKUP_PACKAGE)
                .putExtra("org.petal.backup.extra.FROM_SETUP", true);
        try {
            ApplicationInfo app = getPackageManager().getApplicationInfo(
                    PETAL_BACKUP_PACKAGE, PackageManager.GET_META_DATA);
            if (app.enabled && app.metaData != null
                    && app.metaData.getBoolean(PETAL_BACKUP_READY, false)
                    && petalBackup.resolveActivity(getPackageManager()) != null) {
                startSubactivity(petalBackup);
                return;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // The working restore provider still gets a turn.
        }
        Intent intent = new Intent(ACTION_RESTORE_FROM_BACKUP);
        startSubactivity(intent);
    }

}
