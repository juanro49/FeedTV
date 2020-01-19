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
import android.database.Cursor;

import com.prof.rssparser.Article;
import com.prof.rssparser.OnTaskCompleted;
import com.prof.rssparser.Parser;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.juanro.feedtv.BBDD.FeedDatabase;

/**
 * Clase que se encarga de gestionar el parseo de los feeds de noticias
 */
public class MainViewModel extends ViewModel
{
	// Crear lista de artículos (se usa LiveData porque evita memory leaks y mas actualizable)
	private MutableLiveData<List<Article>> articleListLive = null;

	// URL del Feed
	private String urlString = "";

	// Crear snackbar (similar a toast)
	private MutableLiveData<String> snackbar = new MutableLiveData<>();

	/**
	 * Obtener la lista de artículos
	 *
	 * @return
	 */
	public MutableLiveData<List<Article>> getArticleList()
	{
		if (articleListLive == null)
		{
			articleListLive = new MutableLiveData<>();
		}

		return articleListLive;
	}


	/**
	 * Añadir artículos a la lista
	 *
	 * @param articleList
	 */
	private void setArticleList(List<Article> articleList)
	{
		this.articleListLive.postValue(articleList);
	}

	/**
	 * Establecer url del feed
	 *
	 * @param url
	 */
	public void setUrl(String url)
	{
		this.urlString = url;
	}

	/**
	 * Obtiene la snackbar
	 *
	 * @return
	 */
	public LiveData<String> getSnackbar()
	{
		return snackbar;
	}

	/**
	 * Reinicia el valor de la snackbar una vez mostrada
	 */
	public void onSnackbarShowed()
	{
		snackbar.setValue(null);
	}

	/**
	 * Obtiene el Feed de noticias
	 *
	 * @param context
	 */
	public void fetchFeed(final Context context)
	{
		// Crear el parseador del RSS
		Parser parser = new Parser();

		// Mapeado rápido de indices
		final int COLUMN_TITULO = 1;
		final int COLUMN_FEC = 2;
		final int COLUMN_URL = 3;
		final int COLUMN_IMG = 4;

		// Obtener el feed
		parser.execute(urlString);

		// Acciones a realizar al terminar
		parser.onFinish(new OnTaskCompleted()
		{
			// Cosas a hacer cuando el parseo del RSS es correcto
			@Override
			public void onTaskCompleted(List<Article> list)
			{
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						// Caching
						FeedDatabase.getInstance(context.getApplicationContext()).
								sincronizarEntradas(list);

						// Obtener entradas de la base de datos
						Cursor c = FeedDatabase.getInstance(context.getApplicationContext()).obtenerEntradas();
						list.clear();

						c.moveToFirst();

						do
						{
							Article articulo = new Article();
							articulo.setTitle(c.getString(COLUMN_TITULO));
							articulo.setLink(c.getString(COLUMN_URL));
							articulo.setPubDate(c.getString(COLUMN_FEC));
							articulo.setImage(c.getString(COLUMN_IMG));

							list.add(articulo);
						} while(c.moveToNext());

						// Añadir artículos obtenidos a la lista
						setArticleList(list);

						snackbar.postValue(context.getString(R.string.update_feed_success));
					}
				}).start();
			}

			// Cosas a hacer cuando hay error en el parseo del RSS
			@Override
			public void onError(Exception e)
			{
				setArticleList(new ArrayList<Article>());
				e.printStackTrace();
				snackbar.postValue(context.getString(R.string.update_feed_success) + e.getMessage());
			}
		});
	}
}
