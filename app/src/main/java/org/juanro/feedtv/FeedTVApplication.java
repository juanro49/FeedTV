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
 */

package org.juanro.feedtv;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import org.juanro.feedtv.BBDD.AppDatabase;
import org.juanro.feedtv.Http.HttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FeedTVApplication extends Application {
    private static final String TAG = "FeedTVApplication";
    private static FeedTVApplication instance;
    private final List<Activity> activities = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Seguimiento de todas las actividades para permitir recreación "en caliente"
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                activities.add(activity);
            }
            @Override
            public void onActivityStarted(@NonNull Activity activity) {}
            @Override
            public void onActivityResumed(@NonNull Activity activity) {}
            @Override
            public void onActivityPaused(@NonNull Activity activity) {}
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                activities.remove(activity);
            }
        });
        
        // Inicializar el cliente HTTP optimizado (OkHttp + Conscrypt + Cache)
        HttpClient.init(this);

        aplicarConfiguracionTema();
    }

    public static FeedTVApplication getInstance() {
        return instance;
    }

    /**
     * Cierra la base de datos para permitir operaciones de archivo (backup/restore).
     */
    public static void closeDatabases() {
        Log.i(TAG, "Cerrando base de datos...");
        AppDatabase.resetInstance();
    }

    /**
     * Recrea todas las actividades abiertas para refrescar datos tras una restauración.
     */
    public static void recreateAllActivities() {
        if (instance != null) {
            Log.i(TAG, "Recreando todas las actividades para refrescar datos.");
            synchronized (instance.activities) {
                for (Activity activity : new ArrayList<>(instance.activities)) {
                    activity.recreate();
                }
            }
        }
    }

    private void aplicarConfiguracionTema() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Aplicar Material You (Dynamic Color) con soporte para Expressive
        // Usamos una precondición para permitir activarlo/desactivarlo dinámicamente sin reiniciar la app
        DynamicColors.applyToActivitiesIfAvailable(this, new DynamicColorsOptions.Builder()
                .setPrecondition((activity, themeResId) -> {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
                    return sp.getBoolean("dynamic_color", true);
                })
                .build());

        // Aplicar Modo Noche
        String tema = sharedPref.getString("tema", "default");
        switch (tema) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "default":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
