/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.window.OnBackInvokedDispatcher;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int PICK_FOLDER = 41;
    private static final int CONFIGURE_CLOUD = 42;
    private static final String PREF_FOLDER = "local_folder";
    private static final String PREF_CLOUD = "cloud_provider";
    private static final String[] CLOUD_PROVIDERS = {
            "WebDAV", "S3-compatible", "SFTP", "SMB", "Google Drive", "OneDrive"
    };
    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        mPreferences = getSharedPreferences("destinations", MODE_PRIVATE);
        setContentView(R.layout.activity_backup);
        View root = findViewById(R.id.backup_root);
        BackupUi.apply(this, root);
        if (isRestoreEntry()) {
            ((TextView) findViewById(R.id.title)).setText(R.string.restore_title);
            findViewById(R.id.backup_action).setVisibility(View.GONE);
        }
        findViewById(R.id.local_folder).setOnClickListener(view -> chooseFolder());
        findViewById(R.id.cloud_provider).setOnClickListener(view -> chooseCloudProvider());
        findViewById(R.id.backup_back).setOnClickListener(view -> finishBack());
        if (isSetupEntry()) {
            View skip = findViewById(R.id.skip_restore);
            skip.setVisibility(View.VISIBLE);
            skip.setOnClickListener(view -> {
                setResult(BackupContract.RESULT_SKIPPED, new Intent()
                        .putExtra(BackupContract.EXTRA_RESTORED, false)
                        .putExtra(BackupContract.EXTRA_SKIPPED, true));
                finish();
            });
        }
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::finishBack);
        updateDestinations();
    }

    protected boolean isRestoreEntry() {
        return false;
    }

    private boolean isSetupEntry() {
        return isRestoreEntry() && getIntent().getBooleanExtra(
                BackupContract.EXTRA_FROM_SETUP, false);
    }

    private void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_FOLDER);
    }

    private void chooseCloudProvider() {
        startActivityForResult(new Intent(this, ProviderSelectionActivity.class)
                .putExtra(CloudDestinationActivity.EXTRA_PROVIDER,
                        mPreferences.getString(PREF_CLOUD, "WebDAV")), CONFIGURE_CLOUD);
    }

    private void finishBack() {
        if (isSetupEntry()) setResult(RESULT_CANCELED,
                new Intent().putExtra("onBackPressed", true));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIGURE_CLOUD) {
            if (resultCode == RESULT_OK && data != null) {
                String provider = data.getStringExtra(CloudDestinationActivity.EXTRA_PROVIDER);
                if (java.util.Arrays.asList(CLOUD_PROVIDERS).contains(provider)) {
                    mPreferences.edit().putString(PREF_CLOUD, provider).apply();
                    updateDestinations();
                }
            }
            return;
        }
        if (requestCode != PICK_FOLDER || resultCode != RESULT_OK || data == null) return;
        Uri tree = data.getData();
        if (tree == null || !DocumentsContract.isTreeUri(tree)) return;
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(tree, flags);
            mPreferences.edit().putString(PREF_FOLDER, tree.toString()).apply();
            updateDestinations();
        } catch (SecurityException exception) {
            Toast.makeText(this, R.string.folder_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void updateDestinations() {
        String tree = mPreferences.getString(PREF_FOLDER, null);
        String cloud = mPreferences.getString(PREF_CLOUD, null);
        ((TextView) findViewById(R.id.local_status)).setText(tree == null
                ? getString(R.string.local_unset)
                : getString(R.string.local_selected,
                        DocumentsContract.getTreeDocumentId(Uri.parse(tree))));
        ((TextView) findViewById(R.id.cloud_status)).setText(cloud == null
                ? getString(R.string.cloud_unset)
                : getString(R.string.cloud_selected, cloud));
    }
}
