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

import org.juanro.feedtv.ChannelsActivity;
import org.juanro.feedtv.R;
import org.juanro.feedtv.TV.Ambito;

import java.util.ArrayList;

/**
 * Clase que representa el adapter de la lista de canales
 */
public class AmbitsAdapter extends RecyclerView.Adapter<AmbitsAdapter.ViewHolder> implements Filterable
{
	private ArrayList<Ambito> ambitos;
	private ArrayList<Ambito> ambitosFiltrados;
	private ItemFilter mFilter = new ItemFilter();
	private Context mContext;

	public AmbitsAdapter(Context context, ArrayList<Ambito> ambitos)
	{
		//super(context, 0, ambitos);
		this.ambitos = ambitos;
		this.ambitosFiltrados = ambitos;
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
		return new AmbitsAdapter.ViewHolder(v);
	}

	/**
	 * Crea la vista de cada elemento en la lista
	 *
	 * @param vh
	 * @param position
	 */
	@Override
	public void onBindViewHolder(@NonNull ViewHolder vh, int position)
	{
		// Establecer título
		vh.titleView.setText(ambitosFiltrados.get(vh.getAbsoluteAdapterPosition()).getNombre());

		// Registra las pulsaciones en la lista
		vh.itemView.setOnClickListener(view ->
		{
			// Inicia la activity de detalles del canal seleccionado
			Intent intent = new Intent(mContext, ChannelsActivity.class);
			intent.putExtra("Ambito", ambitosFiltrados.get(vh.getAbsoluteAdapterPosition()));
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
		return ambitosFiltrados.size();
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

			ArrayList<Ambito> ambitosFiltrados = new ArrayList<>();
			String nombreAmbito;

			// Comenzar filtrado de canales
			for (int i = 0; i < ambitos.size(); i++)
			{
				nombreAmbito = ambitos.get(i).getNombre();

				// Comprobar que el nombre del canal contiene la secuencia de búsqueda
				if (nombreAmbito.toLowerCase().contains(filtro))
				{
					ambitosFiltrados.add(ambitos.get(i));
				}
			}

			// Enviar lista filtrada a la clase de filtrado
			result.values = ambitosFiltrados;
			result.count = ambitosFiltrados.size();

			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			// Establecer lista con canales filtrados
			ambitosFiltrados = (ArrayList<Ambito>) results.values;
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
