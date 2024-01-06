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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.bjoernpetersen.m3u.model.M3uEntry;

import org.juanro.feedtv.Adapters.RadiosAdapter;
import org.juanro.feedtv.Radio.M3UParser;

import java.util.List;

/**
 * Clase que muestra la lista de canales
 */
public class RadioActivity extends AppCompatActivity implements M3UParser.ResponseServerCallback
{
	private RadiosAdapter mAdapter;
	private RecyclerView lista;
	private SwipeRefreshLayout swipe;
	private SharedPreferences sharedPref;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_radio);


		swipe = findViewById(R.id.swiperefresh);

		lista = findViewById(R.id.lista);
		lista.setLayoutManager(new LinearLayoutManager(this));
		lista.setItemAnimator(new DefaultItemAnimator());
		lista.setHasFixedSize(true);

		// Establecer botón de atrás en action bar
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// Recargar elementos con swipe
		swipe.setOnRefreshListener(() ->
		{
			Thread cargar1 = new Thread(cargarDatos);
			cargar1.start();
		});

		// Cargar los canales del M3U remoto en un nuevo hilo
		swipe.setRefreshing(true);
		Thread cargar = new Thread(cargarDatos);
		cargar.start();
	}

	/**
	 * Runnable para crear el hilo de la carga de datos
	 */
	private final Runnable cargarDatos = () -> M3UParser.getInstance().loadRadios(true, RadioActivity.this, RadioActivity.this);

	/**
	 * Crear la lista con los ambitos obtenidos
	 *
	 * @param entradasM3u
	 */
	@Override
	public void onChannelsLoadServer(List<M3uEntry> entradasM3u)
	{
		mAdapter = new RadiosAdapter(getApplicationContext(), entradasM3u);
		lista.setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();

		//Detener animación de carga
		swipe.setRefreshing(false);
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
