/*
 *   Copyright 2026 Juanro49
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

package org.juanro.feedtv.BBDD;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ArticleDao {
    @Query("SELECT * FROM articulos WHERE feedId = :feedId ORDER BY numFecha DESC")
    List<Article> getArticlesByFeed(int feedId);

    @Query("SELECT link FROM articulos WHERE feedId = :feedId")
    List<String> getExistingLinks(int feedId);

    /**
     * Obtiene los 20 artículos más recientes de toda la base de datos, 
     * independientemente de su fuente.
     */
    @Query("SELECT * FROM articulos ORDER BY numFecha DESC LIMIT 20")
    List<Article> getGlobalRecentArticles();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Article> articles);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Article article);

    @Update
    void update(Article article);

    @Query("DELETE FROM articulos WHERE id IN (SELECT id FROM articulos WHERE feedId = :feedId ORDER BY numFecha DESC LIMIT 999 OFFSET 20)")
    void deleteOldArticles(int feedId);
}
