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
 *
 *
 */

package org.juanro.feedtv.Adapters;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import net.bjoernpetersen.m3u.model.M3uEntry;

import org.juanro.feedtv.R;
import org.juanro.feedtv.Videoview;
import org.juanro.feedtv.databinding.ItemListNoticiasBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import org.juanro.feedtv.Http.HttpClient;

/**
 * Clase que representa el adapter de la lista de canales
 */
public class RadiosAdapter extends RecyclerView.Adapter<RadiosAdapter.ViewHolder> implements Filterable
{
	private final List<M3uEntry> radios;
	private List<M3uEntry> radiosFiltradas;
	private final ItemFilter mFilter = new ItemFilter();
	private final Context mContext;
	private final Map<String, Integer> cacheEstados = new HashMap<>();

	private static final int STATUS_PENDING = 0;
	private static final int STATUS_ONLINE = 1;
	private static final int STATUS_OFFLINE = 2;

	public RadiosAdapter(Context context, List<M3uEntry> radios)
	{
		this.radios = radios;
		this.radiosFiltradas = radios;
		this.mContext = context;
	}

	/**
	 * Obtiene el filtro de búsqueda en la lista
	 *
	 * @return el filtro de elementos
 	 */
	@Override
	public Filter getFilter()
	{
		return mFilter;
	}


	/**
	 * Establece la vista de los elementos de la lista
	 *
	 * @param viewGroup el grupo de la vista
	 * @param viewType el tipo de la vista
	 * @return un nuevo ViewHolder
	 */
	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType)
	{
		ItemListNoticiasBinding binding = ItemListNoticiasBinding.inflate(LayoutInflater.from(viewGroup.getContext()), viewGroup, false);
		return new ViewHolder(binding);
	}

	/**
	 * Crea la vista de cada elemento en la lista
	 *
	 * @param vh el ViewHolder que debe ser actualizado
	 * @param position la posición del elemento dentro del conjunto de datos del adaptador
	 */
	@Override
	public void onBindViewHolder(@NonNull ViewHolder vh, int position)
	{
		M3uEntry radio = radiosFiltradas.get(position);

		// Establecer título
		vh.binding.titulo.setText(radio.getTitle());

		// Establecer título alternativo
		vh.binding.fecha.setText(radio.getMetadata().get("tvg-name"));

		// Establecer imagen con Coil
		ImageRequest request = new ImageRequest.Builder(mContext)
				.data(radio.getMetadata().getLogo())
				.placeholder(R.drawable.placeholder)
				.target(vh.binding.imagen)
				.build();
		Coil.imageLoader(mContext).enqueue(request);

		// Verificar estado del stream
		verificarEstadoStream(radio.getLocation().getUrl().toString(), vh);

		// Establecer categoría
		vh.binding.categorias.setText(radio.getMetadata().get("group-title"));

		// Registra las pulsaciones en la lista
		vh.itemView.setOnClickListener(view ->
		{
			SharedPreferences sharedPref = mContext.getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);
			String source = radio.getLocation().getUrl().toString();

			// Iniciar reproductor
			if(sharedPref.getBoolean("reproductor", false))
			{
				// Reproductor externo
				Uri uri = Uri.parse(source);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(intent);
			}
			else
			{
				// Reproductor interno
				Intent i = new Intent(mContext, Videoview.class);
				Bundle extras = new Bundle();
				extras.putString("url", source);
				i.putExtras(extras);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(i);
			}
		});

		// Accion pulsación larga
		vh.itemView.setOnLongClickListener(v ->
		{
			// Copiar url
			ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("url", radio.getLocation().getUrl().toString());
			clipboard.setPrimaryClip(clip);

			Toast.makeText(mContext, mContext.getString(R.string.url_clipboard), Toast.LENGTH_LONG).show();

			return true;
		});
	}

	/**
	 * Verifica si un stream está online mediante una petición GET limitada
	 */
	private void verificarEstadoStream(String url, ViewHolder vh) {
		if (cacheEstados.containsKey(url)) {
			actualizarUIEstado(cacheEstados.get(url), vh);
			return;
		}

		// Asegurar que el indicador es visible en radios
		vh.binding.statusIndicator.setVisibility(android.view.View.VISIBLE);
		actualizarUIEstado(STATUS_PENDING, vh);

		Request getRequest = new Request.Builder()
				.url(url)
				.addHeader("Range", "bytes=0-1024") // Solo pedimos el primer KB para ahorrar datos
				.build();

		HttpClient.getInstance().newCall(getRequest).enqueue(new Callback() {
			@Override
			public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
				cacheEstados.put(url, STATUS_OFFLINE);
				actualizarUIMainThread(STATUS_OFFLINE, vh);
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response) {
				// Si el GET devuelve éxito (200 o 206 Partial Content), está vivo
				int status = (response.isSuccessful() || response.code() == 206) ? STATUS_ONLINE : STATUS_OFFLINE;
				cacheEstados.put(url, status);
				actualizarUIMainThread(status, vh);
				response.close();
			}
		});
	}

	private void actualizarUIMainThread(int status, ViewHolder vh) {
		new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
				actualizarUIEstado(status, vh));
	}

	private void actualizarUIEstado(int status, ViewHolder vh) {
		int messageRes;
		switch (status) {
			case STATUS_ONLINE:
				vh.binding.statusIndicator.setBackgroundResource(R.drawable.status_online);
				messageRes = R.string.status_online_msg;
				break;
			case STATUS_OFFLINE:
				vh.binding.statusIndicator.setBackgroundResource(R.drawable.status_offline);
				messageRes = R.string.status_offline_msg;
				break;
			default:
				vh.binding.statusIndicator.setBackgroundResource(R.drawable.status_pending);
				messageRes = R.string.status_pending_msg;
				break;
		}
		vh.binding.statusIndicator.setOnClickListener(v ->
				Toast.makeText(mContext, mContext.getString(messageRes), Toast.LENGTH_SHORT).show());
	}

	/**
	 * Obtiene el tamaño de la lista
	 *
	 * @return el número de elementos en la lista filtrada
	 */
	@Override
	public int getItemCount()
	{
		return radiosFiltradas.size();
	}

	/**
	 * Clase que realiza el filtrado de las búsquedas
	 */
	private class ItemFilter extends Filter
	{
		@Override
		protected FilterResults performFiltering(CharSequence constraint)
		{
			String filtro = constraint.toString().toLowerCase();
			FilterResults result = new FilterResults();

			List<M3uEntry> radiosFiltradasLocal = new ArrayList<>();
			String nombreradio;

			// Comenzar filtrado de radios
			for (int i = 0; i < radios.size(); i++)
			{
				nombreradio = radios.get(i).getTitle();

				// Comprobar que el nombre del canal contiene la secuencia de búsqueda
				if (nombreradio != null && nombreradio.toLowerCase().contains(filtro))
				{
					radiosFiltradasLocal.add(radios.get(i));
				}
			}

			// Enviar lista filtrada a la clase de filtrado
			result.values = radiosFiltradasLocal;
			result.count = radiosFiltradasLocal.size();

			return result;
		}

		@SuppressLint("NotifyDataSetChanged")
		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			// Establecer lista con canales filtrados
			if (results.values != null)
			{
				radiosFiltradas = (List<M3uEntry>) results.values;
				notifyDataSetChanged();
			}
		}
	}

	/**
	 * ViewHolder para asociar variables con elementos gráficos
	 */
	public static class ViewHolder extends RecyclerView.ViewHolder
	{
		public final ItemListNoticiasBinding binding;

		public ViewHolder(@NonNull ItemListNoticiasBinding binding)
		{
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
