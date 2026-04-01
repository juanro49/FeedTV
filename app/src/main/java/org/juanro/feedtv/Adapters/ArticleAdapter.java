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
 *   Basado en la clase original creada por Marco Gomiero para el ejemplo de RSS-Parser
 *
 */

package org.juanro.feedtv.Adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;

import org.juanro.feedtv.BBDD.Article;
import org.juanro.feedtv.R;
import org.juanro.feedtv.databinding.ItemListNoticiasBinding;

/**
 * Clase que representa el adapter de la lista de artículos
 */
public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder>
{
    private static final String TAG = "ArticleAdapter";
    // Lista que contendrá los artículos (Entidad de Room)
    private final List<Article> articles;
    // Contexto
    private final Context mContext;

    public ArticleAdapter(List<Article> list, Context context)
    {
        this.articles = list;
        this.mContext = context;
    }

    /**
     * Obtener lista de artículos
     *
     * @return la lista de artículos
     */
    public List<Article> getArticleList()
    {
        return articles;
    }

    /**
     * Establece la vista de los elementos de la lista
     *
     * @param viewGroup el grupo de la vista
     * @param i el tipo de la vista
     * @return un nuevo ViewHolder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        ItemListNoticiasBinding binding = ItemListNoticiasBinding.inflate(LayoutInflater.from(viewGroup.getContext()), viewGroup, false);
        return new ViewHolder(binding);
    }

    /**
     * Crea la vista de cada elemento en la lista
     *
     * @param viewHolder el ViewHolder que debe ser actualizado
     * @param position la posición del elemento dentro del conjunto de datos del adaptador
     */
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position)
    {
        // Obtener artículo de la posición actual
        Article currentArticle = articles.get(position);

        String pubDateString;
        String title;

        try
        {
            String sourceDateString = currentArticle.getPubDate();

            if (sourceDateString != null) {
                SimpleDateFormat sourceRSS = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                SimpleDateFormat sourceAtom = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                Date date;

                // La fecha viene en diferentes formatos para feeds de Atom y RSS
                if (sourceDateString.startsWith("2")) {
                    if (sourceDateString.length() > 19) {
                        sourceDateString = sourceDateString.substring(0, 19);
                    }
                    date = sourceAtom.parse(sourceDateString);
                } else {
                    date = sourceRSS.parse(sourceDateString);
                }

                // Usar el local configurado en la app para el formato de salida
                Locale currentLocale = mContext.getResources().getConfiguration().getLocales().get(0);
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm", currentLocale);
                pubDateString = date != null ? sdf.format(date) : sourceDateString;
            } else {
                pubDateString = "";
            }
        }
        catch (ParseException e)
        {
            Log.e(TAG, "Error al parsear la fecha", e);
            pubDateString = currentArticle.getPubDate();
        }

        // Setear el título
        title = currentArticle.getTitle();

        if(title != null && title.contains("<![CDATA["))
        {
            title = title.replace("<![CDATA[", "").replace("]]>", "");
        }

        viewHolder.binding.titulo.setText(title);

        // Setear imagen con Coil
        ImageRequest request = new ImageRequest.Builder(mContext)
                .data(currentArticle.getImage())
                .placeholder(R.drawable.placeholder)
                .target(viewHolder.binding.imagen)
                .build();
        Coil.imageLoader(mContext).enqueue(request);

        // Setear fecha
        viewHolder.binding.fecha.setText(pubDateString);

        // Setear categorías
        viewHolder.binding.categorias.setText(formatCategories(currentArticle.getCategories()));

        // Accion pulsación larga
        viewHolder.itemView.setOnLongClickListener(v ->
        {
            // Copiar url
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("url", articles.get(viewHolder.getBindingAdapterPosition()).getLink());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(mContext, mContext.getString(R.string.url_clipboard), Toast.LENGTH_LONG).show();

            return true;
        });

        // Obtener pulsaciones
        viewHolder.itemView.setOnClickListener(view ->
        {
            // Obtener url del artículo
            String url = articles.get(viewHolder.getBindingAdapterPosition()).getLink();

            if (url != null) {
                // Abrir url en formato Custom Tab
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setShareState(CustomTabsIntent.SHARE_STATE_DEFAULT);
                CustomTabsIntent cti = builder.build();
                cti.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse(Intent.URI_ANDROID_APP_SCHEME + "//" + mContext.getPackageName()));
                // Hacer compatible con versiones anteriores a Android 6
                cti.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                cti.launchUrl(mContext, Uri.parse(url));
            }
        });
    }

    /**
     * Formatea la lista de categorías en una cadena separada por comas
     *
     * @param categoryList lista de categorías
     * @return cadena formateada
     */
    private String formatCategories(List<String> categoryList) {
        StringBuilder categories = new StringBuilder();
        if (categoryList != null) {
            for (int i = 0; i < categoryList.size(); i++)
            {
                if (i == categoryList.size() - 1)
                {
                    categories.append(categoryList.get(i));
                }
                else
                {
                    categories.append(categoryList.get(i)).append(", ");
                }
            }
        }
        return categories.toString();
    }

    // Obtener tamaño de la lista
    @Override
    public int getItemCount()
    {
        return articles == null ? 0 : articles.size();
    }

    /**
     * ViewHolder para asociar variables con elementos gráficos
     */
    public static class ViewHolder extends RecyclerView.ViewHolder
	{
        public final ItemListNoticiasBinding binding;

        private ViewHolder(ItemListNoticiasBinding binding)
		{
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
