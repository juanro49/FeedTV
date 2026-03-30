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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import org.juanro.feedtv.databinding.VideoBinding;

/**
 * Clase que representa un reproductor de vídeo simple
 */
public class Videoview extends AppCompatActivity
{
	private VideoBinding binding;
	private ExoPlayer exoPlayer;
	private boolean fullscreen = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		binding = VideoBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

		// Seteamos las acciones a realizar al pulsar boton de pantalla completa
		binding.videoPlayerView.setFullscreenButtonClickListener(isFullScreen ->
		{
			if(fullscreen) {
				controller.show(WindowInsetsCompat.Type.systemBars());

				if(getSupportActionBar() != null)
				{
					getSupportActionBar().show();
				}

				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				fullscreen = false;
			}else{
				controller.hide(WindowInsetsCompat.Type.systemBars());
				controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

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

		if (extras != null && extras.getString("url") != null)
		{
			String url = extras.getString("url");
			play(url);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (exoPlayer != null) {
			exoPlayer.release();
		}
	}

	/**
	 * Acciones botones menú
	 *
	 * @param item El elemento del menú seleccionado
	 * @return boolean Devuelve true si se manejó la selección
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			getOnBackPressedDispatcher().onBackPressed();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Método que configura el reproductor y lo ejecuta
	 *
	 * @param url La URL del vídeo a reproducir
	 */
	private void play(String url)
	{
		exoPlayer = new ExoPlayer.Builder(this).build();
		binding.videoPlayerView.setPlayer(exoPlayer);
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
		SharedPreferences sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

		if("Claro".equals(sharedPref.getString("tema", "Claro")))
		{
			setTheme(R.style.TemaClaro);
		}
		else
		{
			setTheme(R.style.TemaOscuro);
		}
	}
}
