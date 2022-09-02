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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import net.bjoernpetersen.m3u.model.M3uEntry;

import org.juanro.feedtv.R;
import org.juanro.feedtv.Videoview;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa el adapter de la lista de canales
 */
public class RadiosAdapter extends RecyclerView.Adapter<RadiosAdapter.ViewHolder> implements Filterable
{
	private List<M3uEntry> radios;
	private List<M3uEntry> radiosFiltradas;
	private ItemFilter mFilter = new ItemFilter();
	private Context mContext;

	public RadiosAdapter(Context context, List<M3uEntry> radios)
	{
		//super(context, 0, radios);
		this.radios = radios;
		this.radiosFiltradas = radios;
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
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_noticias, viewGroup, false);
		return new RadiosAdapter.ViewHolder(v);
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
		vh.title.setText(radiosFiltradas.get(vh.getAbsoluteAdapterPosition()).getTitle());

		// Establecer título alternativo
		vh.altTitle.setText(radiosFiltradas.get(vh.getAbsoluteAdapterPosition()).getMetadata().get("tvg-name"));

		// Establecer imagen
		Picasso.get()
				.load(radiosFiltradas.get(vh.getAbsoluteAdapterPosition()).getMetadata().getLogo())
				.placeholder(R.drawable.placeholder)
				.into(vh.image);

		// Establecer categoría
		vh.category.setText(radiosFiltradas.get(vh.getAbsoluteAdapterPosition()).getMetadata().get("group-title"));

		// Registra las pulsaciones en la lista
		vh.itemView.setOnClickListener(view ->
		{
			SharedPreferences sharedPref = mContext.getSharedPreferences("org.juanro.feedtv_preferences", MODE_PRIVATE);
			String source = radiosFiltradas.get(vh.getAbsoluteAdapterPosition()).getLocation().getUrl().toString();

			// Iniciar reproductor
			if(sharedPref.getBoolean("reproductor", false))
			{
				// Reproductor externo
				Uri uri = Uri.parse(source);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				//intent.setDataAndType(uri, "video/*");
				mContext.getApplicationContext().startActivity(intent);
			}
			else
			{
				// Reproductor interno
				Intent i = new Intent(mContext.getApplicationContext(), Videoview.class);
				Bundle extras = new Bundle();
				extras.putString("url", source);
				i.putExtras(extras);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.getApplicationContext().startActivity(i);
			}
		});

		// Accion pulsación larga
		vh.itemView.setOnLongClickListener(v ->
		{
			// Copiar url
			ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("url", radiosFiltradas.get(vh.getAbsoluteAdapterPosition()).getLocation().getUrl().toString());
			clipboard.setPrimaryClip(clip);

			Toast.makeText(mContext, mContext.getString(R.string.url_clipboard), Toast.LENGTH_LONG).show();

			return true;
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

			List<M3uEntry> radiosFiltradas = new ArrayList<>();
			String nombreradio;

			// Comenzar filtrado de radios
			for (int i = 0; i < radios.size(); i++)
			{
				nombreradio = radios.get(i).getTitle();

				// Comprobar que el nombre del canal contiene la secuencia de búsqueda
				if (nombreradio != null && nombreradio.toLowerCase().contains(filtro))
				{
					radiosFiltradas.add(radios.get(i));
				}
			}

			// Enviar lista filtrada a la clase de filtrado
			result.values = radiosFiltradas;
			result.count = radiosFiltradas.size();

			return result;
		}

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
	class ViewHolder extends RecyclerView.ViewHolder
	{
		TextView title;
		TextView altTitle;
		ImageView image;
		TextView category;

		public ViewHolder(@NonNull View itemView)
		{
			super(itemView);
			title = itemView.findViewById(R.id.titulo);
			altTitle = itemView.findViewById(R.id.fecha);
			image = itemView.findViewById(R.id.imagen);
			category = itemView.findViewById(R.id.categorias);
		}
	}
}
