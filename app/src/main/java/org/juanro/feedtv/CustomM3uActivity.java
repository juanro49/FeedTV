/*
 *   Copyright 2026 Juanro49
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
 */

package org.juanro.feedtv;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import net.bjoernpetersen.m3u.model.M3uEntry;

import org.juanro.feedtv.Adapters.RadiosAdapter;
import org.juanro.feedtv.Radio.M3UParser;
import org.juanro.feedtv.databinding.ActivityCustomM3uBinding;

import java.util.List;

/**
 * Actividad para cargar listas M3U/M3U8 personalizadas
 */
public class CustomM3uActivity extends AppCompatActivity implements M3UParser.ResponseServerCallback
{
	private ActivityCustomM3uBinding binding;
	private RadiosAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		aplicarTema();
		super.onCreate(savedInstanceState);
		binding = ActivityCustomM3uBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.lista.setLayoutManager(new LinearLayoutManager(this));
		binding.lista.setItemAnimator(new DefaultItemAnimator());
		binding.lista.setHasFixedSize(true);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		binding.btnLoad.setOnClickListener(v -> cargarM3u());

		binding.swiperefresh.setOnRefreshListener(this::cargarM3u);
	}

	private void cargarM3u()
	{
		String url = binding.etUrl.getText().toString().trim();
		if (url.isEmpty())
		{
			Toast.makeText(this, "Introduce una URL", Toast.LENGTH_SHORT).show();
			binding.swiperefresh.setRefreshing(false);
			return;
		}

		binding.swiperefresh.setRefreshing(true);
		new Thread(() -> M3UParser.getInstance().loadRadios(true, url, CustomM3uActivity.this, CustomM3uActivity.this)).start();
	}

	/**
	 * Crea la lista con las radios obtenidas
	 *
	 * @param entradasM3u Lista de entradas M3U
	 */
	@Override
	public void onChannelsLoadServer(List<M3uEntry> entradasM3u)
	{
		runOnUiThread(() -> {
			mAdapter = new RadiosAdapter(getApplicationContext(), entradasM3u);
			binding.lista.setAdapter(mAdapter);
			binding.swiperefresh.setRefreshing(false);
		});
	}

	/**
	 * Crear menu
	 *
	 * @param menu El menú de opciones en el que se colocan los elementos.
	 * @return boolean Debe devolver true para que se muestre el menú; si devuelve false, no se mostrará.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
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
	 * @return boolean Devuelve false para permitir que continúe el procesamiento normal del menú, true para consumirlo aquí.
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
