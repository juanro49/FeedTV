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
 */

package org.juanro.feedtv.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.juanro.feedtv.ChannelDetail;
import org.juanro.feedtv.R;
import org.juanro.feedtv.TV.Canal;
import org.juanro.feedtv.databinding.ItemListCanalesBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa el adapter de la lista de canales
 */
public class ChannelsAdapter extends RecyclerView.Adapter<ChannelsAdapter.ViewHolder> implements Filterable
{
	private final List<Canal> canales;
	private List<Canal> canalesFiltrados;
	private final ItemFilter mFilter = new ItemFilter();
	private final Context mContext;

	public ChannelsAdapter(Context context, List<Canal> canales)
	{
		this.canales = canales;
		this.canalesFiltrados = canales;
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
		ItemListCanalesBinding binding = ItemListCanalesBinding.inflate(LayoutInflater.from(viewGroup.getContext()), viewGroup, false);
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
		Canal canal = canalesFiltrados.get(position);

		// Establecer título
		vh.binding.channelTitle.setText(canal.nombre());

		// Establecer imagen
		Picasso.get()
				.load(canal.logo())
				.placeholder(R.drawable.placeholder)
				.into(vh.binding.channelIcon);


		// Registra las pulsaciones en la lista
		vh.itemView.setOnClickListener(view ->
		{
			// Inicia la activity de detalles del canal seleccionado
			Intent intent = new Intent(mContext, ChannelDetail.class);
			intent.putExtra("canal", canal);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
		});
	}

	/**
	 * Obtiene el tamaño de la lista
	 *
	 * @return el número de elementos en la lista filtrada
	 */
	@Override
	public int getItemCount()
	{
		return canalesFiltrados.size();
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

			ArrayList<Canal> canalesFiltradosLocal = new ArrayList<>();
			String nombreCanal;

			// Comenzar filtrado de canales
			for (int i = 0; i < canales.size(); i++)
			{
				nombreCanal = canales.get(i).nombre();

				// Comprobar que el nombre del canal contiene la secuencia de búsqueda
				if (nombreCanal.toLowerCase().contains(filtro))
				{
					canalesFiltradosLocal.add(canales.get(i));
				}
			}

			// Enviar lista filtrada a la clase de filtrado
			result.values = canalesFiltradosLocal;
			result.count = canalesFiltradosLocal.size();

			return result;
		}

		@SuppressLint("NotifyDataSetChanged")
		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			// Establecer lista con canales filtrados
			canalesFiltrados = (ArrayList<Canal>) results.values;
			notifyDataSetChanged();
		}
	}

	/**
	 * ViewHolder para asociar variables con elementos gráficos
	 */
	public static class ViewHolder extends RecyclerView.ViewHolder
	{
		public final ItemListCanalesBinding binding;

		public ViewHolder(@NonNull ItemListCanalesBinding binding)
		{
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
