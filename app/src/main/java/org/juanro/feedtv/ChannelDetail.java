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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.juanro.feedtv.JSONParser.Canal;
import org.juanro.feedtv.JSONParser.Opciones;

import java.util.ArrayList;

/**
 * Clase que representa los detalles de un canal
 */
public class ChannelDetail extends AppCompatActivity
{
	private Canal canal;
	private TextView titulo, url, subtitulo;
	private ImageView imagen;
	private ListView fuentes;
	private SharedPreferences sharedPref;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Establecer tema de la aplicación
		aplicarTema();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.channel_detail);

		titulo = findViewById(R.id.channelName);
		imagen = findViewById(R.id.channelImage);
		url = findViewById(R.id.channelUrl);
		subtitulo = findViewById(R.id.channelElements);
		fuentes = findViewById(R.id.channelSources);

		// Establecer botón de atrás en action bar
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// Cargamos el archivo de preferencias
		sharedPref = getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);

		// Obtener canal de la anterior activity
		Intent intent = getIntent();
		canal = (Canal) intent.getSerializableExtra("canal");

		titulo.setText(canal.getNombre());
		url.setText(canal.getWeb());

		Picasso.get()
				.load(canal.getLogo())
				.placeholder(R.drawable.ic_launcher_foreground)
				.into(imagen);

		if (!canal.getOpciones().isEmpty())
		{
			// Crear lista con enlaces del canal
			ArrayList<String> sources = new ArrayList<>();

			for (Opciones opcion : canal.getOpciones())
			{
				sources.add(opcion.getUrl());
			}

			// Crear adapter y asignarlo a la lista de fuentes
			ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sources);
			fuentes.setAdapter(adapter);

			// Obtener pulsaciones sobre la lista
			fuentes.setOnItemClickListener((parent, view, position, id) ->
			{
				String source = fuentes.getItemAtPosition(position).toString();

				// Iniciar reproductor
				if(sharedPref.getBoolean("reproductor", false))
				{
					// Reproductor externo
					Uri uri = Uri.parse(source);
					Intent intent1 = new Intent(Intent.ACTION_VIEW, uri);
					//intent.setDataAndType(uri, "video/*");
					startActivity(intent1);
				}
				else
				{
					// Reproductor interno
					Intent i = new Intent(getApplicationContext(), Videoview.class);
					Bundle extras = new Bundle();
					extras.putString("url", source);
					i.putExtras(extras);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getApplicationContext().startActivity(i);
				}
			});
		}
	}

	/**
	 * Acciones menú
	 *
	 * @param item
	 * @return
	 */
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			this.onBackPressed();
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
