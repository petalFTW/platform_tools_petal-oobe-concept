/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup.root;

import android.os.CancellationSignal;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface RootAccess {
    boolean isAvailable(CancellationSignal cancellation) throws IOException;
    int execute(List<String> arguments, InputStream input, OutputStream output,
            CancellationSignal cancellation) throws IOException;
}
