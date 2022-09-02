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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.juanro.feedtv.BBDD.FeedDatabase;
import org.juanro.feedtv.BBDD.RssList;

public class AddFeed extends AppCompatActivity
{
	private EditText nombre, url, tema;
	private Button submit;
	private RadioButton google, bing;
	private CheckBox unlockGN;
	private RssList fuentes;
	private SharedPreferences sharedPref;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_feed);

		nombre = findViewById(R.id.etNombre);
		url = findViewById(R.id.etUrl);
		submit = findViewById(R.id.button);
		tema = findViewById(R.id.etTema);
		google = findViewById(R.id.rbGoogle);
		bing = findViewById(R.id.rbBing);
		unlockGN = findViewById(R.id.cbGN);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// Modo editar
		if(getIntent().getExtras() != null)
		{
			nombre.setText(getIntent().getExtras().getString("titulo"));
			nombre.setFocusable(false);
			url.setText(getIntent().getExtras().getString("url"));
			submit.setText(this.getString(R.string.edit_feed));
		}
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			this.onBackPressed();
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
		String topic = tema.getText().toString().replace(" ", "+");

		if(tema.getText().toString().isEmpty() || !google.isChecked() && !bing.isChecked())
		{
			Toast.makeText(this, this.getString(R.string.add_feed_empty), Toast.LENGTH_LONG).show();
		}
		else
		{
			Configuration config = getResources().getConfiguration();
			String url = "";
			fuentes = new RssList(this);

			if(google.isChecked())
			{
				if(unlockGN.isChecked())
				{
					url = "https://news.google.com/rss/search?q=" + topic + "&hl=es-419&gl=CU&ceid=CU:es-419";
				}
				else
				{
					url = "https://news.google.com/rss/search?q=" + topic + "&hl=" + config.locale.getLanguage();
				}

				topic = topic.replaceAll("[+| ]", "") + "GN";
				fuentes.insertarEntrada(topic, url);
				FeedDatabase.getInstance(getApplicationContext()).crearTabla(topic + "_");
			}
			else if(bing.isChecked())
			{
				url = "https://www.bing.com/news/search?q=" + topic + "&format=rss";
				topic = topic.replaceAll("[+| ]", "") + "BN";
				fuentes.insertarEntrada(topic, url);
				FeedDatabase.getInstance(getApplicationContext()).crearTabla(topic + "_");
			}

			Toast.makeText(this, this.getString(R.string.add_feed_success), Toast.LENGTH_LONG).show();
		}
	}

	public void addFeed()
	{
		String titulo = nombre.getText().toString();

		if(nombre.getText().toString().isEmpty() || url.getText().toString().isEmpty())
		{
			Toast.makeText(this, this.getString(R.string.add_feed_empty), Toast.LENGTH_LONG).show();
		}
		else
		{
			fuentes = new RssList(this);
			titulo = titulo.replaceAll("[+| ]", "");
			fuentes.insertarEntrada(titulo, url.getText().toString());

			FeedDatabase.getInstance(getApplicationContext()).crearTabla(titulo + "_");
			Toast.makeText(this, this.getString(R.string.add_feed_success), Toast.LENGTH_LONG).show();
		}
	}

	public void modFeed()
	{
		if(nombre.getText().toString().isEmpty() || url.getText().toString().isEmpty())
		{
			Toast.makeText(this, this.getString(R.string.add_feed_empty), Toast.LENGTH_LONG).show();
		}
		else
		{
			fuentes = new RssList(this);
			fuentes.editarEntrada(nombre.getText().toString(), url.getText().toString());

			Toast.makeText(this, this.getString(R.string.mod_feed_success), Toast.LENGTH_LONG).show();
		}
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
}
