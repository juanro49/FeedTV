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

package org.juanro.feedtv.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import org.juanro.feedtv.util.backup.Backup;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ViewModel for backup operations.
 */
public class BackupViewModel extends AndroidViewModel {
    private final Backup backup;
    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public BackupViewModel(@NonNull Application application) {
        super(application);
        this.backup = new Backup(application);
    }

    public void runBackup(Consumer<String> onComplete, Runnable onError) {
        DB_EXECUTOR.execute(() -> {
            String fileName = backup.backup();
            if (fileName != null) {
                onComplete.accept(fileName);
            } else {
                onError.run();
            }
        });
    }

    public void runBackup(Uri targetUri, Runnable onComplete, Runnable onError) {
        DB_EXECUTOR.execute(() -> {
            if (backup.backup(targetUri)) {
                onComplete.run();
            } else {
                onError.run();
            }
        });
    }

    public void runRestore(Uri backupUri, Runnable onComplete, Runnable onError) {
        DB_EXECUTOR.execute(() -> {
            if (backup.restore(backupUri)) {
                onComplete.run();
            } else {
                onError.run();
            }
        });
    }

    public String getDefaultFileName() {
        return backup.getDefaultFileName();
    }
}
