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
class BookNameModel {
    // 远程账本id
    var remoteId: String = ""

    // 账本列表
    @PrimaryKey(autoGenerate = true)
    var id = 0L

    /**
     * 账户名
     */
    var name: String = ""

    /**
     * 图标是url或base64编码字符串
     */
    var icon: String = "" // 图标

    companion object {

        suspend fun list(): List<BookNameModel> = withContext(
            Dispatchers.IO
        ) {
            val response = Server.request("book/list")
            val json = Gson().fromJson(response, JsonObject::class.java)

            runCatching {
                Gson().fromJson(
                    json.getAsJsonArray("data"),
                    Array<BookNameModel>::class.java
                ).toList()
            }.getOrNull() ?: emptyList()
        }


        suspend fun getByName(name: String): BookNameModel {
            list().firstOrNull { it.name == name }?.let {
                return it
            }
            return BookNameModel().apply { this.name = name }
        }


        suspend fun getDefaultBook(bookName: String): BookNameModel {
            if (bookName == "默认账本") {
                var book = list().firstOrNull()
                if (book == null) {
                    book = BookNameModel()
                    book.name = bookName
                }
                return book
            } else {
                return getByName(bookName)
            }
        }


        suspend fun getFirstBook(): BookNameModel {
            return list().firstOrNull() ?: BookNameModel().apply {
                name = "默认账本"
            }
        }


        suspend fun put(bookList: ArrayList<BookNameModel>, md5: String) =
            withContext(Dispatchers.IO) {
                Server.request("book/put?md5=$md5", Gson().toJson(bookList))
            }
    }
}