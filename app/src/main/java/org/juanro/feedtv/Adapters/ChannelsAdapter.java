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

package org.juanro.feedtv.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.juanro.feedtv.ChannelDetail;
import org.juanro.feedtv.R;
import org.juanro.feedtv.TV.Canal;

import java.util.ArrayList;

/**
 * Clase que representa el adapter de la lista de canales
 */
public class ChannelsAdapter extends RecyclerView.Adapter<ChannelsAdapter.ViewHolder> implements Filterable
{
	private ArrayList<Canal> canales;
	private ArrayList<Canal> canalesFiltrados;
	private ItemFilter mFilter = new ItemFilter();
	private Context mContext;

	public ChannelsAdapter(Context context, ArrayList<Canal> canales)
	{
		//super(context, 0, canales);
		this.canales = canales;
		this.canalesFiltrados = canales;
		this.mContext = context;
	}

	/**
	 * Obtiene el filtro de búsqueda en la lista
 	 */
	public Filter getFilter()
	{
		return mFilter;
	}


	/**
	 * Establece la vista de los elementos de la lista
	 *
	 * @param viewGroup
	 * @param viewType
	 * @return
	 */
	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType)
	{
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_canales, viewGroup, false);
		return new ChannelsAdapter.ViewHolder(v);
	}

	/**
	 * Crea la vista de cada elemento en la lista
	 *
	 * @param vh
	 */
	@Override
	public void onBindViewHolder(@NonNull ViewHolder vh, int position)
	{
		// Establecer título
		vh.titleView.setText(canalesFiltrados.get(vh.getAbsoluteAdapterPosition()).getNombre());

		// Establecer imagen
		Picasso.get()
				.load(canalesFiltrados.get(vh.getAbsoluteAdapterPosition()).getLogo())
				.placeholder(R.drawable.placeholder)
				.into(vh.imageView);


		// Registra las pulsaciones en la lista
		vh.itemView.setOnClickListener(view ->
		{
			// Inicia la activity de detalles del canal seleccionado
			Intent intent = new Intent(mContext, ChannelDetail.class);
			intent.putExtra("canal", canalesFiltrados.get(vh.getAbsoluteAdapterPosition()));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
		});
	}

	/**
	 * Obtiene el tamaño de la lista
	 *
	 * @return
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

			ArrayList<Canal> canalesFiltrados = new ArrayList<>();
			String nombreCanal;

			// Comenzar filtrado de canales
			for (int i = 0; i < canales.size(); i++)
			{
				nombreCanal = canales.get(i).getNombre();

				// Comprobar que el nombre del canal contiene la secuencia de búsqueda
				if (nombreCanal.toLowerCase().contains(filtro))
				{
					canalesFiltrados.add(canales.get(i));
				}
			}

			// Enviar lista filtrada a la clase de filtrado
			result.values = canalesFiltrados;
			result.count = canalesFiltrados.size();

			return result;
		}

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
	class ViewHolder extends RecyclerView.ViewHolder
	{
		ImageView imageView;
		TextView titleView;

		public ViewHolder(@NonNull View itemView)
		{
			super(itemView);
			imageView = itemView.findViewById(R.id.channel_icon);
			titleView = itemView.findViewById(R.id.channel_title);
		}
	}
}
