/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.setupwizard;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkSetupActivity extends SubBaseActivity {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ArrayList<ScanResult> mNetworks = new ArrayList<>();
    private WifiManager mWifi;
    private ArrayAdapter<ScanResult> mAdapter;
    private boolean mRegistered;
    private boolean mConnecting;
    private final Runnable mConnectionTimeout = () -> {
        mConnecting = false;
        if (!isFinishing() && !isDestroyed()) {
            updateNetworks();
            if (!connected()) status(R.string.petal_network_failed);
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) { updateNetworks(); }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        mWifi = getSystemService(WifiManager.class);
        getGlifLayout().setDescriptionText(getString(R.string.petal_network_description));
        setSkipText(R.string.petal_network_offline);
        ListView list = findViewById(R.id.network_list);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mNetworks) {
            @Override
            public View getView(int position, View recycled, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, recycled, parent);
                view.setText(getItem(position).SSID);
                view.setTextSize(15);
                view.setTextColor(getColor(R.color.petal_setup_text));
                view.setMinHeight(PetalViews.dp(NetworkSetupActivity.this, 64));
                return view;
            }
        };
        list.setAdapter(mAdapter);
        list.setOnItemClickListener((parent, view, position, id) -> choose(mNetworks.get(position)));
        findViewById(R.id.network_more).setOnClickListener(view -> openNetworkSettings());
        if (mWifi != null) tryEnablingWifi();
    }

    @Override
    protected void onStartSubactivity() { setNextAllowed(false); }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        mRegistered = true;
        updateNetworks();
        if (mWifi != null) {
            try { mWifi.startScan(); }
            catch (SecurityException exception) { status(R.string.petal_network_failed); }
        }
        if (mConnecting) mHandler.postDelayed(mConnectionTimeout, 20000);
    }

    @Override
    protected void onStop() {
        if (mRegistered) { unregisterReceiver(mReceiver); mRegistered = false; }
        mHandler.removeCallbacks(mConnectionTimeout);
        super.onStop();
    }

    private boolean connected() {
        ConnectivityManager connectivity = getSystemService(ConnectivityManager.class);
        return connectivity != null && connectivity.getActiveNetwork() != null;
    }

    private void status(int text) { ((TextView) findViewById(R.id.network_status)).setText(text); }

    private void updateNetworks() {
        if (mAdapter == null || isFinishing()) return;
        boolean connected = connected();
        setNextAllowed(connected);
        if (connected) {
            mConnecting = false;
            mHandler.removeCallbacks(mConnectionTimeout);
            status(R.string.petal_network_connected);
        } else status(mConnecting ? R.string.petal_network_connecting : R.string.petal_network_scanning);
        mNetworks.clear();
        if (mWifi != null) {
            try {
                List<ScanResult> scans = new ArrayList<>(mWifi.getScanResults());
                scans.sort(Comparator.comparingInt((ScanResult scan) -> scan.level).reversed());
                Set<String> seen = new HashSet<>();
                for (ScanResult scan : scans) {
                    if (!scan.SSID.isEmpty() && seen.add(scan.SSID
                            + java.util.Arrays.toString(scan.getSecurityTypes()))) {
                        mNetworks.add(scan);
                    }
                }
            } catch (SecurityException ignored) {
                // The system network page can still handle restricted scans.
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void choose(ScanResult scan) {
        String capabilities = scan.capabilities == null ? "" : scan.capabilities;
        if (capabilities.contains("EAP") || capabilities.contains("WEP")) {
            openNetworkSettings();
            return;
        }
        if (capabilities.contains("PSK") || capabilities.contains("SAE")) {
            EditText password = new EditText(this);
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            password.setHint(R.string.petal_network_password);
            password.setSaveEnabled(false);
            int padding = PetalViews.dp(this, 20);
            password.setPadding(padding, padding, padding, padding);
            new AlertDialog.Builder(this).setTitle(scan.SSID).setView(password)
                    .setPositiveButton(R.string.petal_network_connect,
                            (dialog, which) -> connect(scan, password.getText().toString()))
                    .setNegativeButton(android.R.string.cancel, null).show();
        } else connect(scan, null);
    }

    private static String quoted(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void connect(ScanResult scan, String password) {
        if (mWifi == null) return;
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = quoted(scan.SSID);
        String capabilities = scan.capabilities == null ? "" : scan.capabilities;
        if (capabilities.contains("SAE") && !capabilities.contains("PSK")) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.SAE);
            config.requirePmf = true;
            config.preSharedKey = quoted(password == null ? "" : password);
        } else if (password != null) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.preSharedKey = password.matches("[0-9A-Fa-f]{64}") ? password : quoted(password);
        } else if (capabilities.contains("OWE")) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.OWE);
            config.requirePmf = true;
        } else config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        mConnecting = true;
        status(R.string.petal_network_connecting);
        mHandler.removeCallbacks(mConnectionTimeout);
        mHandler.postDelayed(mConnectionTimeout, 20000);
        try {
            mWifi.connect(config, new WifiManager.ActionListener() {
                @Override public void onSuccess() { updateNetworks(); }
                @Override public void onFailure(int reason) {
                    mConnecting = false;
                    mHandler.removeCallbacks(mConnectionTimeout);
                    status(R.string.petal_network_failed);
                }
            });
        } catch (SecurityException | IllegalArgumentException exception) {
            mConnecting = false;
            mHandler.removeCallbacks(mConnectionTimeout);
            status(R.string.petal_network_failed);
        }
    }

    private void openNetworkSettings() {
        startSubactivity(new Intent("android.settings.NETWORK_PROVIDER_SETUP")
                .putExtra(SetupWizardApp.EXTRA_PREFS_SHOW_BUTTON_BAR, true)
                .putExtra(SetupWizardApp.EXTRA_PREFS_SHOW_SKIP, true)
                .putExtra(SetupWizardApp.EXTRA_ENABLE_NEXT_ON_CONNECT, true));
    }

    @Override
    protected void onSubactivityResult(ActivityResult result) { updateNetworks(); }

    @Override
    protected int getLayoutResId() { return R.layout.petal_network_page; }

    @Override
    protected int getTitleResId() { return R.string.petal_network_title; }
}
