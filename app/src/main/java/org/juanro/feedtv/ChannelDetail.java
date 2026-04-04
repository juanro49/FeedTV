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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
		super.onCreate(savedInstanceState);
		ChannelDetailBinding binding = ChannelDetailBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		setSupportActionBar(binding.toolbar);

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
				// Configurar RecyclerView
				binding.channelSources.setLayoutManager(new LinearLayoutManager(this));
				
				// Crear adapter sencillo para RecyclerView
				List<String> sources = new ArrayList<>();
				for (Opciones opcion : canal.opciones())
				{
					sources.add(opcion.url());
				}

				// Usaremos un adaptador simple para el ejemplo, pero en Material 3 lo ideal es uno personalizado
				binding.channelSources.setAdapter(new RecyclerView.Adapter<>() {
					@NonNull
					@Override
					public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
						android.view.View view = android.view.LayoutInflater.from(parent.getContext())
								.inflate(android.R.layout.simple_list_item_1, parent, false);
						return new RecyclerView.ViewHolder(view) {};
					}

					@Override
					public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
						String source = sources.get(position);
						android.widget.TextView textView = holder.itemView.findViewById(android.R.id.text1);
						textView.setText(source);
						
						// Estilo M3: Padding y tipografía
						textView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);

						holder.itemView.setOnClickListener(v -> {
							// Iniciar reproductor
							if(sharedPref.getBoolean("reproductor", false)) {
								Uri uri = Uri.parse(source);
								Intent externalIntent = new Intent(Intent.ACTION_VIEW, uri);
								startActivity(externalIntent);
							} else {
								Intent i = new Intent(ChannelDetail.this, Videoview.class);
								i.putExtra("url", source);
								startActivity(i);
							}
						});

						holder.itemView.setOnLongClickListener(v -> {
							ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
							ClipData clip = ClipData.newPlainText("url", source);
							clipboard.setPrimaryClip(clip);
							Toast.makeText(ChannelDetail.this, getString(R.string.url_clipboard), Toast.LENGTH_LONG).show();
							return true;
						});
					}

					@Override
					public int getItemCount() {
						return sources.size();
					}
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


}
