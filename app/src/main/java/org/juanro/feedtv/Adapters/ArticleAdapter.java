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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.prof.rssparser.Article;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.RecyclerView;

import org.juanro.feedtv.R;

/**
 * Clase que representa el adapter de la lista de artículos
 */
public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder>
{
    // Lista que contendrá los artículos
    private List<Article> articles;
    // Contexto
    private Context mContext;

    public ArticleAdapter(List<Article> list, Context context)
    {
        this.articles = list;
        this.mContext = context;
    }

    /**
     * Obtener lista de artículos
     *
     * @return
     */
    public List<Article> getArticleList()
    {
        return articles;
    }

    /**
     * Establece la vista de los elementos de la lista
     *
     * @param viewGroup
     * @param i
     * @return
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_noticias, viewGroup, false);
        return new ViewHolder(v);
    }

    /**
     * Crea la vista de cada elemento de la lista
     *
     * @param viewHolder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position)
    {
        // Obtener artículo de la posición actual
        Article currentArticle = articles.get(position);

        String pubDateString;

        try
        {
            String sourceDateString = String.valueOf(currentArticle.getPubDate());

            SimpleDateFormat sourceSdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            Date date = sourceSdf.parse(sourceDateString);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault());
            pubDateString = sdf.format(date);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            pubDateString = String.valueOf(currentArticle.getPubDate());
        }

        // Setear el título
        viewHolder.title.setText(currentArticle.getTitle());

        // Setear imagen
        Picasso.get()
                .load(currentArticle.getImage())
                .placeholder(R.drawable.placeholder)
                .into(viewHolder.image);

        // Setear fecha
        viewHolder.pubDate.setText(pubDateString);

        // Obtener categorías del artículo
        StringBuilder categories = new StringBuilder();

        for (int i = 0; i < currentArticle.getCategories().size(); i++)
        {
            if (i == currentArticle.getCategories().size() - 1)
            {
                categories.append(currentArticle.getCategories().get(i));
            }
            else
            {
                categories.append(currentArticle.getCategories().get(i)).append(", ");
            }
        }

        // Setear categorías
        viewHolder.category.setText(categories.toString());

        // Accion pulsación larga
        viewHolder.itemView.setOnLongClickListener(v ->
        {
            // Copiar url
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("url", articles.get(viewHolder.getAbsoluteAdapterPosition()).getLink());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(mContext, mContext.getString(R.string.url_clipboard), Toast.LENGTH_LONG).show();

            return true;
        });

        // Obtener pulsaciones
        viewHolder.itemView.setOnClickListener(view ->
        {
            // Obtener url del artículo
            String url = articles.get(viewHolder.getAbsoluteAdapterPosition()).getLink();

            // Abrir url en formato Custom Tab
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setShareState(CustomTabsIntent.SHARE_STATE_DEFAULT);
            CustomTabsIntent cti = builder.build();
            cti.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse(Intent.URI_ANDROID_APP_SCHEME + "//" + mContext.getPackageName()));
            // Hacer compatible con versiones anteriores a Android 6
            cti.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            cti.launchUrl(mContext, Uri.parse(url));
        });
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
    class ViewHolder extends RecyclerView.ViewHolder
	{
        TextView title;
        TextView pubDate;
        ImageView image;
        TextView category;

        private ViewHolder(View itemView)
		{
            super(itemView);
            title = itemView.findViewById(R.id.titulo);
            pubDate = itemView.findViewById(R.id.fecha);
            image = itemView.findViewById(R.id.imagen);
            category = itemView.findViewById(R.id.categorias);
        }
    }
}