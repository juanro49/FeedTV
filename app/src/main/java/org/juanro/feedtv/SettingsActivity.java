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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.juanro.feedtv.util.Preferences;
import org.juanro.feedtv.viewmodel.BackupViewModel;

/**
 * Clase que representa la activity de ajustes
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        setSupportActionBar(findViewById(R.id.toolbar));

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
        private BackupViewModel backupViewModel;
        private Preferences prefs;
        
        private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/x-sqlite3"),
                uri -> {
                    if (uri != null) {
                        backupViewModel.runBackup(uri,
                                () -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), R.string.backup_success, Toast.LENGTH_SHORT).show());
                                    }
                                },
                                () -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), R.string.backup_error, Toast.LENGTH_SHORT).show());
                                    }
                                }
                        );
                    }
                }
        );

        private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        backupViewModel.runRestore(uri,
                                () -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), R.string.restore_success, Toast.LENGTH_LONG).show();
                                            FeedTVApplication.recreateAllActivities();
                                            getActivity().finish();
                                        });
                                    }
                                },
                                () -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), R.string.restore_error, Toast.LENGTH_SHORT).show());
                                    }
                                }
                        );
                    }
                }
        );

        private final ActivityResultLauncher<Intent> mOpenDocumentTreeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && getContext() != null) {
                            getContext().getContentResolver().takePersistableUriPermission(uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            prefs.setBackupPath(uri.toString());
                            updateBackupPathSummary();
                        }
                    }
                }
        );

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            backupViewModel = new ViewModelProvider(this).get(BackupViewModel.class);
            if (getContext() != null) {
                prefs = new Preferences(getContext());
            }

            checkSystemPrivateDns();

            EditTextPreference dohUrlPreference = findPreference("doh_url");
            if (dohUrlPreference != null) {
                dohUrlPreference.setOnBindEditTextListener(editText -> {
                    editText.setHint(R.string.doh_default_url);
                });
            }

            EditTextPreference articleLimitPreference = findPreference("article_limit");
            if (articleLimitPreference != null) {
                articleLimitPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    try {
                        int val = Integer.parseInt((String) newValue);
                        return val > 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                });
            }

            Preference exportBackup = findPreference("export_backup");
            if (exportBackup != null) {
                exportBackup.setOnPreferenceClickListener(preference -> {
                    if (prefs.getBackupPath().startsWith("content://")) {
                        backupViewModel.runBackup(
                                fileName -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), getString(R.string.toast_backup_succeeded, fileName), Toast.LENGTH_SHORT).show());
                                    }
                                },
                                () -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() ->
                                                Toast.makeText(getContext(), R.string.backup_error, Toast.LENGTH_SHORT).show());
                                    }
                                }
                        );
                    } else {
                        exportLauncher.launch(backupViewModel.getDefaultFileName());
                    }
                    return true;
                });
            }

            Preference importBackup = findPreference("import_backup");
            if (importBackup != null) {
                importBackup.setOnPreferenceClickListener(preference -> {
                    importLauncher.launch(new String[]{"*/*"});
                    return true;
                });
            }

            Preference backupFolder = findPreference("backup_folder");
            if (backupFolder != null) {
                backupFolder.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    mOpenDocumentTreeLauncher.launch(intent);
                    return true;
                });
            }

            Preference backupFolderDefault = findPreference("backup_folder_default");
            if (backupFolderDefault != null) {
                backupFolderDefault.setOnPreferenceClickListener(preference -> {
                    prefs.restoreDefaultBackupPath();
                    updateBackupPathSummary();
                    return true;
                });
            }

            updateBackupPathSummary();
        }

        private void updateBackupPathSummary() {
            Preference backupFolder = findPreference("backup_folder");
            if (backupFolder != null && prefs != null) {
                backupFolder.setSummary(prefs.getBackupPath());
            }
            Preference backupFolderDefault = findPreference("backup_folder_default");
            if (backupFolderDefault != null && prefs != null) {
                backupFolderDefault.setSummary(prefs.getDefaultBackupPath());
            }
        }

        private void checkSystemPrivateDns() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getContext() != null) {
                ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    Network activeNetwork = cm.getActiveNetwork();
                    LinkProperties lp = cm.getLinkProperties(activeNetwork);
                    if (lp != null && lp.getPrivateDnsServerName() != null) {
                        // El sistema ya usa DNS Privado
                        SwitchPreferenceCompat dohSwitch = findPreference("doh_enabled");
                        Preference dohUrl = findPreference("doh_url");
                        if (dohSwitch != null) {
                            dohSwitch.setEnabled(false);
                            dohSwitch.setSummary(getString(R.string.doh_system_active));
                        }
                        if (dohUrl != null) {
                            dohUrl.setEnabled(false);
                        }
                    }
                }
            }
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
                    String theme = sharedPreferences.getString("tema", "default");
                    switch (theme) {
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
                    break;
                case "dynamic_color":
                    // Para Dynamic Color, recrear la actividad es suficiente si el Precondition está en Application
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                    break;
            }
        }
    }


}
