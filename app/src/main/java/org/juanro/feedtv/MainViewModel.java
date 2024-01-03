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
import com.prof18.rssparser.model.RssItem;
import com.prof18.rssparser.model.RssChannel;
import com.prof18.rssparser.RssParserBuilder;
import com.prof18.rssparser.RssParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.NotNull;
import org.juanro.feedtv.BBDD.FeedDatabase;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;

/**
 * Clase que se encarga de gestionar el parseo de los feeds de noticias
 */
public class MainViewModel extends ViewModel
{
	// Crear lista de artículos (se usa LiveData porque evita memory leaks y mas actualizable)
	private MutableLiveData<List<RssItem>> articleListLive = null;

	// URL del Feed
	private String urlString = "";

	// Crear snackbar (similar a toast)
	private MutableLiveData<String> snackbar = new MutableLiveData<>();

	/**
	 * Obtener la lista de artículos
	 *
	 * @return
	 */
	public MutableLiveData<List<RssItem>> getArticleList()
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
	private void setArticleList(List<RssItem> articleList)
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
		RssParser parser = new RssParserBuilder()
				//.charset(Charset.defaultCharset())
				//.cacheExpirationMillis() and .context() not called because on Java side, caching is NOT supported
				.build();

		// Mapeado rápido de indices
		final int COLUMN_TITULO = 1;
		final int COLUMN_FEC = 2;
		final int COLUMN_URL = 3;
		final int COLUMN_IMG = 4;

		// Obtener el feed
		RssChannel channel = null;
		CompletableFuture<RssChannel> suspendResult = new CompletableFuture<>();
		parser.getRssChannel(urlString, new CustomContinuation<>(suspendResult));

		try
		{
			channel = suspendResult.get();

			// Acciones a realizar al terminar cuando el parseo del RSS es correcto
			List<RssItem> list = channel.getItems();

			// Caching
			FeedDatabase.getInstance(context.getApplicationContext()).
					sincronizarEntradas(list);

			// Obtener entradas de la base de datos
			Cursor c = FeedDatabase.getInstance(context.getApplicationContext()).obtenerEntradas();
			list.clear();

			c.moveToFirst();

			do
			{
				String title = c.getString(COLUMN_TITULO);
				String link = c.getString(COLUMN_URL);
				String pubDate = c.getString(COLUMN_FEC);
				String image = "";
				List<String> categories = new ArrayList<String>();

				//Mostrar la imagen del feed si el articulo no tiene imagen
				if(c.getString(COLUMN_IMG) == null && channel.getImage() != null)
				{
					image = channel.getImage().getUrl();
				}
				else
				{
					image = c.getString(COLUMN_IMG);
				}

				RssItem articulo = new RssItem("", title, "", link, pubDate, "", "", image, "", "", "", "", categories, null, "");

				list.add(articulo);
			} while(c.moveToNext());

			// Añadir artículos obtenidos a la lista
			setArticleList(list);

			snackbar.postValue(context.getString(R.string.update_feed_success));
		}
		catch (Exception e)
		{
			// Cosas a hacer cuando hay error en el parseo del RSS
			setArticleList(new ArrayList<RssItem>());
			e.printStackTrace();
			snackbar.postValue(context.getString(R.string.update_feed_failed) + e.getMessage());
		}
	}

	/**
	 * Función que se encarga de obtener tados de funciones suspendidas de kotlin
	 *
	 * @param <RssChannel>
	 */
	public static class CustomContinuation<RssChannel> implements Continuation<RssChannel>
	{
		private final CompletableFuture<RssChannel> future;

		public CustomContinuation(CompletableFuture<RssChannel> future)
		{
			this.future = future;
		}

		@Override
		public void resumeWith(@NotNull Object o)
		{
			future.complete((RssChannel) o);
		}

		@NonNull
		@Override
		public CoroutineContext getContext()
		{
			return Dispatchers.getMain();
		}
	}
}
