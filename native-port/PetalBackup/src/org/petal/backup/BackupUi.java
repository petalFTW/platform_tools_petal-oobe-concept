/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

final class BackupUi {
    private BackupUi() {}

    static void apply(Activity activity, View root) {
        activity.getWindow().setDecorFitsSystemWindows(false);
        activity.getWindow().setNavigationBarContrastEnforced(false);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        root.requestApplyInsets();
        boolean light = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        activity.getWindow().getInsetsController().setSystemBarsAppearance(light
                        ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.backup_slide_enter, R.anim.backup_slide_exit);
        activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.backup_slide_back_enter, R.anim.backup_slide_back_exit);
    }
}
