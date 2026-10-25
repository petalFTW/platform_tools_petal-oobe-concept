/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.setupwizard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.Locale;

public class LocaleActivity extends BaseSetupWizardActivity {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Locale mSelection;
    private final Runnable mApplyLocale = () -> {
        if (mSelection != null && !mSelection.equals(Locale.getDefault())) {
            com.android.internal.app.LocalePicker.updateLocale(mSelection);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getGlifLayout().setDescriptionText(getString(R.string.petal_locale_description));
        ListView list = findViewById(R.id.locale_list);
        ArrayAdapter<com.android.internal.app.LocalePicker.LocaleInfo> adapter =
                com.android.internal.app.LocalePicker.constructAdapter(this,
                        R.layout.petal_locale_row, R.id.locale);
        list.setAdapter(adapter);
        mSelection = Locale.getDefault();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).getLocale().equals(mSelection)) {
                list.setItemChecked(i, true);
                list.setSelection(i);
                break;
            }
        }
        list.setOnItemClickListener((parent, view, position, id) -> {
            mSelection = adapter.getItem(position).getLocale();
            ((SetupWizardApp) getApplication()).setIgnoreSimLocale(true);
            mHandler.removeCallbacks(mApplyLocale);
            mHandler.postDelayed(mApplyLocale, 200);
        });
    }

    @Override
    protected void onNextPressed() {
        mHandler.removeCallbacks(mApplyLocale);
        mApplyLocale.run();
        super.onNextPressed();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mApplyLocale);
        super.onDestroy();
    }

    @Override
    protected int getLayoutResId() { return R.layout.setup_locale; }

    @Override
    protected int getTitleResId() { return R.string.setup_locale; }
}
