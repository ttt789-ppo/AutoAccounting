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

package org.ezbook.server.routes

import com.google.gson.Gson
import org.ezbook.server.Server
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.LogModel
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response

class LogRoute(private val session: IHTTPSession) {
    /**
     * 获取日志列表
     */
    fun list(): Response {
        //remove expired data
        Db.get().logDao().clearOld()

        val params = session.parameters
        val page = params["page"]?.firstOrNull()?.toInt() ?: 1
        val limit = params["limit"]?.firstOrNull()?.toInt() ?: 10
        val offset = (page - 1) * limit
        val logs = Db.get().logDao().loadPage(limit, offset)
        return Server.json(200, "OK", logs)
    }

    /**
     * 添加日志
     */
    fun add(): Response {
        val json = Gson().fromJson(Server.reqData(session), LogModel::class.java)
        val id = Db.get().logDao().insert(json)
        return Server.json(200, "OK", id)
    }

    /**
     * 清空日志
     */
    fun clear(): Response {
        Db.get().logDao().clear()
        return Server.json(200, "OK")
    }
}