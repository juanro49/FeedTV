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
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import net.bjoernpetersen.m3u.M3uParser;
import net.bjoernpetersen.m3u.model.M3uEntry;

import org.juanro.feedtv.Http.InputStreamVolleyRequest;
import org.juanro.feedtv.Http.VolleyController;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Clase que se encarga de parsear el JSON en objetos Java
 */
public class M3UParser
{
	public static final String TAG = M3UParser.class.getSimpleName();
	private static M3UParser instance;
	private List<M3uEntry> entradasM3u;
	private String url = "https://archive.org/download/radiosrecopiladas2019/radios_recopiladas.m3u";

	/**
	 * Método que carga la lista de objetos parseados a la interfaz que lo envía a TvActivity
	 *
	 * @param forceUpdate
	 * @param context
	 * @param responseServerCallback
	 */
	public void loadRadios(boolean forceUpdate, final Context context, final ResponseServerCallback responseServerCallback)
	{
		if (!forceUpdate && entradasM3u != null && !entradasM3u.isEmpty())
		{
			Log.i(TAG, "Cargar radios desde la cache");
			responseServerCallback.onChannelsLoadServer(entradasM3u);
		}
		else
		{
			Log.i(TAG, "Cargar radios desde el servidor: " + url);
			entradasM3u = new ArrayList<>();
			downloadRadios(url, entradasM3u, context, responseServerCallback);
		}
	}

	private M3UParser()
	{
	}

	/**
	 * Método para obtener una instancia única
	 *
	 * @return
	 */
	public static M3UParser getInstance()
	{
		if (instance == null)
		{
			instance = new M3UParser();
		}

		return instance;
	}

	/**
	 * Método que se encarga de parsear el JSON
	 *
	 * @param URL
	 * @param entradasM3u
	 * @param context
	 * @param responseServerCallback
	 */
	private void downloadRadios(final String URL, final List<M3uEntry> entradasM3u, final Context context, final ResponseServerCallback responseServerCallback)
	{
		InputStreamVolleyRequest prueba = new InputStreamVolleyRequest(
				Request.Method.GET,
				URL,
				new Response.Listener<byte[]>()
				{
					@RequiresApi(api = Build.VERSION_CODES.O)
					@Override
					public void onResponse(byte[] response)
					{
						Log.i(TAG, "M3U obtenido correctamente");

						try
						{
							FileOutputStream outputStream;
							//Guardamos el fichero obtenido
							outputStream = context.openFileOutput("radios_recopiladas.m3u", Context.MODE_PRIVATE);
							outputStream.write(response);
							outputStream.close();

							// Parseamos el fichero obtenido
							Path m3uFile = context.getFileStreamPath("radios_recopiladas.m3u").toPath();
							ArrayList<M3uEntry> entradasTotales = new ArrayList<>(M3uParser.parse(m3uFile));

							for (int i = 0; i < entradasTotales.size(); i++)
							{
								if (Objects.equals(entradasTotales.get(i).getMetadata().get("tv-libre-comunitaria"), "yes") || Objects.equals(entradasTotales.get(i).getMetadata().get("radio-libre-comunitaria"), "yes"))
								{
									entradasM3u.add(entradasTotales.get(i));
								}
							}
						} catch (IOException e)
						{
							Toast.makeText(context.getApplicationContext(), "ERROR:" + e.getMessage(), Toast.LENGTH_LONG).show();
							Log.e(TAG, "Error al parsear la lista M3U: " + e.getMessage());
						}

						// Enviar lista de canales al método de carga en la actividad principal
						responseServerCallback.onChannelsLoadServer(entradasM3u);
					}
				},
				new Response.ErrorListener()
				{
					/**
					 * Cosas a hacer cuando la respuesta es incorrecta
					 *
					 * @param error
					 */
					@Override
					public void onErrorResponse(VolleyError error)
					{
						String errorMessage = new String(error.networkResponse.data);
						Log.e(TAG, "Error al acceder a la URL " + URL);
						Toast.makeText(context.getApplicationContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();
						// Enviar lista de canales al método de carga en la actividad principal
						responseServerCallback.onChannelsLoadServer(entradasM3u);
					}
				}, null);

		// Añadir M3U a la cola de peticiones
		VolleyController.getInstance(context).addToQueue(prueba);
	}

	/**
	 * Interfaz para comunicar respuestas de las peticiones
	 */
	public interface ResponseServerCallback
	{
		void onChannelsLoadServer(List<M3uEntry> entradasM3u);
	}
}
