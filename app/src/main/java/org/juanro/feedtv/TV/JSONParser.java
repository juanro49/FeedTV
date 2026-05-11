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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.juanro.feedtv.Http.HttpClient;
import org.juanro.feedtv.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


/**
 * Clase que se encarga de parsear el JSON en objetos Java utilizando OkHttp.
 */
public class JSONParser {
    public static final String TAG = JSONParser.class.getSimpleName();
    private static volatile JSONParser instance;
    private List<Ambito> ambitos;
    // URL oficial actualizada
    private static final String TV_URL = "https://www.tdtchannels.com/lists/tv.json";

    private JSONParser() {
    }

    /**
     * Método para obtener una instancia única (Thread-safe)
     *
     * @return instancia de JSONParser
     */
    public static JSONParser getInstance() {
        if (instance == null) {
            synchronized (JSONParser.class) {
                if (instance == null) {
                    instance = new JSONParser();
                }
            }
        }
        return instance;
    }

    /**
     * Método que carga la lista de objetos parseados a la interfaz que lo envía a TvActivity
     *
     * @param forceUpdate            si se debe ignorar la caché
     * @param context                contexto de la aplicación
     * @param responseServerCallback callback de respuesta
     */
    public void loadChannels(boolean forceUpdate, final Context context, final ResponseServerCallback responseServerCallback) {
        if (!forceUpdate && ambitos != null && !ambitos.isEmpty()) {
            Log.i(TAG, "Cargar canales desde la cache");
            responseServerCallback.onChannelsLoadServer(ambitos);
        } else {
            Log.i(TAG, "Cargar canales desde el servidor: " + TV_URL);
            ambitos = new ArrayList<>();
            downloadChannels(context.getApplicationContext(), responseServerCallback);
        }
    }

    /**
     * Método que se encarga de realizar la descarga y parseo del JSON utilizando OkHttp
     */
    private void downloadChannels(final Context appContext, final ResponseServerCallback callback) {
        Request request = new Request.Builder()
                .url(TV_URL)
                .build();

        HttpClient.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error al acceder a la URL " + TV_URL, e);
                showErrorToast(appContext, appContext.getString(R.string.no_connection));
                callback.onChannelsLoadServer(new ArrayList<>());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        Log.e(TAG, "Respuesta no exitosa: " + response.code());
                        showErrorToast(appContext, appContext.getString(R.string.error) + response.code());
                        callback.onChannelsLoadServer(new ArrayList<>());
                        return;
                    }

                    String content = responseBody.string();
                    Log.i(TAG, "JSON obtenido correctamente");
                    try {
                        JSONObject jsonResponse = new JSONObject(content);
                        List<Ambito> parsedAmbits = parseResponse(jsonResponse);
                        ambitos.clear();
                        ambitos.addAll(parsedAmbits);
                        callback.onChannelsLoadServer(ambitos);
                    } catch (JSONException e) {
                        Log.e(TAG, "ERROR al parsear el JSON: " + e.getMessage());
                        showErrorToast(appContext, appContext.getString(R.string.url_error) + e.getMessage());
                        callback.onChannelsLoadServer(new ArrayList<>());
                    }
                }
            }
        });
    }

    private void showErrorToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show());
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
    public interface ResponseServerCallback {
        void onChannelsLoadServer(List<Ambito> ambitos);
    }
}
