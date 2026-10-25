/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.setupwizard;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PetalAppsActivity extends BaseSetupWizardActivity
        implements PetalAppInstaller.Listener {
    private final ExecutorService mScanner = Executors.newSingleThreadExecutor();
    private final ArrayList<PetalAppInstaller.App> mApps = new ArrayList<>();
    private final Set<String> mSelected = new HashSet<>();
    private PetalAppInstaller mInstaller;
    private boolean mLoaded;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getGlifLayout().setDescriptionText(getString(R.string.petal_apps_description));
        mInstaller = PetalAppInstaller.get(this);
        if (state != null) {
            List<String> selected = state.getStringArrayList("selected");
            if (selected != null) mSelected.addAll(selected);
        }
        setNextAllowed(false);
        mScanner.execute(() -> {
            LinkedHashMap<String, PetalAppInstaller.App> apps = new LinkedHashMap<>();
            for (String directory : new String[] {"/system_ext/etc/petal/appcenter", "/product/etc/petal/appcenter"}) {
                File[] files = new File(directory).listFiles();
                if (files == null) continue;
                java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
                for (File file : files) {
                    if (!file.getName().endsWith(".apk")) continue;
                    PackageInfo info = getPackageManager().getPackageArchiveInfo(file.getPath(), 0);
                    if (info == null || info.applicationInfo == null) continue;
                    ApplicationInfo application = info.applicationInfo;
                    application.sourceDir = file.getPath();
                    application.publicSourceDir = file.getPath();
                    apps.putIfAbsent(info.packageName, new PetalAppInstaller.App(file,
                            info.packageName, application.loadLabel(getPackageManager()).toString()));
                }
            }
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                mApps.addAll(apps.values());
                mLoaded = true;
                onInstallChanged();
            });
        });
    }

    @Override protected void onStart() {
        super.onStart();
        mInstaller.observe(this);
        onInstallChanged();
    }

    @Override protected void onStop() { mInstaller.remove(this); super.onStop(); }

    @Override protected void onDestroy() { mScanner.shutdown(); super.onDestroy(); }

    @Override
    public void onInstallChanged() {
        LinearLayout options = findViewById(R.id.petal_options);
        options.removeAllViews();
        if (!mInstaller.message().isEmpty()) options.addView(PetalViews.text(this, mInstaller.message()));
        if (mLoaded && mApps.isEmpty()) options.addView(PetalViews.text(this, getString(R.string.petal_apps_empty)));
        boolean uninstalledSelection = false;
        for (PetalAppInstaller.App app : mApps) {
            boolean installed = PetalAppInstaller.installed(this, app.packageName);
            CheckBox choice = new CheckBox(this);
            choice.setText(app.label + (installed ? " · " + getString(R.string.petal_apps_installed) : ""));
            choice.setTextSize(15);
            choice.setTextColor(getColor(R.color.petal_setup_text));
            choice.setMinHeight(PetalViews.dp(this, 72));
            choice.setChecked(installed || mSelected.contains(app.packageName));
            choice.setEnabled(!installed && !mInstaller.running());
            choice.setOnCheckedChangeListener((button, checked) -> {
                if (checked) mSelected.add(app.packageName); else mSelected.remove(app.packageName);
                setNextText(mSelected.isEmpty() ? R.string.next : R.string.petal_apps_install);
            });
            options.addView(choice);
            PetalViews.divider(options);
            uninstalledSelection |= !installed && mSelected.contains(app.packageName);
        }
        setNextAllowed(mLoaded && !mInstaller.running());
        setNextText(uninstalledSelection ? R.string.petal_apps_install : R.string.next);
    }

    @Override
    protected void onNextPressed() {
        ArrayList<PetalAppInstaller.App> selected = new ArrayList<>();
        for (PetalAppInstaller.App app : mApps) {
            if (mSelected.contains(app.packageName)
                    && !PetalAppInstaller.installed(this, app.packageName)) selected.add(app);
        }
        if (selected.isEmpty()) super.onNextPressed(); else mInstaller.start(selected);
    }

    @Override protected void onSkipPressed() { mInstaller.cancel(); super.onSkipPressed(); }
    @Override public void onNavigateBack() { mInstaller.cancel(); super.onNavigateBack(); }
    @Override protected int getLayoutResId() { return R.layout.petal_choice_page; }
    @Override protected int getTitleResId() { return R.string.petal_apps_title; }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putStringArrayList("selected", new ArrayList<>(mSelected));
        super.onSaveInstanceState(state);
    }
}
