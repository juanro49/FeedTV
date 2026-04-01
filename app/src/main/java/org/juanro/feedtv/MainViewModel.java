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
import org.juanro.feedtv.BBDD.AppDatabase;
import org.juanro.feedtv.BBDD.Article;
import org.juanro.feedtv.BBDD.RssFeed;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;

/**
 * Clase que se encarga de gestionar el parseo de los feeds de noticias
 */
public class MainViewModel extends ViewModel
{
	private static final String TAG = "MainViewModel";
	private final MutableLiveData<List<Article>> articleListLive = new MutableLiveData<>(new ArrayList<>());
	private String urlString = "";
	private String feedName = "";
	private final MutableLiveData<String> snackbar = new MutableLiveData<>();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public LiveData<List<Article>> getArticleList()
	{
		return articleListLive;
	}

	public void setUrl(String url)
	{
		this.urlString = url;
	}

	public void setFeedName(String name)
	{
		this.feedName = name;
	}

	public LiveData<String> getSnackbar()
	{
		return snackbar;
	}

	public void onSnackbarShowed()
	{
		snackbar.setValue(null);
	}

	/**
	 * Obtiene los 20 artículos más recientes de todas las fuentes
	 */
	public void fetchGlobalRecentArticles(final Context context) {
		executor.execute(() -> {
			AppDatabase db = AppDatabase.getInstance(context);
			List<Article> recentArticles = db.articleDao().getGlobalRecentArticles();
			articleListLive.postValue(recentArticles);
		});
	}

	public void fetchFeed(final Context context)
	{
		if (urlString == null || urlString.isEmpty()) {
			snackbar.postValue(context.getString(R.string.first_start));
			return;
		}

		RssParser parser = new RssParserBuilder().build();
		CompletableFuture<RssChannel> future = new CompletableFuture<>();
		
		parser.getRssChannel(urlString, new CustomContinuation<>(future));

		future.thenAcceptAsync(channel -> {
			try {
				if (channel == null) throw new Exception("Channel is null");

				List<RssItem> items = channel.getItems();
				AppDatabase db = AppDatabase.getInstance(context);

				RssFeed feed = db.feedDao().findByTitle(feedName);
				if (feed == null) {
					feed = new RssFeed(feedName, urlString);
					db.feedDao().insert(feed);
					feed = db.feedDao().findByTitle(feedName);
				}
				final int feedId = feed.getId();

				List<Article> articlesToSave = new ArrayList<>();
				for (RssItem item : items) {
					long numFecha = parsePubDate(item.getPubDate());
					
					String image = item.getImage();
					if (image == null && channel.getImage() != null) {
						image = channel.getImage().getUrl();
					}
					
					articlesToSave.add(new Article(
							feedId,
							item.getGuid(),
							item.getTitle(),
							item.getAuthor(),
							item.getLink(),
							item.getPubDate(),
							item.getDescription(),
							item.getContent(),
							image,
							item.getAudio(),
							item.getVideo(),
							item.getSourceName(),
							item.getSourceUrl(),
							item.getCommentsUrl(),
							item.getCategories(),
							numFecha
					));
				}
				
				db.articleDao().insertAll(articlesToSave);
				db.articleDao().deleteOldArticles(feedId);

				List<Article> listFromDb = db.articleDao().getArticlesByFeed(feedId);
				articleListLive.postValue(listFromDb);
				
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

	private long parsePubDate(String pubDate) {
		if (pubDate == null) return 0;
		try {
			java.text.SimpleDateFormat sourceRSS = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH);
			java.text.SimpleDateFormat sourceAtom = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ENGLISH);
			java.util.Date date;
			if (pubDate.startsWith("2")) {
				if (pubDate.length() > 19) pubDate = pubDate.substring(0, 19);
				date = sourceAtom.parse(pubDate);
			} else {
				date = sourceRSS.parse(pubDate);
			}
			if (date != null) {
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmm", java.util.Locale.getDefault());
				return Long.parseLong(sdf.format(date));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error parsing date", e);
		}
		return 0;
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
