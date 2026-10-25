/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup.core;

import android.os.CancellationSignal;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.petal.backup.storage.BackupStore;

public final class PreparedBackupEngine implements BackupEngine {
    @Override
    public boolean isRestoreReady() {
        return false;
    }

    @Override
    public Set<BackupPlan.Content> supportedContent() {
        return Collections.emptySet();
    }

    @Override
    public void backup(BackupPlan plan, BackupStore store, CancellationSignal cancellation,
            ProgressListener listener) throws IOException {
        throw new IOException("The backup engine is not implemented yet");
    }

    @Override
    public void restore(String snapshotId, BackupStore store, CancellationSignal cancellation,
            ProgressListener listener) throws IOException {
        throw new IOException("The restore engine is not implemented yet");
    }
}
