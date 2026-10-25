/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup.storage;

import android.os.CancellationSignal;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface BackupStore {
    enum Provider { LOCAL_TREE, WEBDAV, S3, SFTP, SMB, GOOGLE_DRIVE, ONEDRIVE }

    Provider provider();
    boolean isConfigured();
    List<String> listSnapshots(CancellationSignal cancellation) throws IOException;
    InputStream openRead(String objectId, CancellationSignal cancellation) throws IOException;
    OutputStream createObject(String objectId, CancellationSignal cancellation) throws IOException;
    void deleteObject(String objectId, CancellationSignal cancellation) throws IOException;
}
