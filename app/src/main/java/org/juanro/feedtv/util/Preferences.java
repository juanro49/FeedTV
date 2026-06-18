/*
 * Copyright 2026 Juanro49
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.juanro.feedtv.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

/**
 * Utility class for managing application preferences.
 */
public class Preferences {
    public static final String KEY_BACKUP_FOLDER = "backup_folder";
    public static final String KEY_BACKUP_FOLDER_DEFAULT = "backup_folder_default";

    private final Context context;
    private final SharedPreferences prefs;

    public Preferences(@NonNull Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    public String getBackupPath() {
        return prefs.getString(KEY_BACKUP_FOLDER, getAppFilesDirPath());
    }

    public void setBackupPath(@NonNull String path) {
        prefs.edit().putString(KEY_BACKUP_FOLDER, path).apply();
    }

    public String getDefaultBackupPath() {
        return prefs.getString(KEY_BACKUP_FOLDER_DEFAULT, getAppFilesDirPath());
    }

    public void restoreDefaultBackupPath() {
        prefs.edit().remove(KEY_BACKUP_FOLDER).apply();
    }

    @NonNull
    private String getAppFilesDirPath() {
        return context.getFilesDir().getAbsolutePath();
    }
}
