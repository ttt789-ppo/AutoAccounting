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

import org.ezbook.server.Server
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.SettingModel
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.response.Response

class SettingRoute(private val session: IHTTPSession) {
    /**
     * 获取设置
     */
    fun get(): Response {
        val params = session.parameters
        val key = params["key"]?.firstOrNull()?.toString() ?: ""
        if (key === "") {
            return Server.json(400, "key is required")
        }
        val data = Db.get().settingDao().query(key)

        return Server.json(200, "OK", data?.value ?: "")
    }

    /**
     * 设置
     */
    fun set(): Response {
        val key = session.parameters["key"]?.firstOrNull()?.toString() ?: ""
        if (key === "") {
            return Server.json(400, "key is required")
        }

        val value = Server.reqData(session)

        setByInner(key, value)
        return Server.json(200, "OK")

    }

    fun list(): Response {
        return Server.json(200, "OK", Db.get().settingDao().load())
    }

    companion object {
        fun setByInner(key: String, value: String) {
            val model = SettingModel()
            model.key = key
            model.value = value

            val data = Db.get().settingDao().query(key)

            if (data != null) {
                model.id = data.id
                Db.get().settingDao().update(model)
            } else {
                Db.get().settingDao().insert(model)
            }
        }
    }

}