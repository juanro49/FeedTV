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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Clase que representa un reproductor de vídeo simple
 */
public class Videoview extends AppCompatActivity
{
    private String url;
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);

        videoView =  findViewById(R.id.videoView);

        // GenErar pantalla completa
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION, WindowManager.LayoutParams.FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        // Obtener elementos de la activity anterior
        Intent i = getIntent();
        Bundle extras = i.getExtras();

        if (extras.getString("url") != null)
        {
            url = extras.getString("url");
            play(url);
        }
    }

    /**
     * Método que configura el reproductor y lo ejecuta
     *
     * @param url
     */
    private void play(String url)
	{
		MediaController mc = new MediaController(this);

		videoView.setVideoURI(Uri.parse(url));
		videoView.setMediaController(mc);
		videoView.requestFocus();

		// Obtener error al reproducir
        videoView.setOnErrorListener((mediaPlayer, what, extra) ->
        {
            Toast.makeText(Videoview.this, "Error: " + what + " - extra: " + extra, Toast.LENGTH_SHORT).show();
            return false;
        });

        // Ejecutar cuando el reproductor esté listo
        videoView.setOnPreparedListener(MediaPlayer::start);
    }
}
