/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;
import org.petal.backup.storage.CloudCredentialsStore;

public final class CloudDestinationActivity extends Activity {
    public static final String EXTRA_PROVIDER = "org.petal.backup.extra.PROVIDER";
    private final ExecutorService mWorker = Executors.newSingleThreadExecutor();
    private String mProvider;
    private CloudCredentialsStore mStore;
    private ViewGroup mForm;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        mProvider = getIntent().getStringExtra(EXTRA_PROVIDER);
        if (!Arrays.asList("WebDAV", "S3-compatible", "SFTP", "SMB", "Google Drive", "OneDrive")
                .contains(mProvider)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_cloud_destination);
        mForm = findViewById(R.id.cloud_form);
        mStore = new CloudCredentialsStore(this);
        BackupUi.apply(this, findViewById(R.id.cloud_root));
        ((TextView) findViewById(R.id.cloud_title)).setText(
                getString(R.string.cloud_configure, mProvider));
        findViewById(R.id.cloud_cancel).setOnClickListener(view -> finish());
        boolean webdav = "WebDAV".equals(mProvider);
        boolean s3 = "S3-compatible".equals(mProvider);
        boolean server = isServerProvider();
        findViewById(R.id.webdav_fields).setVisibility(webdav ? View.VISIBLE : View.GONE);
        findViewById(R.id.s3_fields).setVisibility(s3 ? View.VISIBLE : View.GONE);
        findViewById(R.id.server_fields).setVisibility(server ? View.VISIBLE : View.GONE);
        findViewById(R.id.smb_fields).setVisibility("SMB".equals(mProvider) ? View.VISIBLE : View.GONE);
        if (server) {
            ((TextView) findViewById(R.id.cloud_endpoint_label)).setText(R.string.server_host);
            field(R.id.cloud_endpoint).setHint(R.string.server_host_hint);
            if ("SMB".equals(mProvider)) {
                ((TextView) findViewById(R.id.server_folder_label))
                        .setText(R.string.server_folder_optional);
            }
        }
        if (!webdav && !s3 && !server) {
            findViewById(R.id.cloud_connection_fields).setVisibility(View.GONE);
            findViewById(R.id.cloud_save).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.cloud_info)).setText(R.string.cloud_oauth_pending);
            return;
        }
        findViewById(R.id.cloud_save).setOnClickListener(view -> saveConnection());
        setFormEnabled(false);
        mWorker.execute(() -> {
            try {
                JSONObject config = mStore.load(mProvider);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    populate(config);
                    setFormEnabled(true);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    setFormEnabled(true);
                    Toast.makeText(this, R.string.cloud_load_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void populate(JSONObject config) {
        field(R.id.cloud_endpoint).setText(config.optString(isServerProvider() ? "host" : "endpoint", ""));
        field(R.id.cloud_folder).setText(config.optString("folder", "/PetalBackup"));
        field(R.id.cloud_username).setText(config.optString("username", ""));
        field(R.id.cloud_password).setText(config.optString("password", ""));
        field(R.id.cloud_region).setText(config.optString("region", "us-east-1"));
        field(R.id.cloud_bucket).setText(config.optString("bucket", ""));
        field(R.id.cloud_access_key).setText(config.optString("accessKey", ""));
        field(R.id.cloud_secret_key).setText(config.optString("secretKey", ""));
        field(R.id.server_port).setText(config.optString("port", "SMB".equals(mProvider) ? "445" : "22"));
        field(R.id.server_share).setText(config.optString("share", ""));
        field(R.id.server_domain).setText(config.optString("domain", ""));
        field(R.id.server_folder).setText(config.optString("folder", "SMB".equals(mProvider) ? "" : "/PetalBackup"));
        field(R.id.server_username).setText(config.optString("username", ""));
        field(R.id.server_password).setText(config.optString("password", ""));
    }

    private boolean isServerProvider() {
        return "SFTP".equals(mProvider) || "SMB".equals(mProvider);
    }

    private EditText field(int id) {
        return findViewById(id);
    }

    private String text(int id) {
        return field(id).getText().toString().trim();
    }

    private void saveConnection() {
        String endpoint = text(R.id.cloud_endpoint);
        int port = 0;
        if (isServerProvider()) {
            try {
                port = Integer.parseInt(text(R.id.server_port));
                if (port < 1 || port > 65535) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                field(R.id.server_port).setError(getString(R.string.server_port_invalid));
                return;
            }
        }
        try {
            URI uri = new URI(isServerProvider() ? "ssh://" + endpoint + ":" + port : endpoint);
            boolean valid = isServerProvider()
                    ? uri.getHost() != null && uri.getRawUserInfo() == null
                            && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                            && uri.getRawQuery() == null && uri.getRawFragment() == null
                    : ("https".equalsIgnoreCase(uri.getScheme())
                            || "http".equalsIgnoreCase(uri.getScheme()))
                            && uri.getHost() != null && uri.getRawUserInfo() == null;
            if (!valid) {
                field(R.id.cloud_endpoint).setError(getString(isServerProvider()
                        ? R.string.server_host_invalid : R.string.cloud_url_invalid));
                return;
            }
        } catch (URISyntaxException exception) {
            field(R.id.cloud_endpoint).setError(getString(isServerProvider()
                    ? R.string.server_host_invalid : R.string.cloud_url_invalid));
            return;
        }
        JSONObject config = new JSONObject();
        try {
            if (isServerProvider()) {
                config.put("host", endpoint);
                config.put("port", port);
                config.put("folder", text(R.id.server_folder));
                config.put("username", text(R.id.server_username));
                config.put("password", field(R.id.server_password).getText().toString());
                if ("SMB".equals(mProvider)) {
                    config.put("share", text(R.id.server_share));
                    config.put("domain", text(R.id.server_domain));
                }
                if (text(R.id.server_username).isEmpty() || config.getString("password").isEmpty()
                        || ("SMB".equals(mProvider) && text(R.id.server_share).isEmpty())
                        || ("SFTP".equals(mProvider) && text(R.id.server_folder).isEmpty())) {
                    Toast.makeText(this, R.string.cloud_fields_required, Toast.LENGTH_LONG).show();
                    return;
                }
            } else if ("WebDAV".equals(mProvider)) {
                config.put("endpoint", endpoint);
                config.put("folder", text(R.id.cloud_folder));
                config.put("username", text(R.id.cloud_username));
                config.put("password", field(R.id.cloud_password).getText().toString());
                if (text(R.id.cloud_folder).isEmpty() || text(R.id.cloud_username).isEmpty()
                        || config.getString("password").isEmpty()) {
                    Toast.makeText(this, R.string.cloud_fields_required, Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                config.put("endpoint", endpoint);
                config.put("region", text(R.id.cloud_region));
                config.put("bucket", text(R.id.cloud_bucket));
                config.put("accessKey", text(R.id.cloud_access_key));
                config.put("secretKey", field(R.id.cloud_secret_key).getText().toString());
                if (text(R.id.cloud_region).isEmpty() || text(R.id.cloud_bucket).isEmpty()
                        || text(R.id.cloud_access_key).isEmpty()
                        || config.getString("secretKey").isEmpty()) {
                    Toast.makeText(this, R.string.cloud_fields_required, Toast.LENGTH_LONG).show();
                    return;
                }
            }
        } catch (JSONException exception) {
            Toast.makeText(this, R.string.cloud_save_failed, Toast.LENGTH_LONG).show();
            return;
        }
        // Let the worker deal with Keystore; don't freeze this bloody form.
        setFormEnabled(false);
        mWorker.execute(() -> {
            try {
                mStore.save(mProvider, config);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_PROVIDER, mProvider));
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    setFormEnabled(true);
                    Toast.makeText(this, R.string.cloud_save_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setFormEnabled(boolean enabled) {
        setControlsEnabled(mForm, enabled);
        findViewById(R.id.cloud_save).setEnabled(enabled);
        findViewById(R.id.cloud_save).setAlpha(enabled ? 1f : 0.4f);
    }

    private static void setControlsEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setControlsEnabled(group.getChildAt(i), enabled);
            }
        }
    }

    @Override
    protected void onDestroy() {
        mWorker.shutdown();
        super.onDestroy();
    }
}
