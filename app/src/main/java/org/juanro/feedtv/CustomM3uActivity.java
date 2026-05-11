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

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.WindowCompat;
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
		super.onCreate(savedInstanceState);

		// Habilitar Edge-to-Edge
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

		binding = ActivityCustomM3uBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.lista.setLayoutManager(new LinearLayoutManager(this));
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

		binding.btnLoad.setOnClickListener(v -> cargarM3u());

		binding.swiperefresh.setOnRefreshListener(this::cargarM3u);
	}

	private void cargarM3u()
	{
		final String url = binding.etUrl.getText() != null ? binding.etUrl.getText().toString().trim() : "";

		if (url.isEmpty())
		{
			Toast.makeText(this, getString(R.string.enter_url), Toast.LENGTH_SHORT).show();
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
	 * Acciones botones menú
	 *
	 * @param item El elemento de menú que fue seleccionado.
	 * @return boolean Devuelve false para permitir que continúe el procesamiento normal del menú, true para consumirlo aquí.
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		return super.onOptionsItemSelected(item);
	}
}
