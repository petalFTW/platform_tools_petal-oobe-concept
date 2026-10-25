/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup.core;

import android.os.CancellationSignal;
import java.io.IOException;
import java.util.Set;
import org.petal.backup.storage.BackupStore;

// Blocking work belongs on the job worker, not the bloody activity thread.
public interface BackupEngine {
    boolean isRestoreReady();
    Set<BackupPlan.Content> supportedContent();
    void backup(BackupPlan plan, BackupStore store, CancellationSignal cancellation,
            ProgressListener listener) throws IOException;
    void restore(String snapshotId, BackupStore store, CancellationSignal cancellation,
            ProgressListener listener) throws IOException;

    interface ProgressListener {
        void onProgress(String item, long completedBytes, long totalBytes);
    }
}
