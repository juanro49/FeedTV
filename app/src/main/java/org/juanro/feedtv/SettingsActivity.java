/*
 *   Copyright 2019 Juanro49
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

package org.juanro.feedtv;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

/**
 * Clase que representa la activity de ajustes
 */
public class SettingsActivity extends AppCompatActivity {

    public SettingsActivity() {
        super(R.layout.settings_activity);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        aplicarTema();
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            if (sp != null) {
                sp.registerOnSharedPreferenceChangeListener(this);
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            if (sp != null) {
                sp.unregisterOnSharedPreferenceChangeListener(this);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key == null) return;

            switch (key) {
                case "lang":
                    String lang = sharedPreferences.getString("lang", "auto");
                    AppCompatDelegate.setApplicationLocales(
                            "auto".equals(lang) ? LocaleListCompat.getEmptyLocaleList() : LocaleListCompat.forLanguageTags(lang)
                    );
                    break;
                case "tema":
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                    break;
            }
        }
    }

    private void aplicarTema() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme("Claro".equals(sharedPref.getString("tema", "Claro")) ? R.style.TemaClaro : R.style.TemaOscuro);
    }
}
