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

package org.juanro.feedtv.util.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.juanro.feedtv.FeedTVApplication;
import org.juanro.feedtv.BBDD.AppDatabase;
import org.juanro.feedtv.util.FileCopyUtil;
import org.juanro.feedtv.util.Preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Class for managing database backups and restores.
 */
public class Backup {
    private static final String TAG = "Backup";
    private static final String DATABASE_NAME = "FeedTV.db";
    private static final String INTERNAL_BACKUP = "rescue.db";

    private final File dbFile;
    private final ContentResolver resolver;
    private final Context mContext;
    private final Preferences prefs;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public Backup(Context context) {
        this.dbFile = context.getDatabasePath(DATABASE_NAME);
        this.mContext = context;
        this.resolver = context.getContentResolver();
        this.prefs = new Preferences(context);
    }

    /**
     * @return The target path for a backup.
     */
    public DocumentFile getBackupDir() {
        DocumentFile backupDir = null;
        String backupPath = prefs.getBackupPath();

        try {
            if (backupPath.startsWith("content://")) {
                backupDir = DocumentFile.fromTreeUri(mContext, Uri.parse(backupPath));
            } else {
                backupDir = DocumentFile.fromFile(new File(backupPath));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting backup directory", e);
        }

        return backupDir;
    }

    /**
     * Trigger a backup.
     * @return The filename of the backup if successful, null otherwise.
     */
    public String backup() {
        // Crucial for Room: Close all connections to flush WAL/SHM files to the main DB file.
        FeedTVApplication.closeDatabases();

        String fileName = "FeedTV-" + dateFormat.format(new Date()) + ".db";
        DocumentFile backupDir = getBackupDir();

        if (backupDir == null) return null;

        DocumentFile backupFile = backupDir.createFile("*/*", fileName);

        if (backupFile == null) {
            // Try to find if it exists and replace (similar to Autu Mandu but simplified)
            DocumentFile file = backupDir.findFile(fileName);
            if (file != null && file.isFile()) {
                file.delete();
                backupFile = backupDir.createFile("*/*", fileName);
            }
        }

        if (backupFile == null) {
            Log.e(TAG, "Backup error, can't create file in path: " + backupDir.getUri());
            return null;
        }

        try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(backupFile.getUri(), "w")) {
            if (pfd == null) return null;
            if (FileCopyUtil.copyFile(dbFile, pfd)) {
                return fileName;
            } else {
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Backup error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Trigger a backup to a specific Uri (SAF CreateDocument).
     * @param targetUri The Uri where the backup should be saved.
     * @return true if successful, false otherwise.
     */
    public boolean backup(Uri targetUri) {
        // Crucial for Room: Close all connections to flush WAL/SHM files to the main DB file.
        FeedTVApplication.closeDatabases();

        try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(targetUri, "w")) {
            if (pfd == null) return false;
            return FileCopyUtil.copyFile(dbFile, pfd);
        } catch (IOException e) {
            Log.e(TAG, "Backup error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Restore from a Uri.
     * @param backupUri The Uri of the selected backup.
     * @return Whether the restore succeed.
     */
    public boolean restore(Uri backupUri) {
        // Crucial for Room: Close all connections before overwriting the file.
        FeedTVApplication.closeDatabases();

        File internalBackupFile = new File(dbFile.getParent(), INTERNAL_BACKUP);
        if (FileCopyUtil.copyFile(dbFile, internalBackupFile)) {
            try (InputStream backupSource = resolver.openInputStream(backupUri);
                 OutputStream backupTarget = new FileOutputStream(dbFile)) {

                if (backupSource != null && FileCopyUtil.copyFile(backupSource, backupTarget)) {
                    if (checkBackupSanity()) {
                        return true;
                    } else {
                        throw new IOException("Backup is insane.");
                    }
                }
                throw new IOException("Copying failed or backup source is null.");
            } catch (IOException e) {
                Log.w(TAG, "Need to restore internally, got Exception.", e);
                FileCopyUtil.copyFile(internalBackupFile, dbFile);
            }
        } else {
            Log.e(TAG, "Could not do an internal Backup before restore.");
        }
        return false;
    }

    private boolean checkBackupSanity() {
        try {
            AppDatabase.getInstance(mContext).getOpenHelper().getReadableDatabase();
            return true;
        } catch (Exception e) {
            FeedTVApplication.closeDatabases();
            Log.e(TAG, "Database is broken.", e);
            return false;
        }
    }

    public String getDefaultFileName() {
        return "FeedTV-" + dateFormat.format(new Date()) + ".db";
    }
}
