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
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Database(entities = {RssFeed.class, Article.class}, version = 2)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;

    public abstract FeedDao feedDao();
    public abstract ArticleDao articleDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "FeedTV.db")
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // La migración legada se hace de forma síncrona aquí para asegurar 
                                    // que los datos estén listos en el primer inicio de la app tras actualizar.
                                    migrateLegacyData(context.getApplicationContext(), db);
                                }
                            })
                            .addMigrations(new AssetFileBasedMigration(context, 2))
                            .fallbackToDestructiveMigration(true)
                            .build();
                }
            }
        }
        return INSTANCE;
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
            database.beginTransaction();
            try {
                InputStream migrationInput = mContext.getAssets().open(String.format(Locale.US,
                        "migrations/%d.sql", mNewVersion));
                byte[] binaryMigration = new byte[migrationInput.available()];
                //noinspection ResultOfMethodCallIgnored
                migrationInput.read(binaryMigration);
                migrationInput.close();

                String sqlScript = new String(binaryMigration, StandardCharsets.UTF_8);
                for (String preparedStatement : prepareSqlStatements(sqlScript)) {
                    database.execSQL(preparedStatement);
                }
                database.setTransactionSuccessful();

                Log.i(TAG, String.format(Locale.US, "Migrated to new version %d.", mNewVersion));
            } catch (IOException e) {
                Log.e(TAG, String.format(Locale.US, "File based Migration failed for new version %d.", mNewVersion), e);
            } finally {
                database.endTransaction();
            }
        }

        private static List<String> prepareSqlStatements(String rawSql) {
            String[] rawCommands = rawSql.replaceAll("[\r\n]", " ").split(";");
            List<String> commands = new ArrayList<>(rawCommands.length);
            for (String rawCommand : rawCommands) {
                String cmd = rawCommand.trim();
                if (!cmd.isEmpty()) {
                    commands.add(cmd);
                }
            }
            return commands;
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
