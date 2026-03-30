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
import android.database.Cursor;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.juanro.feedtv.Adapters.ArticleAdapter;
import org.juanro.feedtv.BBDD.FeedDatabase;
import org.juanro.feedtv.BBDD.RssList;
import org.juanro.feedtv.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity
{
	private ActivityMainBinding binding;
	private ArticleAdapter mAdapter;
	private MainViewModel viewModel;
	private ArrayAdapter<String> adapter;
	private String elemento;

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

		// Cargar Fuentes de noticias guardadas
		adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_white, cargarFuentes());
		binding.listaFeeds.setAdapter(adapter);

		// Obtener pulsación lista fuentes
		binding.listaFeeds.setOnItemClickListener((parent, view, pos, id) -> onFeedsItemClick(parent, pos));

		// Acciones pulsación larga lista de fuentes
		binding.listaFeeds.setOnItemLongClickListener((parent, view, pos, id) ->
		{
			onFeedsItemLongClick(parent, pos);
			return true;
		});

		// Obtener objeto que contiene el Feed
		viewModel = new ViewModelProvider(this).get(MainViewModel.class);

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
			if (articles != null)
			{
				// Asociar adapter con RecyclerView
				mAdapter = new ArticleAdapter(articles, MainActivity.this);
				binding.contentMain.listaNoticias.setAdapter(mAdapter);
			}
			else
			{
				Toast.makeText(getApplicationContext(), getString(R.string.no_feeds), Toast.LENGTH_LONG).show();
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
				// Limpiar lista de artículos
				binding.contentMain.swipeLayout.setRefreshing(true);

				// Obtener Feed
				viewModel.fetchFeed(getApplicationContext());
			}
			else
			{
				Toast.makeText(getApplicationContext(), getString(R.string.no_connection), Toast.LENGTH_LONG).show();
				binding.contentMain.swipeLayout.setRefreshing(false);
			}
		});

		if (isNetworkAvailable())
		{
			// Cargar artículos al inicio
			if(adapter.isEmpty())
			{
				// Si no el listado de RSS está vacio, se consulta si se quiere añadir el rss de noticias recientes
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setMessage(getString(R.string.recentNews));
				builder.setTitle(getString(R.string.title_recentNews));
				builder.setCancelable(true);
				builder.setIcon(android.R.drawable.ic_dialog_alert);

				// Añadir
				builder.setPositiveButton(getString(R.string.add), (dialog, id) ->
				{
					try (RssList fuentes = new RssList(MainActivity.this))
					{
						String url = "https://www.meneame.net/rss?status=all";
						fuentes.insertarEntrada("News", url);
						FeedDatabase.getInstance(getApplicationContext()).crearTabla("News_");

						// Recargamos las fuentes
						adapter.add("News");
					}

					adapter.notifyDataSetChanged();
					binding.listaFeeds.performItemClick(binding.listaFeeds.getSelectedView(),0, 0);

					Toast.makeText(this, this.getString(R.string.add_feed_success), Toast.LENGTH_LONG).show();
				});

				// No añadir
				builder.setNegativeButton(getString(R.string.notAdd), (dialog, id) ->
				{
					Toast.makeText(this, getString(R.string.first_start), Toast.LENGTH_LONG).show();
					binding.drawerlayout.openDrawer(GravityCompat.START);
				});

				AlertDialog alert = builder.create();
				alert.show();
			}
			else
			{
				binding.listaFeeds.performItemClick(binding.listaFeeds.getSelectedView(),0, 0);
			}
		}
		else
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.alert_message)
					.setTitle(R.string.alert_title)
					.setCancelable(false)
					.setPositiveButton(R.string.alert_positive,
							(dialog, id) -> finish());

			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	public void onResume()
	{
		super.onResume();
		adapter.clear();
		adapter.addAll(cargarFuentes());
		adapter.notifyDataSetChanged();
	}

	/**
	 * Selecciona el feed correspondiente pulsado en la lista de feeds de la barra lateral
	 *
	 * @param parent Adaptador de la vista
	 * @param pos Posición pulsada
	 */
	private void onFeedsItemClick(AdapterView<?> parent, int pos)
	{
		binding.contentMain.swipeLayout.setRefreshing(true);

		new Thread(() ->
		{
			String fuente = parent.getItemAtPosition(pos).toString();
			try (RssList listaRss = new RssList(getApplicationContext()))
			{
				try (Cursor c = listaRss.obtenerEntradas())
				{
					if (c.moveToFirst())
					{
						do
						{
							if (c.getString(1).equals(fuente))
							{
								String url = c.getString(2);
								viewModel.setUrl(url);
								FeedDatabase.getInstance(MainActivity.this).setTabla(fuente);
								viewModel.fetchFeed(getApplicationContext());
								break;
							}
						} while (c.moveToNext());
					}
				}
			}
		}).start();

		binding.drawerlayout.closeDrawers();
	}

	/**
	 * Acciones al pulsar de forma prolongada un feed en la lista de feeds de la barra lateral
	 *
	 * @param parent Adaptador de la vista
	 * @param pos Posición pulsada
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
		builder.setPositiveButton(getString(R.string.delete), (dialog, id) ->
		{
			try (RssList fuente = new RssList(MainActivity.this))
			{
				// Eliminar fuente de la base de datos y de la lista
				fuente.eliminarEntradas(elemento);
				FeedDatabase.getInstance(MainActivity.this).eliminarTabla(elemento);
				adapter.remove(elemento);
			}

			adapter.notifyDataSetChanged();
			Toast.makeText(MainActivity.this, getString(R.string.delete_feed_success), Toast.LENGTH_LONG).show();
		});

		// Editar
		builder.setNegativeButton(getString(R.string.edit), (dialog, id) ->
		{
			String url = "";
			try (RssList fuente = new RssList(MainActivity.this))
			{
				try (Cursor c = fuente.obtenerEntradas())
				{
					if (c.moveToFirst())
					{
						do
						{
							if (c.getString(1).equals(elemento))
							{
								url = c.getString(2);
								break;
							}
						} while (c.moveToNext());
					}
				}
			}
			Intent i = new Intent(this, AddFeed.class);
			i.putExtra("titulo", elemento);
			i.putExtra("url", url);
			startActivity(i);
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Inicia el menú correspondiente pulsado en la barra lateral
	 *
	 * @param item ID del elemento de menú seleccionado
	 */
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

	/**
	 * Muestra el diálogo de información de la aplicación
	 */
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

		// Permitir que los enlaces en el HTML sean clickeables
		TextView messageView = alertDialog.findViewById(android.R.id.message);
		if (messageView != null)
		{
			messageView.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	/**
	 * Comprueba si existe conexión a internet
	 *
	 * @return boolean True si hay conexión
	 */
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

	/**
	 * Acción al pulsar botones de menu
	 *
	 * @param item El elemento seleccionado
	 * @return boolean True si se maneja la pulsación
	 */
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

	/**
	 * Carga la lista de fuentes de noticias
	 *
	 * @return ArrayList con los nombres de las fuentes
	 */
	public ArrayList<String> cargarFuentes()
	{
		ArrayList <String> fuentes = new ArrayList<>();
		try (RssList rss = new RssList(this))
		{
			try (Cursor c = rss.obtenerEntradas())
			{
				if (c.moveToFirst())
				{
					do
					{
						fuentes.add(c.getString(1));
					} while (c.moveToNext());
				}
			}
		}

		return fuentes;
	}

	/**
	 * Método que aplica el tema de la aplicación
	 */
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
