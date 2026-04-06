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
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;

import org.juanro.feedtv.Adapters.ChannelsAdapter;
import org.juanro.feedtv.TV.Ambito;
import org.juanro.feedtv.databinding.ActivityTvBinding;

/**
 * Clase que muestra la lista de canales
 */
public class ChannelsActivity extends AppCompatActivity
{
	private ActivityTvBinding binding;
	private ChannelsAdapter mAdapter;

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

		// Obtener el ambito de la anterior activity que contiene sus canales
		Intent intent = getIntent();
		Ambito ambito;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			ambito = intent.getSerializableExtra("Ambito", Ambito.class);
		}
		else
		{
			ambito = (Ambito) intent.getSerializableExtra("Ambito");
		}

		if (ambito != null)
		{
			// Crear el adapter de los canales de ese ambito y listarlo
			mAdapter = new ChannelsAdapter(getApplicationContext(), ambito.canales());
			binding.lista.setAdapter(mAdapter);
		}

		binding.swiperefresh.setEnabled(false);
	}

	/**
	 * Acciones botones menú
	 *
	 * @param item El elemento de menú que fue seleccionado.
	 * @return boolean Devuelve true si se maneja la selección.
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		return super.onOptionsItemSelected(item);
	}
}
