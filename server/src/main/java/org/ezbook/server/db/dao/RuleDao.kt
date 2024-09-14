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
import org.ezbook.server.db.model.RuleModel

@Dao
interface RuleDao {

    @Query(
        """
    SELECT * FROM RuleModel 
    WHERE app = :app 
    AND (:type IS NULL OR type = :type)
    AND (:searchTerm IS NULL OR name LIKE '%' || :searchTerm || '%')
    ORDER BY id DESC 
    LIMIT :limit 
    OFFSET :offset
"""
    )
    fun loadByAppAndFilters(
        limit: Int,
        offset: Int,
        app: String,
        type: String? = null,
        searchTerm: String? = null
    ): List<RuleModel>

    //查询所有启用的规则，用于JS执行
    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type AND enabled = 1")
    fun loadAllEnabled(app: String, type: String): List<RuleModel>


    @Insert
    fun insert(rule: RuleModel): Long

    @Update
    fun update(rule: RuleModel)

    @Query("DELETE FROM RuleModel WHERE id = :id")
    fun delete(id: Int)

    @Query("SELECT app FROM RuleModel")
    fun queryApps(): List<String>

    @Query("SELECT * FROM RuleModel WHERE creator = 'system'")
    fun loadAllSystem(): List<RuleModel>


    @Query("SELECT * FROM RuleModel WHERE app = :app AND type = :type AND name = :name limit 1")
    fun query(type: String, app: String, name: String): RuleModel?
}