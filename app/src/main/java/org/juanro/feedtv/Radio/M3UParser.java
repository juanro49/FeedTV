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
 */

package org.juanro.feedtv.Radio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.bjoernpetersen.m3u.M3uParser;
import net.bjoernpetersen.m3u.model.M3uEntry;

import org.juanro.feedtv.Http.HttpClient;
import org.juanro.feedtv.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


/**
 * Clase que se encarga de parsear el M3U en objetos Java utilizando OkHttp.
 */
public class M3UParser {
    public static final String TAG = M3UParser.class.getSimpleName();
    private static volatile M3UParser instance;
    private List<M3uEntry> entradasM3u;
    private String urlCacheada = "";
    public static final String DEFAULT_URL = "https://archive.org/download/radiosrecopiladas2019/radios_recopiladas.m3u";

    private M3UParser() {
    }

    /**
     * Método para obtener una instancia única (Thread-safe)
     *
     * @return instancia de M3UParser
     */
    public static M3UParser getInstance() {
        if (instance == null) {
            synchronized (M3UParser.class) {
                if (instance == null) {
                    instance = new M3UParser();
                }
            }
        }
        return instance;
    }

    /**
     * Método que carga la lista usando la URL por defecto.
     *
     * @param forceUpdate            si se debe ignorar la caché
     * @param context                contexto de la aplicación
     * @param responseServerCallback callback de respuesta
     */
    public void loadRadios(boolean forceUpdate, final Context context, final ResponseServerCallback responseServerCallback) {
        loadRadios(forceUpdate, DEFAULT_URL, context, responseServerCallback);
    }

    /**
     * Método que carga la lista desde una URL específica
     *
     * @param forceUpdate            si se debe ignorar la caché
     * @param url                    dirección del M3U
     * @param context                contexto de la aplicación
     * @param responseServerCallback callback de respuesta
     */
    public void loadRadios(boolean forceUpdate, String url, final Context context, final ResponseServerCallback responseServerCallback) {
        if (!forceUpdate && url.equals(urlCacheada) && entradasM3u != null && !entradasM3u.isEmpty()) {
            Log.i(TAG, "Cargar radios desde la cache");
            responseServerCallback.onChannelsLoadServer(entradasM3u);
        } else {
            Log.i(TAG, "Cargar radios desde el servidor: " + url);
            downloadRadios(url, context.getApplicationContext(), responseServerCallback);
        }
    }

    /**
     * Realiza la descarga y parseo del M3U utilizando OkHttp
     */
    private void downloadRadios(final String url, final Context appContext, final ResponseServerCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        HttpClient.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error al acceder a la URL " + url, e);
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

                    Log.i(TAG, "M3U obtenido correctamente");
                    String content = responseBody.string();
                    try {
                        List<M3uEntry> entradasTotales = M3uParser.parse(content);

                        if (url.equals(DEFAULT_URL)) {
                            // Filtrado optimizado con Streams para la URL por defecto
                            entradasM3u = entradasTotales.stream()
                                    .filter(entry -> Objects.equals(entry.getMetadata().get("tv-libre-comunitaria"), "yes")
                                            || Objects.equals(entry.getMetadata().get("radio-libre-comunitaria"), "yes"))
                                    .collect(Collectors.toCollection(ArrayList::new));
                        } else {
                            entradasM3u = new ArrayList<>(entradasTotales);
                        }
                        urlCacheada = url;
                    } catch (Exception e) {
                        Log.e(TAG, "Error al parsear la lista M3U: " + e.getMessage());
                        showErrorToast(appContext, appContext.getString(R.string.url_error) + e.getMessage());
                        entradasM3u = new ArrayList<>();
                    }
                    callback.onChannelsLoadServer(entradasM3u);
                }
            }
        });
    }

    private void showErrorToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    /**
     * Interfaz para comunicar respuestas de las peticiones
     */
    public interface ResponseServerCallback {
        void onChannelsLoadServer(List<M3uEntry> entradasM3u);
    }
}
