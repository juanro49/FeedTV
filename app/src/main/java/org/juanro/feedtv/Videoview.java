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
 *
 */

package org.juanro.feedtv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;

/**
 * Clase que representa un reproductor de vídeo simple
 */
public class Videoview extends AppCompatActivity
{
    private SharedPreferences sharedPref;
    private String url;
    private ExoPlayer exoPlayer;
    private StyledPlayerView playerView;
    private boolean fullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Establecer tema de la aplicación
        aplicarTema();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);

        playerView = findViewById(R.id.videoPlayerView);

        // Seteamos las acciones a realizar al pulsar boton de pantalla completa
        playerView.setControllerOnFullScreenModeChangedListener(isFullScreen ->
        {
            if(fullscreen) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

                if(getSupportActionBar() != null)
                {
                    getSupportActionBar().show();
                }

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                fullscreen = false;
            }else{
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                        |View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

                if(getSupportActionBar() != null)
                {
                    getSupportActionBar().hide();
                }

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                fullscreen = true;
            }
        });

        // Establecer botón de atrás en action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Obtener elementos de la activity anterior
        Intent i = getIntent();
        Bundle extras = i.getExtras();

        if (extras.getString("url") != null)
        {
            url = extras.getString("url");
            play(url);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        exoPlayer.release();
    }

    /**
     * Acciones botones menú
     *
     * @param item
     * @return
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                this.onBackPressed();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Método que configura el reproductor y lo ejecuta
     *
     * @param url
     */
    private void play(String url)
	{
        exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
        playerView.setPlayer(exoPlayer);
        // Establecemos el enlace a reproducir.
        MediaItem mediaItem = MediaItem.fromUri(url);
        exoPlayer.setMediaItem(mediaItem);
        // Lanzamos la reproducción
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.play();
    }

    /**
     * Método que aplica el tema de la aplicación
     */
    private void aplicarTema()
    {
        sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

        if(sharedPref.getString("tema", "Claro").equals("Claro"))
        {
            setTheme(R.style.TemaClaro);
        }
        else
        {
            setTheme(R.style.TemaOscuro);
        }
    }
}
