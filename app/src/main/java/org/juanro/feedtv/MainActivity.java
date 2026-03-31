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
 *   Basado en la clase original creada por Marco Gomiero para el ejemplo de RSS-Parser
 *
 */

package org.juanro.feedtv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.juanro.feedtv.Adapters.ArticleAdapter;
import org.juanro.feedtv.BBDD.AppDatabase;
import org.juanro.feedtv.BBDD.RssFeed;
import org.juanro.feedtv.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity
{
	private ActivityMainBinding binding;
	private ArticleAdapter mAdapter;
	private MainViewModel viewModel;
	private ArrayAdapter<String> adapter;
	private String elemento;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		setSupportActionBar(binding.toolbar);

		// Boton para abrir barra lateral
		if (getSupportActionBar() != null) {
			getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_menu);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		// Inicializar adaptador de la lista de feeds (vacío inicialmente)
		adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_white, new ArrayList<>());
		binding.listaFeeds.setAdapter(adapter);

		// Obtener objeto que contiene el Feed
		viewModel = new ViewModelProvider(this).get(MainViewModel.class);

		// Cargar Fuentes desde Room y poblar artículos iniciales
		refreshFeeds();

		// Obtener pulsación lista fuentes
		binding.listaFeeds.setOnItemClickListener((parent, view, pos, id) -> onFeedsItemClick(parent, pos));

		// Acciones pulsación larga lista de fuentes
		binding.listaFeeds.setOnItemLongClickListener((parent, view, pos, id) ->
		{
			onFeedsItemLongClick(parent, pos);
			return true;
		});

		// Setear la lista en la que se mostrarán los artículos
		binding.contentMain.listaNoticias.setLayoutManager(new LinearLayoutManager(this));
		binding.contentMain.listaNoticias.setItemAnimator(new DefaultItemAnimator());
		binding.contentMain.listaNoticias.setHasFixedSize(true);

		// Obtener pulsación barra lateral
		binding.navview.setNavigationItemSelectedListener(menuItem ->
		{
			onItemSelected(menuItem.getItemId());
			return true;
		});

		// Obtener artículos y mostrarlos
		viewModel.getArticleList().observe(this, articles ->
		{
			if (articles != null && !articles.isEmpty())
			{
				// Asociar adapter con RecyclerView
				mAdapter = new ArticleAdapter(articles, MainActivity.this);
				binding.contentMain.listaNoticias.setAdapter(mAdapter);
			}
			else if (articles != null)
			{
				// Si la lista está vacía pero no es nula, es el estado inicial o tras borrar todo
				mAdapter = new ArticleAdapter(new ArrayList<>(), MainActivity.this);
				binding.contentMain.listaNoticias.setAdapter(mAdapter);
			}

			binding.contentMain.swipeLayout.setRefreshing(false);
		});

		// Obtener mensajes de la snackbar
		viewModel.getSnackbar().observe(this, s ->
		{
			if (s != null)
			{
				Snackbar.make(binding.getRoot(), s, Snackbar.LENGTH_LONG).show();
				viewModel.onSnackbarShowed();
				binding.contentMain.swipeLayout.setRefreshing(false);
			}
		});

		// Setear Swipe Layout
		binding.contentMain.swipeLayout.setColorSchemeResources(R.color.colorHeader, R.color.colorAccent);
		binding.contentMain.swipeLayout.setOnRefreshListener(() ->
		{
			if(isNetworkAvailable())
			{
				// Intentar obtener Feed (El ViewModel manejará si no hay URL seleccionada)
				viewModel.fetchFeed(getApplicationContext());
			}
			else
			{
				Toast.makeText(getApplicationContext(), getString(R.string.no_connection), Toast.LENGTH_LONG).show();
				binding.contentMain.swipeLayout.setRefreshing(false);
			}
		});
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		refreshFeeds();
	}

	/**
	 * Actualiza la lista de feeds desde la base de datos Room
	 */
	private void refreshFeeds()
	{
		executor.execute(() -> {
			AppDatabase db = AppDatabase.getInstance(MainActivity.this);
			List<RssFeed> feeds = db.feedDao().getAll();
			List<String> titles = new ArrayList<>();
			for (RssFeed f : feeds) {
				titles.add(f.getTitle());
			}

			runOnUiThread(() -> {
				adapter.clear();
				adapter.addAll(titles);
				adapter.notifyDataSetChanged();

				// Cargar los 20 artículos más recientes globales al inicio
				viewModel.fetchGlobalRecentArticles(getApplicationContext());

				// Si está vacía, ofrecer añadir feed por defecto
				if (titles.isEmpty() && isNetworkAvailable()) {
					showDefaultFeedDialog();
				}
			});
		});
	}

	private void showDefaultFeedDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(getString(R.string.recentNews));
		builder.setTitle(getString(R.string.title_recentNews));
		builder.setCancelable(true);
		builder.setIcon(android.R.drawable.ic_dialog_alert);

		builder.setPositiveButton(getString(R.string.add), (dialog, id) -> {
			executor.execute(() -> {
				AppDatabase db = AppDatabase.getInstance(MainActivity.this);
				db.feedDao().insert(new RssFeed("News", "https://www.meneame.net/rss?status=all"));
				refreshFeeds();
			});
			Toast.makeText(this, getString(R.string.add_feed_success), Toast.LENGTH_LONG).show();
		});

		builder.setNegativeButton(getString(R.string.notAdd), (dialog, id) -> {
			Toast.makeText(this, getString(R.string.first_start), Toast.LENGTH_LONG).show();
			binding.drawerlayout.openDrawer(GravityCompat.START);
		});

		builder.create().show();
	}

	/**
	 * Selecciona el feed correspondiente pulsado en la lista de feeds de la barra lateral
	 */
	private void onFeedsItemClick(AdapterView<?> parent, int pos)
	{
		binding.contentMain.swipeLayout.setRefreshing(true);
		String fuente = parent.getItemAtPosition(pos).toString();

		executor.execute(() -> {
			AppDatabase db = AppDatabase.getInstance(getApplicationContext());
			RssFeed feed = db.feedDao().findByTitle(fuente);
			if (feed != null) {
				viewModel.setUrl(feed.getUrl());
				viewModel.setFeedName(feed.getTitle());
				viewModel.fetchFeed(getApplicationContext());
			}
		});

		binding.drawerlayout.closeDrawers();
	}

	/**
	 * Acciones al pulsar de forma prolongada un feed
	 */
	public void onFeedsItemLongClick(AdapterView<?> parent, int pos)
	{
		elemento = parent.getItemAtPosition(pos).toString();

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(getString(R.string.dialog_rss_list) + " " + elemento + "?");
		builder.setTitle(getString(R.string.title_dialog_rss_list));
		builder.setCancelable(true);
		builder.setIcon(android.R.drawable.ic_dialog_alert);

		// Eliminar
		builder.setPositiveButton(getString(R.string.delete), (dialog, id) -> {
			executor.execute(() -> {
				AppDatabase db = AppDatabase.getInstance(MainActivity.this);
				db.feedDao().deleteByTitle(elemento);
				refreshFeeds();
			});
			Toast.makeText(MainActivity.this, getString(R.string.delete_feed_success), Toast.LENGTH_LONG).show();
		});

		// Editar
		builder.setNegativeButton(getString(R.string.edit), (dialog, id) -> executor.execute(() -> {
			AppDatabase db = AppDatabase.getInstance(MainActivity.this);
			RssFeed feed = db.feedDao().findByTitle(elemento);
			if (feed != null) {
				runOnUiThread(() -> {
					Intent i = new Intent(MainActivity.this, AddFeed.class);
					i.putExtra("titulo", feed.getTitle());
					i.putExtra("url", feed.getUrl());
					startActivity(i);
				});
			}
		}));

		builder.create().show();
	}

	private void onItemSelected(int item)
	{
		if (item == R.id.about)
		{
			showAboutDialog();
		}
		else
		{
			Class<?> activityClass = null;

			if (item == R.id.addFeed) activityClass = AddFeed.class;
			else if (item == R.id.tv) activityClass = TvActivity.class;
			else if (item == R.id.radio) activityClass = RadioActivity.class;
			else if (item == R.id.custom_m3u) activityClass = CustomM3uActivity.class;
			else if (item == R.id.ajustes) activityClass = SettingsActivity.class;

			if (activityClass != null)
			{
				startActivity(new Intent(this, activityClass));
			}
		}

		binding.drawerlayout.closeDrawers();
	}

	private void showAboutDialog()
	{
		String aboutMessage = getString(R.string.about) +
				getString(R.string.agradecimientos) +
				getString(R.string.author) +
				getString(R.string.version) + " " + BuildConfig.VERSION_NAME +
				"<br/><br/>" + getString(R.string.github);

		AlertDialog alertDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.action_about)
				.setIcon(R.mipmap.ic_launcher)
				.setMessage(Html.fromHtml(aboutMessage, Html.FROM_HTML_MODE_LEGACY))
				.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
				.create();

		alertDialog.show();

		TextView messageView = alertDialog.findViewById(android.R.id.message);
		if (messageView != null)
		{
			messageView.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

    public boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || 
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			binding.drawerlayout.openDrawer(GravityCompat.START);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void aplicarTema()
	{
		SharedPreferences sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

		if("Claro".equals(sharedPref.getString("tema", "Claro")))
		{
			setTheme(R.style.TemaClaro_NoActionBar);
		}
		else
		{
			setTheme(R.style.TemaOscuro_NoActionBar);
		}
	}
}

