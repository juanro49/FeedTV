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
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
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
	private ChannelsAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		ActivityTvBinding binding = ActivityTvBinding.inflate(getLayoutInflater());
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

		// Obtener el ambito de la anterior activity que contiene sus canales
		Intent intent = getIntent();
		Ambito ambito;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			ambito = intent.getSerializableExtra("Ambito", Ambito.class);
		}
		else
		{
			//noinspection deprecation
			ambito = (Ambito) intent.getSerializableExtra("Ambito");
		}

		if (ambito != null)
		{
			// Crear el adapter de los canales de ese ambito y listarlo
			mAdapter = new ChannelsAdapter(getApplicationContext(), ambito.canales());
			binding.lista.setAdapter(mAdapter);
		}
	}

	/**
	 * Crear menu
	 *
	 * @param menu El menú de opciones en el que se colocan los elementos.
	 * @return boolean Debe devolver true para que se muestre el menú.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);

		if (searchItem != null)
		{
			// Cambiar color botón de búsqueda
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

		return true;
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

		if("Claro".equals(sharedPref.getString("tema", "Claro")))
		{
			setTheme(R.style.TemaClaro);
		}
		else
		{
			setTheme(R.style.TemaOscuro);
		}
	}
}
