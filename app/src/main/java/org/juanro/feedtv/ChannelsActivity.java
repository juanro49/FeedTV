/*
 *   Copyright 2021 Juanro49
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
 *   Basado en la clase original creada por LaQuay
 *
 */

package org.juanro.feedtv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.juanro.feedtv.Adapters.ChannelsAdapter;
import org.juanro.feedtv.TV.Ambito;

/**
 * Clase que muestra la lista de canales
 */
public class ChannelsActivity extends AppCompatActivity
{
	private ChannelsAdapter mAdapter;
	private RecyclerView lista;
	private SharedPreferences sharedPref;
	private Ambito ambito;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tv);

		lista = findViewById(R.id.lista);
		lista.setLayoutManager(new GridLayoutManager(this, 2));
		lista.setItemAnimator(new DefaultItemAnimator());
		lista.setHasFixedSize(true);

		// Establecer botón de atrás en action bar
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// Cargamos el archivo de preferencias
		sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

		// Obtener el ambito de la anterior activity que contiene sus canales
		Intent intent = getIntent();
		ambito = (Ambito) intent.getSerializableExtra("Ambito");

		// Crear el adapter de los canales de ese ambito y listarlo
		mAdapter = new ChannelsAdapter(getApplicationContext(), ambito.getCanales());
		lista.setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();
	}

	/**
	 * Crear menu
	 *
	 * @param menu
	 * @return
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);

		// Cambiar color botón de búsqueda
		if (getApplicationContext() != null)
		{
			Drawable drawable = DrawableCompat.wrap(searchItem.getIcon());
			DrawableCompat.setTint(drawable, ContextCompat.getColor(getApplicationContext(), android.R.color.white));
			menu.findItem(R.id.action_search).setIcon(drawable);
		}

		return true;
	}

	/**
	 * Acciones botones menú
	 *
	 * @param item
	 * @return
	 */
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
			{
				this.onBackPressed();
				return true;
			}

			case R.id.action_search:
			{
				// Mostrar resultados búsqueda
				SearchView searchView = (SearchView) item.getActionView();
				searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
				{
					@Override
					public boolean onQueryTextSubmit(String texto)
					{
						return false;
					}

					@Override
					public boolean onQueryTextChange(String texto)
					{
						if (mAdapter != null)
						{
							mAdapter.getFilter().filter(texto);
						}

						return false;
					}
				});
			}
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
}
