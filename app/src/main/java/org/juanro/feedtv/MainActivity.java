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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final List<RssFeed> currentFeeds = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Habilitar Edge-to-Edge (Material 3 Expressive)
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		// Ajustar insets solo para el menú lateral (evitar solapamiento con la barra de navegación)
		ViewCompat.setOnApplyWindowInsetsListener(binding.navview, (v, windowInsets) -> {
			androidx.core.graphics.Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(0, 0, 0, systemBars.bottom);
			return windowInsets;
		});

		// Boton para abrir barra lateral
		binding.toolbar.setNavigationOnClickListener(v -> binding.drawerlayout.openDrawer(GravityCompat.START));

		// Obtener objeto que contiene el Feed
		viewModel = new ViewModelProvider(this).get(MainViewModel.class);

		// Cargar Fuentes desde Room y poblar artículos iniciales
		refreshFeeds();

		// Setear la lista en la que se mostrarán los artículos
		binding.contentMain.listaNoticias.setLayoutManager(new LinearLayoutManager(this));
		binding.contentMain.listaNoticias.setItemAnimator(new DefaultItemAnimator());
		binding.contentMain.listaNoticias.setHasFixedSize(true);

		// Obtener pulsación barra lateral
		binding.navview.setNavigationItemSelectedListener(menuItem ->
		{
			onItemSelected(menuItem);
			return true;
		});

		// Obtener artículos y mostrarlos
		viewModel.getArticleList().observe(this, articles ->
		{
			if (articles != null)
			{
				// Asociar adapter con RecyclerView
				mAdapter = new ArticleAdapter(articles, MainActivity.this);
				binding.contentMain.listaNoticias.setAdapter(mAdapter);
			}
		});

		// Observar estado de carga (Material 3 Expressive feedback)
		viewModel.getIsLoading().observe(this, loading -> binding.contentMain.swipeLayout.setRefreshing(loading));

		// Obtener mensajes de la snackbar
		viewModel.getSnackbar().observe(this, s ->
		{
			if (s != null)
			{
				Snackbar.make(binding.getRoot(), s, Snackbar.LENGTH_LONG).show();
				viewModel.onSnackbarShowed();
			}
		});

		// Setear Swipe Layout
		binding.contentMain.swipeLayout.setOnRefreshListener(() ->
		{
			if(isNetworkAvailable())
			{
				// Intentar obtener Feed (El ViewModel manejará si no hay URL seleccionada)
				viewModel.fetchFeed();
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
	 * Actualiza la lista de feeds desde la base de datos Room y los añade al NavigationView
	 */
	@SuppressLint("RestrictedApi")
	private void refreshFeeds()
	{
		executor.execute(() -> {
			AppDatabase db = AppDatabase.getInstance(MainActivity.this);
			List<RssFeed> feeds = db.feedDao().getAll();

			runOnUiThread(() -> {
				currentFeeds.clear();
				currentFeeds.addAll(feeds);

				Menu menu = binding.navview.getMenu();
				MenuItem feedsRoot = menu.findItem(R.id.menu_feeds_root);
				if (feedsRoot != null && feedsRoot.hasSubMenu()) {
					SubMenu subMenu = feedsRoot.getSubMenu();
					// Limpiar solo los feeds dinámicos (asumimos que están en el grupo específico)
					if (subMenu != null) {
						subMenu.removeGroup(R.id.group_feeds);

						for (int i = 0; i < feeds.size(); i++) {
							RssFeed f = feeds.get(i);
							MenuItem item = subMenu.add(R.id.group_feeds, Menu.NONE, i, f.getTitle());
							item.setIcon(R.drawable.ic_rss_feed);
						}
					}
				}

				// Cargar los 20 artículos más recientes globales al inicio si no hay nada seleccionado
				if (viewModel.getArticleList().getValue() == null || viewModel.getArticleList().getValue().isEmpty()) {
					viewModel.fetchGlobalRecentArticles();
				}

				// Si está vacía, ofrecer añadir feed por defecto
				if (feeds.isEmpty() && isNetworkAvailable()) {
					showDefaultFeedDialog();
				}
			});
		});
	}

	private void showDefaultFeedDialog()
	{
		new MaterialAlertDialogBuilder(MainActivity.this)
				.setMessage(getString(R.string.recentNews))
				.setTitle(getString(R.string.title_recentNews))
				.setCancelable(true)
				.setIcon(R.drawable.ic_alert_m3)
				.setPositiveButton(getString(R.string.add), (dialog, id) -> {
					executor.execute(() -> {
						AppDatabase db = AppDatabase.getInstance(MainActivity.this);
						db.feedDao().insert(new RssFeed("News", "https://www.meneame.net/rss?status=all"));
						refreshFeeds();
					});
					Toast.makeText(this, getString(R.string.add_feed_success), Toast.LENGTH_LONG).show();
				})
				.setNegativeButton(getString(R.string.notAdd), (dialog, id) -> {
					Toast.makeText(this, getString(R.string.first_start), Toast.LENGTH_LONG).show();
					binding.drawerlayout.openDrawer(GravityCompat.START);
				})
				.show();
	}

	private void onItemSelected(MenuItem menuItem)
	{
		int id = menuItem.getItemId();

		if (menuItem.getGroupId() == R.id.group_feeds) {
			// Es un feed dinámico
			final String title = menuItem.getTitle() != null ? menuItem.getTitle().toString() : "";

			executor.execute(() -> {
				AppDatabase db = AppDatabase.getInstance(getApplicationContext());
				RssFeed feed = db.feedDao().findByTitle(title);
				if (feed != null) {
					viewModel.setUrl(feed.getUrl());
					viewModel.setFeedName(feed.getTitle());
					viewModel.fetchFeed();
				}
			});
		} else if (id == R.id.nav_home) {
			if (currentFeeds.isEmpty()) {
				showDefaultFeedDialog();
			} else {
				viewModel.fetchGlobalRecentArticles();
			}
		} else if (id == R.id.about) {
			showAboutDialog();
		} else if (id == R.id.manage_feeds) {
			showManageFeedsDialog();
		} else {
			Class<?> activityClass = null;

			if (id == R.id.addFeed) activityClass = AddFeed.class;
			else if (id == R.id.tv) activityClass = TvActivity.class;
			else if (id == R.id.radio) activityClass = RadioActivity.class;
			else if (id == R.id.custom_m3u) activityClass = CustomM3uActivity.class;
			else if (id == R.id.ajustes) activityClass = SettingsActivity.class;

			if (activityClass != null) {
				startActivity(new Intent(this, activityClass));
			}
		}

		binding.drawerlayout.closeDrawers();
	}

	private void showManageFeedsDialog() {
		if (currentFeeds.isEmpty()) {
			Toast.makeText(this, R.string.no_feeds, Toast.LENGTH_SHORT).show();
			return;
		}

		String[] titles = new String[currentFeeds.size()];
		for (int i = 0; i < currentFeeds.size(); i++) {
			titles[i] = currentFeeds.get(i).getTitle();
		}

		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.title_dialog_rss_list)
				.setItems(titles, (dialog, which) -> {
					String selectedTitle = titles[which];
					showEditDeleteOptions(selectedTitle);
				})
				.show();
	}

	private void showEditDeleteOptions(String title) {
		String[] options = {getString(R.string.edit), getString(R.string.delete)};
		new MaterialAlertDialogBuilder(this)
				.setTitle(title)
				.setItems(options, (dialog, which) -> {
					if (which == 0) {
						// Editar
						executor.execute(() -> {
							AppDatabase db = AppDatabase.getInstance(getApplicationContext());
							RssFeed feed = db.feedDao().findByTitle(title);
							if (feed != null) {
								Intent intent = new Intent(MainActivity.this, AddFeed.class);
								intent.putExtra("titulo", feed.getTitle());
								intent.putExtra("url", feed.getUrl());
								startActivity(intent);
							}
						});
					} else if (which == 1) {
						// Eliminar
						showDeleteFeedDialog(title);
					}
				})
				.show();
	}

	private void showDeleteFeedDialog(String title) {
		new MaterialAlertDialogBuilder(this)
				.setTitle(R.string.delete)
				.setMessage(getString(R.string.delete) + ": " + title + "?")
				.setPositiveButton(R.string.delete, (dialog, which) -> {
					executor.execute(() -> {
						AppDatabase db = AppDatabase.getInstance(getApplicationContext());
						db.feedDao().deleteByTitle(title);
						refreshFeeds();
					});
					Toast.makeText(this, R.string.delete_feed_success, Toast.LENGTH_SHORT).show();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showAboutDialog()
	{
		String aboutMessage = getString(R.string.about) +
				getString(R.string.agradecimientos) +
				getString(R.string.author) +
				getString(R.string.version) + " " + BuildConfig.VERSION_NAME +
				"<br/><br/>" + getString(R.string.github);

		AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
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
		return super.onOptionsItemSelected(item);
	}


}
