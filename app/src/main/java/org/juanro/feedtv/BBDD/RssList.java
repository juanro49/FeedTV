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

package org.juanro.feedtv.BBDD;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


/**
 * Clase que administra la base de datos de las fuentes de noticias
 */
public class RssList extends SQLiteOpenHelper
{
	// Metainformación de la base de datos
	public static String FEED_TABLE_NAME = "fuentes";
	public static final String STRING_TYPE = "TEXT";
	public static final String INT_TYPE = "INTEGER";

    // Nombre de la base de datos
	public static final String DATABASE_NAME = "ListaFeeds.db";


	// Versión actual de la base de datos
	public static final int DATABASE_VERSION = 1;


	public RssList(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		// Crear la tabla 'entrada'
		db.execSQL(CREAR_FUENTE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// Añade los cambios que se realizarán en el esquema
		db.execSQL("DROP TABLE IF EXISTS " + FEED_TABLE_NAME);
		onCreate(db);
	}

	/**
	 * Campos de las tablas de las fuentes
	 */
	public static class ColumnFeeds
	{
		public static final String ID = BaseColumns._ID;
		public static final String NOMBRE = "titulo";
		public static final String URL = "url";
	}

	// Comando CREATE para la tabla de feeds
	public static final String CREAR_FUENTE =
			"CREATE TABLE " + FEED_TABLE_NAME + "(" +
					ColumnFeeds.ID + " " + INT_TYPE + " primary key autoincrement," +
					ColumnFeeds.NOMBRE + " " + STRING_TYPE + " not null," +
					ColumnFeeds.URL + " " + STRING_TYPE + " not null" + ")";

	/**
	 * Obtiene todos los registros de la tabla
	 *
	 * @return cursor con los registros
	 */
	public Cursor obtenerEntradas()
	{
		// Seleccionamos todas las filas de la tabla
		return this.getReadableDatabase().rawQuery("select * from " + FEED_TABLE_NAME + " order by " + ColumnFeeds.ID + " asc", null);
	}


	/**
	 * Inserta un registro en la tabla
	 *
	 * @param nombre      nombre de la entrada
	 * @param url         url del feed
	 */
	public void insertarEntrada(String nombre, String url)
	{
		ContentValues values = new ContentValues();
		values.put(ColumnFeeds.NOMBRE, nombre);
		values.put(ColumnFeeds.URL, url);

		// Insertando el registro en la base de datos
		this.getWritableDatabase().insert(FEED_TABLE_NAME, null, values);
	}

	/**
	 * Edita un registro en la tabla
	 *
	 * @param nombre      nombre de la entrada
	 * @param url         url del feed
	 */
	public void editarEntrada(String nombre, String url)
	{
		ContentValues values = new ContentValues();
		values.put(ColumnFeeds.URL, url);

		// Insertando el registro en la base de datos
		this.getWritableDatabase().update(FEED_TABLE_NAME, values, ColumnFeeds.NOMBRE + "=?", new String[]{String.valueOf(nombre)});
	}

	/**
	 * Eliminar entrada
	 * @param fuente
	 */
	public void eliminarEntradas(String fuente)
	{
		getWritableDatabase().delete(
				FEED_TABLE_NAME,
				ColumnFeeds.NOMBRE + "=?",
				new String[]{String.valueOf(fuente)});
	}
}
