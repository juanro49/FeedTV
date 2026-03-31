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
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FeedDao {
    @Query("SELECT * FROM fuentes")
    List<RssFeed> getAll();

    @Query("SELECT * FROM fuentes WHERE title = :title LIMIT 1")
    RssFeed findByTitle(String title);

    @Insert
    void insert(RssFeed feed);

    @Update
    void update(RssFeed feed);

    @Delete
    void delete(RssFeed feed);

    @Query("DELETE FROM fuentes WHERE title = :title")
    void deleteByTitle(String title);
}
