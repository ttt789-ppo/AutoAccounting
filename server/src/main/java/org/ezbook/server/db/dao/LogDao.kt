/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.ezbook.server.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.ezbook.server.db.model.LogModel

@Dao
interface LogDao {

    // change page
    @Query("SELECT * FROM LogModel ORDER BY id DESC LIMIT :limit OFFSET :offset")
    fun loadPage(limit: Int, offset: Int): List<LogModel>


    @Insert
    fun insert(log: LogModel): Long

    @Query("DELETE FROM LogModel")
    fun clear()

    // only keep the latest 10000 records
    @Query("DELETE FROM LogModel WHERE id NOT IN (SELECT id FROM LogModel ORDER BY id DESC LIMIT 10000)")
    fun clearOld()
}