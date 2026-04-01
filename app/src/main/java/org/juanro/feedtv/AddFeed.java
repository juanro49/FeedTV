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

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.juanro.feedtv.BBDD.AppDatabase;
import org.juanro.feedtv.BBDD.RssFeed;
import org.juanro.feedtv.databinding.AddFeedBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddFeed extends AppCompatActivity
{
	private AddFeedBinding binding;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		binding = AddFeedBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// Modo editar
		if(getIntent().getExtras() != null)
		{
			binding.etNombre.setText(getIntent().getExtras().getString("titulo"));
			binding.etNombre.setFocusable(false);
			binding.etUrl.setText(getIntent().getExtras().getString("url"));
			binding.button.setText(this.getString(R.string.edit_feed));
		}

		// Configurar listeners de botones
		binding.button.setOnClickListener(this::submit);
		binding.btnSeguir.setOnClickListener(this::submitSeguir);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			getOnBackPressedDispatcher().onBackPressed();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	// Añadir o modificar feed
	public void submit(View view)
	{
		if(getIntent().getExtras() == null)
		{
			addFeed();
		}
		else
		{
			modFeed();
		}
	}

	// Seguir tema de Google News o Bing News
	public void submitSeguir(View view)
	{
		String topic = binding.etTema.getText().toString().replace(" ", "+");

		if(topic.isEmpty() || (!binding.rbGoogle.isChecked() && !binding.rbBing.isChecked()))
		{
			Toast.makeText(this, this.getString(R.string.add_feed_empty), Toast.LENGTH_LONG).show();
		}
		else
		{
			Configuration config = getResources().getConfiguration();
			final String url;
			final String finalTopic;

			if(binding.rbGoogle.isChecked())
			{
				if(binding.cbGN.isChecked())
				{
					url = "https://news.google.com/rss/search?q=" + topic + "&hl=es-419&gl=CU&ceid=CU:es-419";
				}
				else
				{
					url = "https://news.google.com/rss/search?q=" + topic + "&hl=" + config.getLocales().get(0).getLanguage();
				}
				finalTopic = topic.replaceAll("[+| -]", "") + "GN";
			}
			else
			{
				url = "https://www.bing.com/news/search?q=" + topic + "&format=rss";
				finalTopic = topic.replaceAll("[+| -]", "") + "BN";
			}

			executor.execute(() -> {
				AppDatabase db = AppDatabase.getInstance(getApplicationContext());
				db.feedDao().insert(new RssFeed(finalTopic, url));
				runOnUiThread(() -> {
					Toast.makeText(AddFeed.this, getString(R.string.add_feed_success), Toast.LENGTH_LONG).show();
					finish();
				});
			});
		}
	}

	public void addFeed()
	{
		final String titulo = binding.etNombre.getText().toString();
		final String urlStr = binding.etUrl.getText().toString();

		if(titulo.isEmpty() || urlStr.isEmpty())
		{
			Toast.makeText(this, this.getString(R.string.add_feed_empty), Toast.LENGTH_LONG).show();
		}
		else
		{
			executor.execute(() -> {
				AppDatabase db = AppDatabase.getInstance(getApplicationContext());
				String cleanTitulo = titulo.replaceAll("[+| -]", "");
				db.feedDao().insert(new RssFeed(cleanTitulo, urlStr));
				runOnUiThread(() -> {
					Toast.makeText(AddFeed.this, getString(R.string.add_feed_success), Toast.LENGTH_LONG).show();
					finish();
				});
			});
		}
	}

	public void modFeed()
	{
		final String titulo = binding.etNombre.getText().toString();
		final String urlStr = binding.etUrl.getText().toString();

		if(titulo.isEmpty() || urlStr.isEmpty())
		{
			Toast.makeText(this, this.getString(R.string.add_feed_empty), Toast.LENGTH_LONG).show();
		}
		else
		{
			executor.execute(() -> {
				AppDatabase db = AppDatabase.getInstance(getApplicationContext());
				RssFeed existingFeed = db.feedDao().findByTitle(titulo);
				if (existingFeed != null) {
					existingFeed.setUrl(urlStr);
					db.feedDao().update(existingFeed);
					runOnUiThread(() -> {
						Toast.makeText(AddFeed.this, getString(R.string.mod_feed_success), Toast.LENGTH_LONG).show();
						finish();
					});
				}
			});
		}
	}

	/**
	 * Método que aplica el tema de la aplicación
	 */
	private void aplicarTema()
	{
		SharedPreferences sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

		if("Claro".equals(sharedPref.getString("tema", "Claro")))
		{
			setTheme(R.style.TemaClaro);
		}
		else
		{
			setTheme(R.style.TemaOscuro);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		executor.shutdown();
	}
}
