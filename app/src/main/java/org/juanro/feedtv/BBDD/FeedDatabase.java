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
 *   Basado en la clase original creada por Hermosa Programación
 *
 */

package org.juanro.feedtv.BBDD;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.prof18.rssparser.model.RssItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Clase que administra el acceso y operaciones hacia la base de datos de las noticias
 */
public final class FeedDatabase extends SQLiteOpenHelper
{
    // Mapeado de indices
    private static final int columnId = 0;
    private static final int columnTitulo = 1;
    private static final int columnFecha = 2;
    private static final int columnUrl = 3;

    // Instancia única
    private static FeedDatabase singleton;

    // Etiqueta de depuración
    private static final String TAG = FeedDatabase.class.getSimpleName();

    // Nombre de la base de datos
    public static final String DATABASE_NAME = "Feed.db";

    // Versión actual de la base de datos
    public static final int DATABASE_VERSION = 1;


    private FeedDatabase(Context context)
	{
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Retorna la instancia unica de la base de datos
     *
     * @param context contexto donde se ejecutarán las peticiones
     * @return Instancia
     */
    public static synchronized FeedDatabase getInstance(Context context)
	{
        if (singleton == null)
        {
            singleton = new FeedDatabase(context.getApplicationContext());
        }

        return singleton;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
	{
        // Crear tabla en la BD
        db.execSQL(crearTabla);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
        // Añade los cambios que se realizarán en el esquema de la BBDD
        db.execSQL("DROP TABLE IF EXISTS " + nombreTabla);
        onCreate(db);
    }

	// Nombre Tabla
	public static String nombreTabla = "entrada";

	// Comando CREATE para la tabla
	public String crearTabla = "CREATE TABLE IF NOT EXISTS " + nombreTabla + " (" +
									Columnas.id + " integer primary key autoincrement," +
									Columnas.TITULO + " text not null," +
									Columnas.FECHA + " text," +
									Columnas.URL + " text not null," +
									Columnas.URL_MINIATURA + " text," +
									Columnas.NUMFECHA + " integer)";

	/**
	 * Clase que representa los campos de las tablas
	 */
	public static class Columnas
	{
		public static final String id = BaseColumns._ID;
		public static final String TITULO = "titulo";
		public static final String FECHA = "fecha";
		public static final String URL = "url";
		public static final String URL_MINIATURA = "thumb_url";
		public static final String NUMFECHA = "numFecha";
	}

	/**
	 * Crea una nueva tabla en la base de datos
	 *
	 * @param tabla nueva tabla
	 */
	public void crearTabla(String tabla)
	{
		nombreTabla = tabla;
		crearTabla = "CREATE TABLE IF NOT EXISTS " + nombreTabla + " (" +
						Columnas.id + " integer primary key autoincrement," +
						Columnas.TITULO + " text not null," +
						Columnas.FECHA + " text," +
						Columnas.URL + " text not null," +
						Columnas.URL_MINIATURA + " text," +
						Columnas.NUMFECHA + " integer)";

		getWritableDatabase().execSQL(crearTabla);
	}

	/**
	 * Elimina la tabla de un feed eliminado
	 *
	 * @param tabla
	 */
	public void eliminarTabla(String tabla)
	{
		getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + tabla);
		getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + tabla + "_");
	}

	/**
	 * Indica la tabla a usar por el feed
	 *
	 * @param tabla
	 */
	public void setTabla(String tabla)
	{
		Cursor c;
		c = getReadableDatabase().rawQuery("select name from sqlite_master where type = 'table' and name in('" + tabla + "', '" + tabla + "_')", null);
		c.moveToFirst();
		nombreTabla = c.getString(0);
		c.close();
	}

    /**
     * Obtiene todos los registros de la tabla
     *
     * @return cursor con los registros
     */
    public Cursor obtenerEntradas()
	{
        return getReadableDatabase().rawQuery("select * from " + nombreTabla + " order by " + Columnas.NUMFECHA + " desc", null);
    }

    /**
     * Inserta un registro en la tabla
     *
     * @param titulo      titulo de la entrada
     * @param fecha       fecha de la entrada
     * @param url         url del articulo
     * @param thumb_url   url de la miniatura
     */
    public void insertarEntrada(String titulo, String fecha, String url, String thumb_url, long numFecha)
	{
        ContentValues values = new ContentValues();
        values.put(Columnas.TITULO, titulo);
        values.put(Columnas.FECHA, fecha);
        values.put(Columnas.URL, url);
        values.put(Columnas.URL_MINIATURA, thumb_url);
        values.put(Columnas.NUMFECHA, numFecha);

        // Insertando el registro en la base de datos
        getWritableDatabase().insert(nombreTabla, null, values);
    }

    /**
     * Modifica los valores de las columnas de la tabla
     *
     * @param id          identificador de la entrada
     * @param titulo      titulo nuevo de la entrada
     * @param fecha       fecha nueva para la entrada
     * @param url         url nueva para la entrada
     * @param thumb_url   url nueva para la miniatura de la entrada
     */
    public void actualizarEntrada(int id, String titulo, String fecha, String url, String thumb_url, long numFecha)
	{
        ContentValues values = new ContentValues();
        values.put(Columnas.TITULO, titulo);
        values.put(Columnas.FECHA, fecha);
        values.put(Columnas.URL, url);
        values.put(Columnas.URL_MINIATURA, thumb_url);
        values.put(Columnas.NUMFECHA, numFecha);

        // Modificar entrada
        getWritableDatabase().update(nombreTabla, values, Columnas.id + "= ?", new String[]{String.valueOf(id)});
    }

    /**
     * Procesa una lista de items para su almacenamiento local
     * y sincronización.
     *
	 * @param lista lista de items
	 */
    public void sincronizarEntradas(List<RssItem> lista)
	{
        /*
        	Mapear temporalemente las entradas nuevas para realizar una
            comparación con las locales
        */
        LinkedHashMap <String, RssItem> entryMap = new LinkedHashMap<>();

        for (RssItem e : lista)
        {
            entryMap.put(e.getTitle(), e);
        }


        /*
        	Obtener las entradas locales y comenzar a comparar las entradas
        */
		Log.i(TAG, "Consultar entradas actualmente almacenadas");

		Cursor c = obtenerEntradas();
		Log.i(TAG, "Se encontraron " + c.getCount() + " entradas locales, comparando con las obtenidas del feed");

        int id;
        String titulo;
        String fecha;
        String url;

        while (c.moveToNext())
        {
            id = c.getInt(columnId);
            titulo = c.getString(columnTitulo);
            fecha = c.getString(columnFecha);
            url = c.getString(columnUrl);

			RssItem articulo = entryMap.get(titulo);

            if (articulo != null)
            {
                // Eliminar entradas existentes de la lista para prevenir su futura inserción
                entryMap.remove(titulo);

				String link = articulo.getLink();

				if(link.contains("?"))
				{
					link = link + "&utm_source=FeedTV&utm_medium=RSS";
				}
				else
				{
					link = link + "?utm_source=FeedTV&utm_medium=RSS";
				}

                // Comprobar si la entrada necesita ser actualizada
                if ((articulo.getTitle() != null && !articulo.getTitle().equals(titulo)) ||
                        (articulo.getPubDate() != null && !articulo.getPubDate().equals(fecha)) ||
                        (link != null && !link.equals(url)))
                {
					try
					{
						// Crear el campo numFecha para ordenar a partir de la fecha de publicación
						String pubDate = articulo.getPubDate();

						SimpleDateFormat sourceRSS = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
						SimpleDateFormat sourceAtom = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
						Date date = new Date();

						// La fecha viene en diferentes formatos para feeds de Atom y RSS
						if(pubDate.startsWith("2"))
						{
							pubDate = pubDate.substring(0, 19);
							date = sourceAtom.parse(pubDate);
						}
						else
						{
							date = sourceRSS.parse(pubDate);
						}

						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());
						long numFecha = Long.parseLong(sdf.format(date));

						// Actualizar artículo
						actualizarEntrada(id, articulo.getTitle(), articulo.getPubDate(), link, articulo.getImage(), numFecha);
					}
					catch (ParseException e)
					{
						e.printStackTrace();
					}
                }
            }
        }

        c.close();

        /*
        	Añadir entradas nuevas
        */
        for (RssItem a : entryMap.values())
		{
			try
			{
				// Crear el campo numFecha para ordenar a partir de la fecha de publicación
				String pubDate = a.getPubDate();

				SimpleDateFormat sourceRSS = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
				SimpleDateFormat sourceAtom = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
				Date date = new Date();

				// La fecha viene en diferentes formatos para feeds de Atom y RSS
				if(pubDate.startsWith("2"))
				{
					pubDate = pubDate.substring(0, 19);
					date = sourceAtom.parse(pubDate);
				}
				else
				{
					date = sourceRSS.parse(pubDate);
				}

				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());
				long numFecha = Long.parseLong(sdf.format(date));

				Log.i(TAG, "Insertado: " + a.getTitle());
				// Insertar artículo con link source feedtv
				String link = a.getLink();

				if(link.contains("?"))
				{
					link = link + "&utm_source=feedtv&utm_medium=feed";
				}
				else
				{
					link = link + "?utm_source=feedtv&utm_medium=feed";
				}

				insertarEntrada(a.getTitle(), a.getPubDate(), link, a.getImage(), numFecha);
			}
			catch (ParseException e)
			{
				e.printStackTrace();
			}
		}

        /*
        	Eliminar artículos antiguos
        */
        eliminarEntradas();

        Log.i(TAG, "Se actualizaron los registros");
    }

	/**
	 * Método para eliminar entradas
	 */
	public void eliminarEntradas()
    {
        int i = 1;
        Cursor c = obtenerEntradas();

        if(c.getCount() > 20)
		{
			c.moveToFirst();

			do
			{
				if(i > 20)
				{
					Log.i(TAG, "Eliminando: titulo=" + c.getString(columnTitulo));

					// Eliminar entrada
					getWritableDatabase().delete(
							nombreTabla,
							Columnas.TITULO + "= ?",
							new String[]{String.valueOf(c.getString(columnTitulo))});
				}

				i++;
			} while(c.moveToNext());
		}

        c.close();
    }
}