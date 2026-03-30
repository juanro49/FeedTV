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

package org.juanro.feedtv;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import coil.Coil;
import coil.request.ImageRequest;

import org.juanro.feedtv.TV.Canal;
import org.juanro.feedtv.TV.Opciones;
import org.juanro.feedtv.databinding.ChannelDetailBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa los detalles de un canal
 */
public class ChannelDetail extends AppCompatActivity
{
	private SharedPreferences sharedPref;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		ChannelDetailBinding binding = ChannelDetailBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		// Establecer botón de atrás en action bar
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// Cargamos el archivo de preferencias
		sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

		// Obtener canal de la anterior activity de forma segura (AndroidX IntentCompat)
		Intent intent = getIntent();
		Canal canal = IntentCompat.getSerializableExtra(intent, "canal", Canal.class);

		if (canal != null)
		{
			binding.channelName.setText(canal.nombre());
			binding.channelUrl.setText(canal.web());

			// Carga de imagen con Coil
			ImageRequest request = new ImageRequest.Builder(this)
					.data(canal.logo())
					.placeholder(R.drawable.ic_launcher_foreground)
					.target(binding.channelImage)
					.build();
			Coil.imageLoader(this).enqueue(request);

			if (!canal.opciones().isEmpty())
			{
				// Crear lista con enlaces del canal
				List<String> sources = new ArrayList<>();

				for (Opciones opcion : canal.opciones())
				{
					sources.add(opcion.url());
				}

				// Crear adapter y asignarlo a la lista de fuentes
				ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sources);
				binding.channelSources.setAdapter(adapter);

				// Obtener pulsaciones sobre la lista
				binding.channelSources.setOnItemClickListener((parent, view, position, id) ->
				{
					String source = parent.getItemAtPosition(position).toString();

					// Iniciar reproductor
					if(sharedPref.getBoolean("reproductor", false))
					{
						// Reproductor externo
						Uri uri = Uri.parse(source);
						Intent externalIntent = new Intent(Intent.ACTION_VIEW, uri);
						startActivity(externalIntent);
					}
					else
					{
						// Reproductor interno
						Intent i = new Intent(this, Videoview.class);
						Bundle extras = new Bundle();
						extras.putString("url", source);
						i.putExtras(extras);
						startActivity(i);
					}
				});

				// Accion pulsación larga
				binding.channelSources.setOnItemLongClickListener((parent, view, position, id) ->
				{
					// Copiar url
					ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("url", parent.getItemAtPosition(position).toString());
					clipboard.setPrimaryClip(clip);

					Toast.makeText(this, this.getString(R.string.url_clipboard), Toast.LENGTH_LONG).show();

					return true;
				});
			}
		}
	}

	/**
	 * Acciones menú
	 *
	 * @param item El elemento de menú seleccionado
	 * @return boolean Devuelve true si se maneja la pulsación
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
	 * Método que aplica el tema de la aplicación
	 */
	private void aplicarTema()
	{
		sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

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
