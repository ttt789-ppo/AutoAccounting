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
import androidx.room.Update
import org.ezbook.server.db.model.SettingModel

@Dao
interface SettingDao {
    @Insert
    fun insert(setting: SettingModel)

    @Update
    fun update(setting: SettingModel)

    @Query("SELECT * FROM SettingModel WHERE `key` = :key limit 1")
    fun query(key: String): SettingModel?

    @Query("SELECT * FROM SettingModel")
    fun load(): List<SettingModel>

}