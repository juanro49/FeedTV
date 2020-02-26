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
 *
 */

package org.juanro.feedtv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Locale;

/**
 * Clase que representa la activity de ajustes
 */
public class SettingsActivity extends AppCompatActivity
{
	private SharedPreferences sharedPref;
	private Configuration config = new Configuration();
	private Locale locale;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);
		getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();
		aplicarTema();
		aplicarIdioma();
	}

	public static class SettingsFragment extends PreferenceFragmentCompat
	{
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
		{
			setPreferencesFromResource(R.xml.root_preferences, rootKey);
		}
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				this.onBackPressed();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Método que aplica el tema de la aplicación
	 */
	private void aplicarTema()
	{
		sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

		if(sharedPref.getString("tema", "Claro").equals("Claro"))
		{
			setTheme(R.style.TemaClaro);
		}
		else
		{
			setTheme(R.style.TemaOscuro);
		}
	}

	/**
	 * Método que carga el idioma de la aplicación
	 */
	private void aplicarIdioma()
	{
		sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);
		String lang = sharedPref.getString("lang", "auto");

		switch(lang)
		{
			case "auto":
				config.setToDefaults();
				Log.i(this.getLocalClassName(), this.getString(R.string.idioma_establecido) + lang);
				break;
			case "es":
				locale = new Locale("es");
				config.locale = locale;
				Log.i(this.getLocalClassName(), this.getString(R.string.idioma_establecido) + config.locale.getLanguage());
				break;
			case "ext":
				locale = new Locale("ext");
				config.locale = locale;
				Log.i(this.getLocalClassName(), this.getString(R.string.idioma_establecido) + config.locale.getLanguage());
				break;
			case "en":
				locale = new Locale("en");
				config.locale = locale;
				Log.i(this.getLocalClassName(), this.getString(R.string.idioma_establecido) + config.locale.getLanguage());
				break;
			case "nb":
				locale = new Locale("nb");
				config.locale = locale;
				Log.i(this.getLocalClassName(), this.getString(R.string.idioma_establecido) + config.locale.getLanguage());
				break;
			case "de":
				locale = new Locale("de");
				config.locale = locale;
				Log.i(this.getLocalClassName(), this.getString(R.string.idioma_establecido) + config.locale.getLanguage());
				break;
			case "ca":
				locale = new Locale("ca");
				config.locale = locale;
				Log.i(this.getLocalClassName(), this.getString(R.string.idioma_establecido) + config.locale.getLanguage());
				break;
			case "pl":
				locale = new Locale("pl");
				config.locale = locale;
				Log.i(this.getLocalClassName(), this.getString(R.string.idioma_establecido) + config.locale.getLanguage());
				break;
		}

		getResources().updateConfiguration(config, null);
		Intent refresh = new Intent(getApplicationContext(), MainActivity.class);
		refresh.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(refresh);
		finish();
	}
}