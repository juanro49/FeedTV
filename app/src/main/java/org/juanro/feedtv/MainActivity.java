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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.prof.rssparser.Article;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.juanro.feedtv.BBDD.FeedDatabase;
import org.juanro.feedtv.BBDD.RssList;


public class MainActivity extends AppCompatActivity
{
    private RecyclerView mRecyclerView;
    private static ListView listaFeeds;
    private static ArticleAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private MainViewModel viewModel;
    private FrameLayout frameLayout;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
	private ArrayAdapter<String> adapter;
	private String elemento;
	private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setear barra lateral
        drawerLayout = findViewById(R.id.drawerlayout);
        navView = findViewById(R.id.navview);
        listaFeeds = findViewById(R.id.listaFeeds);
		// Boton para abrir barra lateral
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_menu);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Cargar Fuentes de noticias guardadas
		adapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_white, cargarFuentes());
		listaFeeds.setAdapter(adapter);

		// Obtener pulsación lista fuentes
		listaFeeds.setOnItemClickListener((parent, view, pos, id) -> onFeedsItemClick(parent, pos));

		// Acciones pulsación larga lista de fuentes
		listaFeeds.setOnItemLongClickListener((parent, view, pos, id) ->
		{
			onFeedsItemLongClick(parent, pos);
			return true;
		});

		// Obtener objeto que contiene el Feed
		viewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        // Setear la lista en la que se mostrarán los artículos
        mRecyclerView = findViewById(R.id.listaNoticias);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setHasFixedSize(true);

        frameLayout = findViewById(R.id.root_layout);

        // Obtener pulsación barra lateral
		navView.setNavigationItemSelectedListener(menuItem ->
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
				mRecyclerView.setAdapter(mAdapter);
				mAdapter.notifyDataSetChanged();
			}
			else
			{
				Toast.makeText(getApplicationContext(), getString(R.string.no_feeds), Toast.LENGTH_LONG).show();
			}

			mSwipeRefreshLayout.setRefreshing(false);
		});

		// Obtener mensajes de la snackbar
		viewModel.getSnackbar().observe(this, new Observer<String>()
		{
			@Override
			public void onChanged(String s)
			{
				if (s != null)
				{
					Snackbar.make(frameLayout, s, Snackbar.LENGTH_LONG).show();
					viewModel.onSnackbarShowed();
					mSwipeRefreshLayout.setRefreshing(false);
				}
			}
		});

        // Setear Swipe Layout
        mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorHeader, R.color.colorAccent);
        mSwipeRefreshLayout.canChildScrollUp();
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                if(isNetworkAvailable())
                {
                    // Limpiar lista de artículos
                    mAdapter.notifyDataSetChanged();
                    mSwipeRefreshLayout.setRefreshing(true);

                    // Obtener Feed
                    viewModel.fetchFeed(getApplicationContext());
                }
                else
                {
                    Toast.makeText(getApplicationContext(), getString(R.string.no_connection), Toast.LENGTH_LONG).show();
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        if (isNetworkAvailable())
        {
        	// Cargar artículos al inicio
			if(listaFeeds.getAdapter().isEmpty())
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
					RssList fuentes = new RssList(MainActivity.this);
					String url = "https://www.bing.com/news/search?qft=sortbydate%3d\"1\"&format=rss";
					fuentes.insertarEntrada("News", url);
					FeedDatabase.getInstance(getApplicationContext()).crearTabla("News_");

					// Recargamos las fuentes
					adapter.add("News");
					fuentes.close();

					adapter.notifyDataSetChanged();
					listaFeeds.performItemClick(listaFeeds.getSelectedView(),0, 0);

					Toast.makeText(this, this.getString(R.string.add_feed_success), Toast.LENGTH_LONG).show();
				});

				// No añadir
				builder.setNegativeButton(getString(R.string.notAdd), (dialog, id) ->
				{
					Toast.makeText(this, getString(R.string.first_start), Toast.LENGTH_LONG).show();
					drawerLayout.openDrawer(Gravity.LEFT);
				});

				AlertDialog alert = builder.create();
				alert.show();
			}
			else
			{
				listaFeeds.performItemClick(listaFeeds.getSelectedView(),0, 0);
			}

            //viewModel.fetchFeed(getApplicationContext());
        }
        else if (!isNetworkAvailable())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.alert_message)
                    .setTitle(R.string.alert_title)
                    .setCancelable(false)
                    .setPositiveButton(R.string.alert_positive,
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id)
                                {
                                    finish();
                                }
                            });

            AlertDialog alert = builder.create();
            alert.show();
        }
    }

	public void onResume()
	{
		super.onResume();

		adapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_white, cargarFuentes());
		listaFeeds.setAdapter(adapter);
	}

	/**
	 * Selecciona el feed correspondiente pulsado en la lista de feeds de la barra lateral
	 *
	 * @param parent
	 * @param pos
	 */
	private void onFeedsItemClick(AdapterView<?> parent, int pos)
	{
		mSwipeRefreshLayout.setRefreshing(true);

		new Thread(() ->
		{
			String fuente = parent.getItemAtPosition(pos).toString();
			String url;
			boolean encontrado = false;

			RssList listaRss = new RssList(getApplicationContext());
			Cursor c = listaRss.obtenerEntradas();

			c.moveToFirst();

			do
			{
				if(c.getString(1).equals(fuente))
				{
					url = c.getString(2);
					encontrado = true;
					viewModel.setUrl(url);

					FeedDatabase.getInstance(MainActivity.this).setTabla(fuente);

					// Obtener Feed
					viewModel.fetchFeed(getApplicationContext());
				}
			}while(c.moveToNext() && !encontrado);

			c.close();
			listaRss.close();
		}).start();

		drawerLayout.closeDrawers();
	}

	/**
	 * Acciones al pulsar de forma prolongada un feed en la lista de feeds de la barra lateral
	 *
	 * @param parent
	 * @param pos
	 * @return
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
			RssList fuente = new RssList(MainActivity.this);

			// Eliminar fuente de la base de datos y de la lista
			fuente.eliminarEntradas(elemento);
			FeedDatabase.getInstance(MainActivity.this).eliminarTabla(elemento);
			adapter.remove(elemento);
			fuente.close();

			adapter.notifyDataSetChanged();
			Toast.makeText(MainActivity.this, getString(R.string.delete_feed_success), Toast.LENGTH_LONG).show();
		});

		// Editar
		builder.setNegativeButton(getString(R.string.edit), (dialog, id) ->
		{
			RssList fuente = new RssList(MainActivity.this);
			Cursor c = fuente.obtenerEntradas();
			String url = "";
			boolean encontrado = false;

			c.moveToFirst();

			do
			{
				if(c.getString(1).equals(elemento))
				{
					url = c.getString(2);
					encontrado = true;
				}
			}while(c.moveToNext() && !encontrado);

			c.close();
			fuente.close();
			Intent i = new Intent(getApplicationContext(), AddFeed.class);
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
	 * @param item
	 */
	private void onItemSelected(int item)
	{
		switch (item)
		{
			case R.id.addFeed:
			{
				Intent i = new Intent(getApplicationContext(), AddFeed.class);
				startActivity(i);
				break;
			}

			case R.id.tv:
			{
				Intent i = new Intent(getApplicationContext(), TvActivity.class);
				startActivity(i);
				break;
			}

			case R.id.ajustes:
			{
				Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
				startActivity(i);
				break;
			}

			case R.id.about:
			{

				AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
				alertDialog.setTitle(R.string.action_about);
				alertDialog.setIcon(R.mipmap.ic_launcher);
				alertDialog.setMessage(Html.fromHtml( MainActivity.this.getString(R.string.about) +
						MainActivity.this.getString(R.string.agradecimientos) +
						MainActivity.this.getString(R.string.author) +
						MainActivity.this.getString(R.string.version) + " " + BuildConfig.VERSION_NAME + "<br/><br/>" + MainActivity.this.getString(R.string.github)));
				alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int which)
							{
								dialog.dismiss();
							}
						});
				alertDialog.show();

				((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

				break;
			}
		}

		drawerLayout.closeDrawers();
	}

	/**
	 * Comprueba si existe conexión a internet
	 *
	 * @return
	 */
    public boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

	/**
	 * Acción al pulsar botones de menu
	 *
	 * @param item
	 * @return
	 */
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			drawerLayout.openDrawer(GravityCompat.START);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Carga la lista de fuentes de noticias
	 *
	 * @return
	 */
	public ArrayList cargarFuentes()
	{
		RssList rss = new RssList(this);
		ArrayList <String> fuentes = new ArrayList<>();

		Cursor c = rss.obtenerEntradas();

		if(c.getCount() != 0)
		{
			c.moveToFirst();

			do
			{
				fuentes.add(c.getString(1));
			} while(c.moveToNext());
		}

		c.close();
		rss.close();

		return fuentes;
	}

	/**
	 * Método que aplica el tema de la aplicación
	 */
	private void aplicarTema()
	{
		sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

		if(sharedPref.getString("tema", "Claro").equals("Claro"))
		{
			setTheme(R.style.TemaClaro_NoActionBar);
		}
		else
		{
			setTheme(R.style.TemaOscuro_NoActionBar);
		}
	}
}