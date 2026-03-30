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
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.juanro.feedtv.Http.VolleyController;

import java.util.ArrayList;
import java.util.List;


/**
 * Clase que se encarga de parsear el JSON en objetos Java
 */
public class JSONParser
{
	public static final String TAG = JSONParser.class.getSimpleName();
	private static volatile JSONParser instance;
	private List<Ambito> ambitos;
	private static final String TV_URL = "https://www.tdtchannels.com/lists/tv.json";

	private JSONParser() {}

	/**
	 * Método para obtener una instancia única (Thread-safe)
	 *
	 * @return instancia de JSONParser
	 */
	public static JSONParser getInstance()
	{
		if (instance == null)
		{
			synchronized (JSONParser.class)
			{
				if (instance == null)
				{
					instance = new JSONParser();
				}
			}
		}
		return instance;
	}

	/**
	 * Método que carga la lista de objetos parseados a la interfaz que lo envía a TvActivity
	 *
	 * @param forceUpdate si se debe ignorar la caché
	 * @param context contexto de la aplicación
	 * @param responseServerCallback callback de respuesta
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
			Log.i(TAG, "Cargar canales desde el servidor: " + TV_URL);
			ambitos = new ArrayList<>();
			downloadChannels(context.getApplicationContext(), responseServerCallback);
		}
	}

	/**
	 * Método que se encarga de realizar la descarga y parseo del JSON
	 */
	private void downloadChannels(final Context appContext, final ResponseServerCallback callback)
	{
		JsonObjectRequest jsonRequest = new JsonObjectRequest(
				Request.Method.GET,
				TV_URL,
				null,
				response -> {
					Log.i(TAG, "JSON obtenido correctamente");
					try
					{
						List<Ambito> parsedAmbits = parseResponse(response);
						ambitos.clear();
						ambitos.addAll(parsedAmbits);
					}
					catch (JSONException e)
					{
						Toast.makeText(appContext, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
						Log.e(TAG, "ERROR al parsear el JSON: " + e.getMessage());
					}
					callback.onChannelsLoadServer(ambitos);
				},
				error -> {
					String errorMessage = "Error de red";
					if (error.networkResponse != null && error.networkResponse.data != null) {
						errorMessage = new String(error.networkResponse.data);
					}
					Log.e(TAG, "Error al acceder a la URL " + TV_URL);
					Toast.makeText(appContext, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
					callback.onChannelsLoadServer(ambitos);
				}
		);

		VolleyController.getInstance(appContext).addToQueue(jsonRequest);
	}

	/**
	 * Lógica de parseo del objeto JSON a la estructura de clases
	 */
	private List<Ambito> parseResponse(JSONObject response) throws JSONException {
		List<Ambito> ambitList = new ArrayList<>();
		JSONArray countries = response.getJSONArray("countries");

		for (int i = 0; i < countries.length(); i++) {
			JSONObject countryJson = countries.getJSONObject(i);
			JSONArray ambitsArray = countryJson.getJSONArray("ambits");

			for (int j = 0; j < ambitsArray.length(); j++) {
				JSONObject ambitJson = ambitsArray.getJSONObject(j);
				String ambitName = ambitJson.getString("name");
				JSONArray channelsArray = ambitJson.getJSONArray("channels");
				
				List<Canal> canales = new ArrayList<>();
				for (int k = 0; k < channelsArray.length(); k++) {
					JSONObject channelJson = channelsArray.getJSONObject(k);
					
					JSONArray optionsJson = channelJson.getJSONArray("options");
					List<Opciones> channelOptions = new ArrayList<>();
					for (int l = 0; l < optionsJson.length(); l++) {
						JSONObject optionJson = optionsJson.getJSONObject(l);
						channelOptions.add(new Opciones(
								optionJson.getString("format"),
								optionJson.getString("url")
						));
					}

					canales.add(new Canal(
							channelJson.getString("name"),
							channelJson.getString("web"),
							channelJson.getString("logo"),
							channelOptions
					));
				}
				ambitList.add(new Ambito(ambitName, canales));
			}
		}
		return ambitList;
	}

	/**
	 * Interfaz para comunicar respuestas de las peticiones
	 */
	public interface ResponseServerCallback
	{
		void onChannelsLoadServer(List<Ambito> ambitos);
	}
}
