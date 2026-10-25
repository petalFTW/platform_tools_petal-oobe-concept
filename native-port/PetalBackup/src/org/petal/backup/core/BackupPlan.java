/* Copyright (C) 2026 petalOS; SPDX-License-Identifier: Apache-2.0 */
package org.petal.backup.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class BackupPlan {
    public enum Content { APK, PRIVATE_APP_DATA, EXTERNAL_APP_DATA, SMS, CALL_LOGS, WALLPAPER }

    public final int userId;
    public final List<String> packages;
    public final Set<Content> content;

    public BackupPlan(int userId, List<String> packages, Set<Content> content) {
        if (userId < 0) throw new IllegalArgumentException("A user ID is required");
        if (Objects.requireNonNull(content).isEmpty()) {
            throw new IllegalArgumentException("Choose something to back up");
        }
        ArrayList<String> names = new ArrayList<>(Objects.requireNonNull(packages));
        for (String name : names) {
            if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)*")) {
                throw new IllegalArgumentException("Invalid package name");
            }
        }
        this.userId = userId;
        this.packages = Collections.unmodifiableList(names);
        this.content = Collections.unmodifiableSet(EnumSet.copyOf(content));
    }
}
