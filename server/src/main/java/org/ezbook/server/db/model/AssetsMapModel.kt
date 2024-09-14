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

package org.ezbook.server.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ezbook.server.Server

@Entity
class AssetsMapModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0L

    /**
     * 是否将原始映射的账户名作为正则使用
     */
    var regex: Boolean = false

    /**
     * 原始获取到的账户名
     */
    var name: String = "" // 账户名

    /**
     * 映射到的账户名
     */
    var mapName: String = "" // 映射账户名

    companion object {
        suspend fun list(page: Int, pageSize: Int): List<AssetsMapModel> = withContext(
            Dispatchers.IO
        ) {
            val response = Server.request("assets/map/list?page=$page&limit=$pageSize")
            val json = Gson().fromJson(response, JsonObject::class.java)

            runCatching {
                Gson().fromJson(
                    json.getAsJsonArray("data"),
                    Array<AssetsMapModel>::class.java
                ).toList()
            }.getOrNull() ?: emptyList()
        }

        suspend fun put(model: AssetsMapModel): JsonObject = withContext(Dispatchers.IO) {
            val response = Server.request("assets/map/put", Gson().toJson(model))
            val json = Gson().fromJson(response, JsonObject::class.java)

            runCatching {
                Gson().fromJson(
                    json.getAsJsonArray("data"),
                    JsonObject::class.java
                )
            }.getOrNull() ?: JsonObject()
        }

        suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
            Server.request("assets/map/delete?id=$id")
        }
    }
}