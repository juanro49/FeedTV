/*
 *   Copyright 2021 Juanro49
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
 *
 */

package org.juanro.feedtv.Radio;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;

import net.bjoernpetersen.m3u.M3uParser;
import net.bjoernpetersen.m3u.model.M3uEntry;

import org.juanro.feedtv.Http.InputStreamVolleyRequest;
import org.juanro.feedtv.Http.VolleyController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Clase que se encarga de parsear el M3U en objetos Java
 */
public class M3UParser
{
	public static final String TAG = M3UParser.class.getSimpleName();
	private static volatile M3UParser instance;
	private List<M3uEntry> entradasM3u;
	private String urlCacheada = "";
	public static final String DEFAULT_URL = "https://archive.org/download/radiosrecopiladas2019/radios_recopiladas.m3u";

	private M3UParser() {}

	/**
	 * Método para obtener una instancia única (Thread-safe)
	 *
	 * @return instancia de M3UParser
	 */
	public static M3UParser getInstance()
	{
		if (instance == null)
		{
			synchronized (M3UParser.class)
			{
				if (instance == null)
				{
					instance = new M3UParser();
				}
			}
		}
		return instance;
	}

	/**
	 * Método que carga la lista usando la URL por defecto.
	 *
	 * @param forceUpdate si se debe ignorar la caché
	 * @param context contexto de la aplicación
	 * @param responseServerCallback callback de respuesta
	 */
	public void loadRadios(boolean forceUpdate, final Context context, final ResponseServerCallback responseServerCallback)
	{
		loadRadios(forceUpdate, DEFAULT_URL, context, responseServerCallback);
	}

	/**
	 * Método que carga la lista desde una URL específica
	 *
	 * @param forceUpdate si se debe ignorar la caché
	 * @param url dirección del M3U
	 * @param context contexto de la aplicación
	 * @param responseServerCallback callback de respuesta
	 */
	public void loadRadios(boolean forceUpdate, String url, final Context context, final ResponseServerCallback responseServerCallback)
	{
		if (!forceUpdate && url.equals(urlCacheada) && entradasM3u != null && !entradasM3u.isEmpty())
		{
			Log.i(TAG, "Cargar radios desde la cache");
			responseServerCallback.onChannelsLoadServer(entradasM3u);
		}
		else
		{
			Log.i(TAG, "Cargar radios desde el servidor: " + url);
			downloadRadios(url, context.getApplicationContext(), responseServerCallback);
		}
	}

	/**
	 * Realiza la descarga y parseo del M3U
	 */
	private void downloadRadios(final String url, final Context appContext, final ResponseServerCallback callback)
	{
		InputStreamVolleyRequest request = new InputStreamVolleyRequest(
				Request.Method.GET,
				url,
				response -> {
					Log.i(TAG, "M3U obtenido correctamente");
					try
					{
						String content = new String(response, StandardCharsets.UTF_8);
						List<M3uEntry> entradasTotales = M3uParser.parse(content);

						if (url.equals(DEFAULT_URL))
						{
							// Filtrado optimizado con Streams para la URL por defecto
							entradasM3u = entradasTotales.stream()
									.filter(entry -> Objects.equals(entry.getMetadata().get("tv-libre-comunitaria"), "yes")
											|| Objects.equals(entry.getMetadata().get("radio-libre-comunitaria"), "yes"))
									.collect(Collectors.toCollection(ArrayList::new));
						}
						else
						{
							entradasM3u = new ArrayList<>(entradasTotales);
						}
						urlCacheada = url;
					} catch (Exception e)
					{
						Toast.makeText(appContext, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
						Log.e(TAG, "Error al parsear la lista M3U: " + e.getMessage());
						entradasM3u = new ArrayList<>();
					}
					callback.onChannelsLoadServer(entradasM3u);
				},
				error -> {
					String errorMessage = "Error de red";
					if (error.networkResponse != null && error.networkResponse.data != null) {
						errorMessage = new String(error.networkResponse.data);
					}
					Log.e(TAG, "Error al acceder a la URL " + url);
					Toast.makeText(appContext, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
					callback.onChannelsLoadServer(new ArrayList<>());
				}, null);

		VolleyController.getInstance(appContext).addToQueue(request);
	}

	/**
	 * Interfaz para comunicar respuestas de las peticiones
	 */
	public interface ResponseServerCallback
	{
		void onChannelsLoadServer(List<M3uEntry> entradasM3u);
	}
}
