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
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.juanro.feedtv.BBDD.FeedDatabase;
import org.juanro.feedtv.BBDD.RssList;

public class AddFeed extends AppCompatActivity
{
	private EditText nombre, url;
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

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
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

	public void AddFeed(View view)
	{
		if(nombre.getText().toString().isEmpty() || url.getText().toString().isEmpty())
		{
			Toast.makeText(this, "Ambos campos son obligatorios", Toast.LENGTH_LONG).show();
		}
		else
		{
			fuentes = new RssList(this);
			fuentes.insertarEntrada(nombre.getText().toString(), url.getText().toString());

			FeedDatabase.getInstance(getApplicationContext()).crearTabla(nombre.getText().toString());
			Toast.makeText(this, "Fuente añadida correctamente", Toast.LENGTH_LONG).show();
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
