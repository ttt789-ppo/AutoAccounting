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

package net.ankio.auto.update

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.broadcast.LocalBroadcastHelper
import net.ankio.auto.setting.SettingUtils
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.ZipUtils
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.CategoryMapModel
import org.ezbook.server.db.model.RuleModel
import org.ezbook.server.db.model.SettingModel

class RuleUpdate(private val context: Context) : BaseUpdate(context) {

    override val repo: String
        get() = "AutoRule"

    override val dir: String
        get() = "规则"

    override fun ruleVersion(): String {
        return ConfigUtils.getString(Setting.RULE_VERSION, "None")
    }


    override fun onCheckedUpdate() {
        download = if (ConfigUtils.getString(
                Setting.UPDATE_CHANNEL,
                UpdateChannel.Github.name
            ) == UpdateChannel.Github.name
        ) {
            "https://cors.isteed.cc/github.com/AutoAccountingOrg/$repo/releases/download/$version/$version.zip"
        } else {
            pan() + "/$version.zip"
        }
    }


    override fun update(activity: Activity) {
        if (version.isEmpty()) return
        val loading = LoadingUtils(activity)
        loading.show(activity.getString(R.string.downloading))
        App.launch {
            try {
                val file = context.cacheDir.resolve("rule.zip")

                if (request.download(download, file)) {
                    try {
                        // 解压
                        val zip = context.cacheDir.resolve("rule")

                        ZipUtils.unzip(file.absolutePath, zip.absolutePath) { filename ->
                            val name = filename.substringAfterLast("/")
                            loading.setText(context.getString(R.string.unzip, name))
                        }
                        loading.setText(context.getString(R.string.unzip_finish))
                        // 解压完成
                        file.delete()

                        val root = zip.resolve("home/runner/work/AutoRule/AutoRule/dist")

                        // 读取rules.json
                        val rules = root.resolve("rules.json")
                        val arrays = Gson().fromJson(rules.readText(), JsonArray::class.java)

                        // 这是所有规则的元数据
                        val models = mutableListOf<RuleModel>()
                        val isXposed = BuildConfig.APPLICATION_ID.endsWith("xposed")
                        // 读取每个规则的json文件
                        arrays.forEach { json ->
                            val type = json.asJsonObject.get("ruleType").asString
                            val app = json.asJsonObject.get("ruleApp").asString
                            //Xposed模式不同步无障碍数据
                            if (isXposed && type == "helper") {
                                return@forEach
                            }
                            //无障碍模式不同步Xposed数据，短信除外
                            if (!isXposed && type == "app" && app !== "com.android.phone") {
                                return@forEach
                            }

                            val ruleModel = RuleModel()
                            ruleModel.name = json.asJsonObject.get("ruleChineseName").asString
                            ruleModel.systemRuleName = json.asJsonObject.get("ruleName").asString
                            ruleModel.app = app

                            ruleModel.type = when (type) {
                                "app" -> DataType.DATA.name
                                "helper" -> DataType.DATA.name
                                "notice" -> DataType.NOTICE.name
                                else -> ""
                            }
                            ruleModel.js =
                                root.resolve(json.asJsonObject.get("path").asString).readText()
                            ruleModel.creator = "system"

                            models.add(ruleModel)

                        }

                        SettingModel.set(Setting.JS_COMMON, root.resolve("common.js").readText())
                        SettingModel.set(
                            Setting.JS_CATEGORY,
                            root.resolve("category.js").readText()
                        )

                        val systemRule = RuleModel.system()
                        // 系统规则和云端规则做对比，使用name作为唯一标识，获取云端没有的系统规则，执行删除
                        systemRule.forEach { systemRuleModel ->
                            val find = models.find { it.name == systemRuleModel.name }
                            if (find == null) {
                                RuleModel.delete(systemRuleModel.id)
                                loading.setText(
                                    context.getString(
                                        R.string.delete_rule,
                                        systemRuleModel.name
                                    )
                                )
                            } else {
                                // 更新models中的数据
                                find.id = systemRuleModel.id
                                find.autoRecord = systemRuleModel.autoRecord
                                find.enabled = systemRuleModel.enabled
                            }
                        }

                        // 导入云端规则
                        models.forEach {
                            if (it.id == 0) {
                                RuleModel.add(it)
                                loading.setText(context.getString(R.string.add_rule, it.name))
                            } else {
                                RuleModel.update(it)
                                loading.setText(context.getString(R.string.update_rule, it.name))
                            }
                        }


                        // 更新分类映射
                        val categoryMapDb = CategoryMapModel.list(1, 0)

                        val categoryName = Gson().fromJson(
                            root.resolve("category.json").readText(),
                            JsonArray::class.java
                        )

                        categoryName.forEach { json ->
                            val name = json.asString
                            val find = categoryMapDb.find { it.name == name }
                            if (find == null) {
                                val model = CategoryMapModel()
                                model.name = name
                                model.mapName = name
                                CategoryMapModel.put(model)
                                loading.setText(context.getString(R.string.add_category_map, name))
                            }
                        }

                        categoryMapDb.forEach { categoryMapModel ->
                            val find = categoryName.find { it.asString == categoryMapModel.name }
                            if (find == null) {
                                CategoryMapModel.remove(categoryMapModel.id)
                                loading.setText(
                                    context.getString(
                                        R.string.delete_category_map,
                                        categoryMapModel.name
                                    )
                                )
                            }
                        }


                        // 更新版本号
                        ConfigUtils.putString(Setting.RULE_VERSION, version)

                        ToastUtils.info(R.string.update_success)

                    } catch (e: Exception) {
                        Logger.e("Update error", e)
                        ToastUtils.error(R.string.update_error)
                    }


                } else {
                    ToastUtils.error(context.getString(R.string.net_error_msg))
                }

            } catch (e: Exception) {
                Logger.e("Update error", e)
                ToastUtils.error(R.string.update_error)
            }finally {
                loading.close()
                LocalBroadcastHelper.sendBroadcast(LocalBroadcastHelper.ACTION_UPDATE_FINISH)
            }
        }


    }
}