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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;

import android.os.Bundle;
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
		super.onCreate(savedInstanceState);

		// Habilitar Edge-to-Edge
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

		binding = ActivityTvBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.lista.setLayoutManager(new GridLayoutManager(this, 2));
		binding.lista.setItemAnimator(new DefaultItemAnimator());
		binding.lista.setHasFixedSize(true);

		// Configurar SearchBar persistente
		binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

		binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				binding.searchView.clearFocus();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (mAdapter != null) {
					mAdapter.getFilter().filter(newText);
				}
				return true;
			}
		});

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
	 * Acciones botones menú
	 *
	 * @param item item pulsado
	 * @return true si se maneja la pulsación
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		return super.onOptionsItemSelected(item);
	}
}
