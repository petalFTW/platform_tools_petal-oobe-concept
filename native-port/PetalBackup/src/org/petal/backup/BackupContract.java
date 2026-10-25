/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup;

public final class BackupContract {
    public static final String PACKAGE_NAME = "org.petal.backup";
    public static final String ACTION_RESTORE = "org.petal.backup.action.RESTORE";
    public static final String EXTRA_FROM_SETUP = "org.petal.backup.extra.FROM_SETUP";
    public static final String EXTRA_RESTORED = "org.petal.backup.extra.RESTORED";
    public static final String EXTRA_SKIPPED = "org.petal.backup.extra.SKIPPED";
    public static final String METADATA_RESTORE_READY = "org.petal.backup.RESTORE_READY";
    public static final String METADATA_SETUP_UI_READY = "org.petal.backup.SETUP_UI_READY";
    public static final int RESULT_SKIPPED = 1;

    private BackupContract() {}
}
