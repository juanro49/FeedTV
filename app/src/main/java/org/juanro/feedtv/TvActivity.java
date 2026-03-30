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
 *   Basado en la clase original creada por LaQuay
 *
 */

package org.juanro.feedtv;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.juanro.feedtv.Adapters.AmbitsAdapter;
import org.juanro.feedtv.TV.Ambito;
import org.juanro.feedtv.TV.JSONParser;
import org.juanro.feedtv.databinding.ActivityTvBinding;

import java.util.List;

/**
 * Clase que muestra la lista de canales
 */
public class TvActivity extends AppCompatActivity implements JSONParser.ResponseServerCallback
{
	private ActivityTvBinding binding;
	private AmbitsAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		binding = ActivityTvBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.lista.setLayoutManager(new GridLayoutManager(this, 2));
		binding.lista.setItemAnimator(new DefaultItemAnimator());
		binding.lista.setHasFixedSize(true);

		// Establecer botón de atrás en action bar
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// Recargar elementos con swipe
		binding.swiperefresh.setOnRefreshListener(() -> new Thread(cargarDatos).start());

		// Cargar los canales del JSON remoto en un nuevo hilo
		binding.swiperefresh.setRefreshing(true);
		new Thread(cargarDatos).start();
	}

	/**
	 * Runnable para crear el hilo de la carga de datos
	 */
	private final Runnable cargarDatos = () -> JSONParser.getInstance().loadChannels(true, TvActivity.this, TvActivity.this);

	/**
	 * Crear la lista con los ambitos obtenidos
	 *
	 * @param ambitos lista de ámbitos
	 */
	@Override
	public void onChannelsLoadServer(List<Ambito> ambitos)
	{
		runOnUiThread(() -> {
			mAdapter = new AmbitsAdapter(getApplicationContext(), ambitos);
			binding.lista.setAdapter(mAdapter);

			//Detener animación de carga
			binding.swiperefresh.setRefreshing(false);
		});
	}

	/**
	 * Crear menu
	 *
	 * @param menu menu a crear
	 * @return true si se crea correctamente
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);

		if (searchItem != null)
		{
			// Cambiar color botón de búsqueda de forma más directa
			if (searchItem.getIcon() != null)
			{
				searchItem.getIcon().setTint(ContextCompat.getColor(this, android.R.color.white));
			}

			// Configurar el buscador una sola vez
			SearchView searchView = (SearchView) searchItem.getActionView();
			if (searchView != null)
			{
				searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
				{
					@Override
					public boolean onQueryTextSubmit(String texto) { return false; }

					@Override
					public boolean onQueryTextChange(String texto)
					{
						if (mAdapter != null)
						{
							mAdapter.getFilter().filter(texto);
						}
						return true;
					}
				});
			}
		}

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Acciones botones menú
	 *
	 * @param item item pulsado
	 * @return true si se maneja la pulsación
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			getOnBackPressedDispatcher().onBackPressed();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Método que aplica el tema de la aplicación
	 */
	private void aplicarTema()
	{
		SharedPreferences sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

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
