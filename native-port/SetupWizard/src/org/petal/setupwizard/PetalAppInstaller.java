/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.setupwizard;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Application-owned so rotating the page doesn't strand the install queue.
public final class PetalAppInstaller {
    public static final class App {
        public final File apk;
        public final String packageName;
        public final String label;
        public App(File apk, String packageName, String label) {
            this.apk = apk; this.packageName = packageName; this.label = label;
        }
    }
    public interface Listener { void onInstallChanged(); }
    private static final String ACTION = "org.petal.setupwizard.APP_INSTALL_STATUS";
    private static PetalAppInstaller sInstance;
    private final Context mContext;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private final ExecutorService mWorker = Executors.newSingleThreadExecutor();
    private final CopyOnWriteArrayList<Listener> mListeners = new CopyOnWriteArrayList<>();
    private final ArrayDeque<App> mQueue = new ArrayDeque<>();
    private volatile int mGeneration;
    private volatile int mSession = -1;
    private boolean mRunning;
    private String mMessage = "";
    private App mCurrent;

    public static synchronized PetalAppInstaller get(Context context) {
        if (sInstance == null) sInstance = new PetalAppInstaller(context);
        return sInstance;
    }

    private PetalAppInstaller(Context context) {
        mContext = context.getApplicationContext();
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) { result(intent); }
        }, new IntentFilter(ACTION), Context.RECEIVER_NOT_EXPORTED);
    }

    public void observe(Listener listener) { mListeners.add(listener); }
    public void remove(Listener listener) { mListeners.remove(listener); }
    public boolean running() { return mRunning; }
    public String message() { return mMessage; }

    public static boolean installed(Context context, String packageName) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);
            return info.metaData == null
                    || !info.metaData.getBoolean("org.petalos.appcenter.PLACEHOLDER", false);
        } catch (PackageManager.NameNotFoundException exception) { return false; }
    }

    public void start(List<App> apps) {
        if (mRunning) return;
        mQueue.clear();
        mQueue.addAll(apps);
        mGeneration++;
        mRunning = true;
        installNext();
    }

    private void notifyChanged() {
        for (Listener listener : mListeners) listener.onInstallChanged();
    }

    public void cancel() {
        mGeneration++;
        mQueue.clear();
        mRunning = false;
        int session = mSession;
        mSession = -1;
        if (session >= 0) {
            try { mContext.getPackageManager().getPackageInstaller().abandonSession(session); }
            catch (RuntimeException ignored) { }
        }
        mMessage = "";
        notifyChanged();
    }

    private void installNext() {
        mSession = -1;
        mCurrent = mQueue.poll();
        if (mCurrent == null) {
            mRunning = false;
            mSession = -1;
            mMessage = "";
            notifyChanged();
            return;
        }
        App app = mCurrent;
        int generation = mGeneration;
        mMessage = mContext.getString(R.string.petal_apps_installing, app.label);
        notifyChanged();
        mWorker.execute(() -> {
            PackageInstaller installer = mContext.getPackageManager().getPackageInstaller();
            int id = -1;
            try {
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(app.packageName);
                params.setInstallerPackageName(mContext.getPackageName());
                id = installer.createSession(params);
                if (generation != mGeneration) { installer.abandonSession(id); return; }
                mSession = id;
                try (PackageInstaller.Session session = installer.openSession(id)) {
                    try (FileInputStream input = new FileInputStream(app.apk);
                            OutputStream output = session.openWrite("base.apk", 0, app.apk.length())) {
                        byte[] buffer = new byte[65536];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            if (generation != mGeneration) throw new IOException("Install cancelled");
                            output.write(buffer, 0, read);
                        }
                        session.fsync(output);
                    }
                    if (generation != mGeneration) throw new IOException("Install cancelled");
                    Intent callback = new Intent(ACTION).setPackage(mContext.getPackageName())
                            .putExtra("generation", generation);
                    PendingIntent status = PendingIntent.getBroadcast(mContext, id, callback,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                    session.commit(status.getIntentSender());
                }
            } catch (Exception exception) {
                if (id >= 0) {
                    try { installer.abandonSession(id); } catch (RuntimeException ignored) { }
                }
                mMain.post(() -> { if (generation == mGeneration) failed(app); });
            }
        });
    }

    private void failed(App app) {
        mRunning = false;
        mSession = -1;
        mQueue.clear();
        mMessage = mContext.getString(R.string.petal_apps_failed, app.label);
        notifyChanged();
    }

    private void result(Intent intent) {
        if (!mRunning || intent.getIntExtra("generation", -1) != mGeneration
                || intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1) != mSession) return;
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            if (confirmation == null) { failed(mCurrent); return; }
            confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { mContext.startActivity(confirmation); }
            catch (RuntimeException exception) { failed(mCurrent); }
        } else if (status == PackageInstaller.STATUS_SUCCESS) installNext();
        else failed(mCurrent);
    }
}
