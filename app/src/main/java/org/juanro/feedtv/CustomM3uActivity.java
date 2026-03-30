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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
 * Actividad para cargar listas M3U/M3U8 personalizadas
 */
public class CustomM3uActivity extends AppCompatActivity implements M3UParser.ResponseServerCallback
{
	private RadiosAdapter mAdapter;
	private RecyclerView lista;
	private SwipeRefreshLayout swipe;
	private EditText etUrl;
	private Button btnLoad;
	private SharedPreferences sharedPref;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		aplicarTema();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_custom_m3u);

		etUrl = findViewById(R.id.et_url);
		btnLoad = findViewById(R.id.btn_load);
		swipe = findViewById(R.id.swiperefresh);
		lista = findViewById(R.id.lista);

		lista.setLayoutManager(new LinearLayoutManager(this));
		lista.setItemAnimator(new DefaultItemAnimator());
		lista.setHasFixedSize(true);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		btnLoad.setOnClickListener(v -> cargarM3u());

		swipe.setOnRefreshListener(this::cargarM3u);
	}

	private void cargarM3u()
	{
		String url = etUrl.getText().toString().trim();
		if (url.isEmpty())
		{
			Toast.makeText(this, "Introduce una URL", Toast.LENGTH_SHORT).show();
			swipe.setRefreshing(false);
			return;
		}

		swipe.setRefreshing(true);
		new Thread(() -> M3UParser.getInstance().loadRadios(true, url, CustomM3uActivity.this, CustomM3uActivity.this)).start();
	}

	@Override
	public void onChannelsLoadServer(List<M3uEntry> entradasM3u)
	{
		runOnUiThread(() -> {
			mAdapter = new RadiosAdapter(getApplicationContext(), entradasM3u);
			lista.setAdapter(mAdapter);
			mAdapter.notifyDataSetChanged();
			swipe.setRefreshing(false);
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		if (getApplicationContext() != null)
		{
			Drawable drawable = DrawableCompat.wrap(searchItem.getIcon());
			DrawableCompat.setTint(drawable, ContextCompat.getColor(getApplicationContext(), android.R.color.white));
			searchItem.setIcon(drawable);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				onBackPressed();
				return true;
			case R.id.action_search:
				SearchView searchView = (SearchView) item.getActionView();
				searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
				{
					@Override
					public boolean onQueryTextSubmit(String texto) { return false; }
					@Override
					public boolean onQueryTextChange(String texto)
					{
						if (mAdapter != null) mAdapter.getFilter().filter(texto);
						return false;
					}
				});
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

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
