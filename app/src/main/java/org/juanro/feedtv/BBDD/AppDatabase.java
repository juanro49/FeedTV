/*
 *   Copyright 2026 Juanro49
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.juanro.feedtv.BBDD;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serial;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Database(entities = {RssFeed.class, Article.class}, version = AppDatabase.VERSION)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    public static final String DATABASE_NAME = "FeedTV.db";
    public static final int VERSION = 2;

    private static volatile AppDatabase INSTANCE;

    public abstract FeedDao feedDao();
    public abstract ArticleDao articleDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context,
                                    AppDatabase.class, DATABASE_NAME)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // La migración legada se hace de forma síncrona aquí para asegurar 
                                    // que los datos estén listos en el primer inicio de la app tras actualizar.
                                    migrateLegacyData(context, db);
                                }
                            })
                            .addMigrations(new AssetFileBasedMigration(context, 2))
                            .fallbackToDestructiveMigration(true)
                            .build();

                    // Perform a sanity check to ensure migrations and schema validation are successful.
                    try {
                        INSTANCE.getOpenHelper().getWritableDatabase();
                    } catch (DatabaseMigrationException e) {
                        Log.e(TAG, "Critical migration error detected. Attempting recovery...", e);
                        handleMigrationFailure(context, e.getVersion());
                        throw e;
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Database schema mismatch detected. Attempting recovery...", e);
                        handleMigrationFailure(context, VERSION);
                        throw e;
                    }
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Attempts to recover from a failed migration by backing up the database file
     * and downgrading the database version so that the migration can be retried after fixes.
     */
    private static void handleMigrationFailure(Context context, int failedVersion) {
        try {
            var dbPath = context.getDatabasePath(DATABASE_NAME);
            if (dbPath.exists()) {
                // 1. Auto-export/backup the database before rollback to allow manual recovery
                exportFailedDatabase(context, dbPath, failedVersion);

                // 2. Perform rollback
                try (var db = SQLiteDatabase.openDatabase(
                        dbPath.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE)) {
                    int currentVersion = db.getVersion();
                    int targetVersion = failedVersion - 1;
                    if (currentVersion >= failedVersion) {
                        Log.w(TAG, String.format(Locale.US, "Rolling back database version from %d to %d to allow retry.", currentVersion, targetVersion));
                        db.setVersion(targetVersion);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform migration rollback", e);
        }
    }

    /**
     * Copies the database file to a location accessible by the user for recovery purposes.
     * Uses MediaStore on Android 10+ for better visibility.
     */
    private static void exportFailedDatabase(Context context, File source, int version) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = String.format(Locale.US, "FeedTV_failed_migration_v%d_%s.db", version, timeStamp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportToDownloads(context, source, fileName);
        } else {
            exportToExternalFiles(context, source, fileName);
        }
    }

    /**
     * Exports the database to the public Downloads folder using MediaStore (Android 10+).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private static void exportToDownloads(Context context, File source, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/x-sqlite3");
        values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/FeedTV_Recovery");

        Uri externalUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        Uri fileUri = context.getContentResolver().insert(externalUri, values);

        if (fileUri != null) {
            try (InputStream is = new FileInputStream(source);
                 OutputStream os = context.getContentResolver().openOutputStream(fileUri)) {
                if (os != null) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
                    Log.i(TAG, "Database successfully backed up for recovery at Download/FeedTV_Recovery/" + fileName);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to export database to Downloads", e);
            }
        }
    }

    /**
     * Fallback for Android < 10: Exports to app-specific external files directory.
     */
    private static void exportToExternalFiles(Context context, File source, String fileName) {
        try {
            File externalDir = context.getExternalFilesDir(null);
            if (externalDir != null) {
                File destination = new File(externalDir, fileName);
                try (FileInputStream fis = new FileInputStream(source);
                     FileOutputStream fos = new FileOutputStream(destination);
                     FileChannel sourceChannel = fis.getChannel();
                     FileChannel destChannel = fos.getChannel()) {
                    destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                    Log.i(TAG, "Database successfully backed up for recovery at: " + destination.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to export database to external files", e);
        }
    }

    public static void resetInstance() {
        synchronized (AppDatabase.class) {
            if (INSTANCE != null) {
                INSTANCE.close();
                INSTANCE = null;
            }
        }
    }

    /**
     * Clase para manejar migraciones basadas en archivos SQL en assets.
     * Basada en la implementación de Autu Mandu con optimizaciones.
     */
    private static class AssetFileBasedMigration extends Migration {
        private final int mNewVersion;
        private final Context mContext;

        AssetFileBasedMigration(Context context, int newVersion) {
            super(newVersion - 1, newVersion);
            this.mNewVersion = newVersion;
            this.mContext = context.getApplicationContext();
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.i(TAG, String.format(Locale.US, "Migrating database to version %d...", mNewVersion));
            try (var reader = new BufferedReader(new InputStreamReader(
                    mContext.getAssets().open(String.format(Locale.US, "migrations/%d.sql", mNewVersion))))) {

                var statement = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    // Remove SQL comments (--)
                    int commentIndex = line.indexOf("--");
                    if (commentIndex != -1) {
                        line = line.substring(0, commentIndex);
                    }

                    var trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        continue;
                    }

                    statement.append(line);
                    if (trimmedLine.endsWith(";")) {
                        try {
                            database.execSQL(statement.toString());
                        } catch (SQLException e) {
                            var message = e.getMessage();
                            if (message != null && message.contains("duplicate column name")) {
                                Log.w(TAG, "Ignoring duplicate column error during migration: " + message);
                            } else {
                                throw e;
                            }
                        }
                        statement.setLength(0);
                    } else {
                        statement.append(" ");
                    }
                }
                Log.i(TAG, String.format(Locale.US, "Migration to version %d completed successfully.", mNewVersion));
            } catch (IOException e) {
                Log.e(TAG, String.format(Locale.US, "Error during migration to version %d.", mNewVersion), e);
                throw new DatabaseMigrationException(mNewVersion, e);
            }
        }
    }

    public static class DatabaseMigrationException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        private final int version;

        public DatabaseMigrationException(int version, Throwable cause) {
            super("Critical error during database migration to version " + version, cause);
            this.version = version;
        }

        public int getVersion() {
            return version;
        }
    }

    /**
     * Migra los datos de las bases de datos antiguas (ListaFeeds.db y Feed.db) a Room v2.
     * Este proceso es atómico gracias al uso de transacciones de SupportSQLiteDatabase.
     */
    private static void migrateLegacyData(Context context, SupportSQLiteDatabase newDb) {
        File oldListDbFile = context.getDatabasePath("ListaFeeds.db");
        File oldFeedDbFile = context.getDatabasePath("Feed.db");

        if (!oldListDbFile.exists()) {
            return;
        }

        Log.i(TAG, "Iniciando migración transparente de datos antiguos a Room v2...");

        newDb.beginTransaction();
        try (SQLiteDatabase oldListDb = SQLiteDatabase.openDatabase(oldListDbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
            SQLiteDatabase oldFeedDb = null;
            if (oldFeedDbFile.exists()) {
                oldFeedDb = SQLiteDatabase.openDatabase(oldFeedDbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            }

            try (Cursor cursorFeeds = oldListDb.query("fuentes", null, null, null, null, null, null)) {
                while (cursorFeeds.moveToNext()) {
                    String titulo = cursorFeeds.getString(cursorFeeds.getColumnIndexOrThrow("titulo"));
                    String url = cursorFeeds.getString(cursorFeeds.getColumnIndexOrThrow("url"));

                    ContentValues feedValues = new ContentValues();
                    feedValues.put("title", titulo);
                    feedValues.put("url", url);
                    long feedId = newDb.insert("fuentes", SQLiteDatabase.CONFLICT_REPLACE, feedValues);

                    if (oldFeedDb != null && feedId != -1) {
                        String[] possibleTables = {titulo, titulo + "_", "entrada"};
                        for (String tableName : possibleTables) {
                            if (tableExists(oldFeedDb, tableName)) {
                                try (Cursor cArt = oldFeedDb.query("'" + tableName + "'", null, null, null, null, null, null)) {
                                    while (cArt.moveToNext()) {
                                        ContentValues artValues = new ContentValues();
                                        artValues.put("feedId", (int) feedId);
                                        artValues.put("title", cArt.getString(cArt.getColumnIndexOrThrow("titulo")));
                                        artValues.put("pubDate", cArt.getString(cArt.getColumnIndexOrThrow("fecha")));
                                        artValues.put("link", cArt.getString(cArt.getColumnIndexOrThrow("url")));
                                        artValues.put("image", cArt.getString(cArt.getColumnIndexOrThrow("thumb_url")));
                                        artValues.put("numFecha", cArt.getLong(cArt.getColumnIndexOrThrow("numFecha")));
                                        artValues.put("categories", ""); 
                                        newDb.insert("articulos", SQLiteDatabase.CONFLICT_REPLACE, artValues);
                                    }
                                }
                                break; 
                            }
                        }
                    }
                }
            }

            if (oldFeedDb != null) oldFeedDb.close();
            newDb.setTransactionSuccessful();

            Log.i(TAG, "Migración completada con éxito. Eliminando archivos antiguos...");
            context.deleteDatabase("ListaFeeds.db");
            context.deleteDatabase("Feed.db");

        } catch (Exception e) {
            Log.e(TAG, "Error durante la migración de datos: " + e.getMessage(), e);
        } finally {
            newDb.endTransaction();
        }
    }

    private static boolean tableExists(SQLiteDatabase db, String tableName) {
        try (Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableName})) {
            return cursor.moveToFirst();
        }
    }
}
