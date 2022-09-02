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
 *   Basado en la clase original creada por LaQuay
 *
 */

package org.juanro.feedtv.TV;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.juanro.feedtv.Http.VolleyController;

import java.util.ArrayList;


/**
 * Clase que se encarga de parsear el JSON en objetos Java
 */
public class JSONParser
{
	public static final String TAG = JSONParser.class.getSimpleName();
	private static JSONParser instance;
	private ArrayList<Ambito> ambitos;
	private String url = "https://www.tdtchannels.com/lists/tv.json";


	/**
	 * Método que carga la lista de objetos parseados a la interfaz que lo envía a TvActivity
	 *
	 * @param forceUpdate
	 * @param context
	 * @param responseServerCallback
	 */
	public void loadChannels(boolean forceUpdate, final Context context, final ResponseServerCallback responseServerCallback)
	{
		if (!forceUpdate && ambitos != null && !ambitos.isEmpty())
		{
			Log.i(TAG, "Cargar canales desde la cache");
			responseServerCallback.onChannelsLoadServer(ambitos);
		}
		else
		{
			Log.i(TAG, "Cargar canales desde el servidor: " + url);
			ambitos = new ArrayList<>();
			downloadChannels(url, ambitos, context, responseServerCallback);
		}
	}

	private JSONParser()
	{
	}

	/**
	 * Método para obtener una instancia única
	 *
	 * @return
	 */
	public static JSONParser getInstance()
	{
		if (instance == null)
		{
			instance = new JSONParser();
		}

		return instance;
	}

	/**
	 * Método que se encarga de parsear el JSON
	 *
	 * @param URL
	 * @param ambitos
	 * @param context
	 * @param responseServerCallback
	 */
	private void downloadChannels(final String URL, final ArrayList<Ambito> ambitos, final Context context, final ResponseServerCallback responseServerCallback)
	{
		// Petición del JSON
		JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(
				Request.Method.GET,
				URL,
				null,
				new Response.Listener<JSONObject>()
				{
					/**
					 * Cosas a hacer cuando la respuesta es correcta
					 *
					 * @param response
					 */
					@Override
					public void onResponse(JSONObject response)
					{
						Log.i(TAG, "JSON obtenido correctamente");

						// Parsear elementos del JSON
						try
						{
							JSONArray countries = response.getJSONArray("countries");

							for (int i = 0; i < countries.length(); i++)
							{
								JSONObject jsonElement = countries.getJSONObject(i);
								JSONArray ambitsArray = jsonElement.getJSONArray("ambits");

								for (int j = 0; j < ambitsArray.length(); j++)
								{
									JSONObject ambitJson = ambitsArray.getJSONObject(j);
									JSONArray channelsArray = ambitJson.getJSONArray("channels");
									ArrayList<Canal> canales = new ArrayList<>();
									String ambitName = ambitJson.getString("name");

									for (int k = 0; k < channelsArray.length(); k++)
									{
										JSONObject channelJson = channelsArray.getJSONObject(k);

										String channelName = channelJson.getString("name");
										String channelWeb = channelJson.getString("web");
										String channelLogo = channelJson.getString("logo");

										JSONArray channelOptionsJson = channelJson.getJSONArray("options");
										ArrayList<Opciones> channelOptions = new ArrayList<>();

										for (int l = 0; l < channelOptionsJson.length(); l++)
										{
											JSONObject optionJson = channelOptionsJson.getJSONObject(l);

											String optionFormat = optionJson.getString("format");
											String optionURL = optionJson.getString("url");

											channelOptions.add(new Opciones(optionFormat, optionURL));
										}

										// Añadir canal obtenido a la lista de ese ámbito
										canales.add(new Canal(channelName, channelWeb, channelLogo, channelOptions));
									}

									// Añadir ámbito obtenido a la lista
									ambitos.add(new Ambito(ambitName, canales));
								}
							}
						}
						catch (JSONException e)
						{
							Toast.makeText(context.getApplicationContext(), "ERROR:" + e.getMessage(), Toast.LENGTH_LONG).show();
							Log.e(TAG, "ERROR al parsear el JSON");
						}

						// Enviar lista de canales al método de carga en la actividad principal
						responseServerCallback.onChannelsLoadServer(ambitos);
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
						// Enviar lista de canales vacío al método de carga en la actividad principal
						responseServerCallback.onChannelsLoadServer(ambitos);
					}
				}
		);

		// Añadir JSON a la cola de peticiones
		VolleyController.getInstance(context).addToQueue(jsonArrayRequest);
	}

	/**
	 * Interfaz para comunicar respuestas de las peticiones
	 */
	public interface ResponseServerCallback
	{
		void onChannelsLoadServer(ArrayList<Ambito> ambitos);
	}
}
