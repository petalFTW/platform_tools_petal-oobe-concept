/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public final class ProviderSelectionActivity extends Activity {
    private static final int LOGIN = 51;
    private String mSelection = "WebDAV";
    private static final String[] PROVIDERS = {
            "WebDAV", "S3-compatible", "SFTP", "SMB", "Google Drive", "OneDrive"
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_provider_selection);
        BackupUi.apply(this, findViewById(R.id.provider_root));
        String selected = state == null ? getIntent().getStringExtra(
                CloudDestinationActivity.EXTRA_PROVIDER) : state.getString("provider");
        if (java.util.Arrays.asList(PROVIDERS).contains(selected)) mSelection = selected;
        RadioGroup group = findViewById(R.id.provider_choices);
        for (String provider : PROVIDERS) {
            RadioButton button = new RadioButton(this);
            button.setId(android.view.View.generateViewId());
            button.setText(provider);
            button.setTextSize(15);
            button.setMinHeight(Math.round(64 * getResources().getDisplayMetrics().density));
            button.setLayoutParams(new RadioGroup.LayoutParams(-1, -2));
            button.setTextColor(getColor(R.color.backup_text));
            button.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff0969f6));
            group.addView(button);
            if (provider.equals(mSelection)) group.check(button.getId());
            button.setOnCheckedChangeListener((view, checked) -> { if (checked) mSelection = provider; });
        }
        findViewById(R.id.provider_back).setOnClickListener(view -> finish());
        findViewById(R.id.provider_next).setOnClickListener(view -> startActivityForResult(
                new Intent(this, CloudDestinationActivity.class).putExtra(
                        CloudDestinationActivity.EXTRA_PROVIDER, mSelection), LOGIN));
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == LOGIN && result == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("provider", mSelection);
        super.onSaveInstanceState(state);
    }
}
