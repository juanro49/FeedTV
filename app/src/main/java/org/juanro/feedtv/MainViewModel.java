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
import android.util.Log;

import com.prof18.rssparser.model.RssItem;
import com.prof18.rssparser.model.RssChannel;
import com.prof18.rssparser.RssParserBuilder;
import com.prof18.rssparser.RssParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private static final String TAG = "MainViewModel";
	private final MutableLiveData<List<RssItem>> articleListLive = new MutableLiveData<>(new ArrayList<>());
	private String urlString = "";
	private final MutableLiveData<String> snackbar = new MutableLiveData<>();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public LiveData<List<RssItem>> getArticleList()
	{
		return articleListLive;
	}

	public void setUrl(String url)
	{
		this.urlString = url;
	}

	public LiveData<String> getSnackbar()
	{
		return snackbar;
	}

	public void onSnackbarShowed()
	{
		snackbar.setValue(null);
	}

	public void fetchFeed(final Context context)
	{
		if (urlString == null || urlString.isEmpty()) return;

		RssParser parser = new RssParserBuilder().build();
		CompletableFuture<RssChannel> future = new CompletableFuture<>();
		
		// Iniciar petición asíncrona de RSS
		parser.getRssChannel(urlString, new CustomContinuation<>(future));

		future.thenAcceptAsync(channel -> {
			try {
				if (channel == null) throw new Exception("Channel is null");

				List<RssItem> items = channel.getItems();
				FeedDatabase db = FeedDatabase.getInstance(context);

				// Sincronizar con DB en segundo plano
				db.sincronizarEntradas(items);

				// Obtener entradas finales de la DB
				try (Cursor c = db.obtenerEntradas()) {
					List<RssItem> listFromDb = new ArrayList<>();

					if (c.moveToFirst()) {
						final int colTitulo = 1, colFec = 2, colUrl = 3, colImg = 4;
						do {
							String image = c.getString(colImg);
							if (image == null && channel.getImage() != null) {
								image = channel.getImage().getUrl();
							}

							listFromDb.add(new RssItem("", c.getString(colTitulo), "", c.getString(colUrl), 
									c.getString(colFec), "", "", image, "", "", "", "", 
									new ArrayList<>(), null, "", null, null, null));
						} while (c.moveToNext());
					}
					articleListLive.postValue(listFromDb);
				}
				
				snackbar.postValue(context.getString(R.string.update_feed_success));

			} catch (Exception e) {
				handleError(context, e);
			}
		}, executor).exceptionally(ex -> {
			handleError(context, ex);
			return null;
		});
	}

	private void handleError(Context context, Throwable e) {
		Log.e(TAG, "Error fetching feed", e);
		articleListLive.postValue(new ArrayList<>());
		snackbar.postValue(context.getString(R.string.update_feed_failed) + e.getMessage());
	}

	@Override
	protected void onCleared() {
		executor.shutdown();
	}

	/**
	 * Clase para manejar la continuación de corrutinas desde Java
	 */
	public static class CustomContinuation<T> implements Continuation<T> {
		private final CompletableFuture<T> future;

		public CustomContinuation(CompletableFuture<T> future) {
			this.future = future;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void resumeWith(@NotNull Object o) {
			if (o instanceof Throwable) {
				future.completeExceptionally((Throwable) o);
			} else {
				try {
					// Casting genérico para interoperar con Kotlin
					future.complete((T) o);
				} catch (ClassCastException e) {
					future.completeExceptionally(e);
				}
			}
		}

		@NonNull
		@Override
		public CoroutineContext getContext() {
			return Dispatchers.getIO();
		}
	}
}
