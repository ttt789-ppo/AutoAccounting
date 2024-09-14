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

package net.ankio.auto.hooks.qianji.sync

import com.google.gson.Gson
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.core.App
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.xposed.Hooker
import net.ankio.auto.hooks.qianji.tools.QianJiAssetType
import net.ankio.auto.hooks.qianji.tools.UserUtils
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.constant.Currency
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.AssetsModel
import org.ezbook.server.db.model.SettingModel
import java.lang.reflect.Proxy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 将钱迹的资产数据同步给自动记账
 */
class AssetsUtils(private val manifest: HookerManifest, private val classLoader: ClassLoader) {

    private val assetPreviewPresenterImplClazz by lazy {
        XposedHelpers.findClass(
            "com.mutangtech.qianji.asset.account.mvp.AssetPreviewPresenterImpl",
            classLoader
        )
    }

    private val assetSqlHelperClazz by lazy {
        manifest.clazz("AssetDbHelper",classLoader)
    }

    /**
     * 从钱迹获取资产列表
     */
    suspend fun getAssetsList(): List<*> = suspendCoroutine { continuation ->
        var resumed = false
        // 获取所有构造函数
        val constructor = assetPreviewPresenterImplClazz.constructors.firstOrNull()

        /**
         * 410_951 public AssetPreviewPresenterImpl(u7.b bVar, f8.c cVar)
         */

        /**
         * 410_951 public AssetPreviewPresenterImpl(u7.b bVar, f8.c cVar)
         */

        if (constructor == null) {
            continuation.resumeWith(Result.failure(NoSuchMethodException("构造函数未找到")))
            return@suspendCoroutine
        }
        val parameterTypes: Array<Class<*>> = constructor.parameterTypes
        // 第一个是interface, 第二个是class
        val param2Object = XposedHelpers.newInstance(parameterTypes[1]) // f8.c
        val param1Clazz = parameterTypes[0] // u7.b


        val param1Object = Proxy.newProxyInstance(
            classLoader,
            arrayOf(param1Clazz)
        ) { _, _, _ ->
            null
        }

        val assetPreviewPresenterImplObj =
            XposedHelpers.newInstance(assetPreviewPresenterImplClazz, param1Object, param2Object)

        //  f8.c.setAccountList(java.util.List, boolean, boolean, boolean, java.util.HashMap, int)
        Hooker.hookOnce(
            parameterTypes[1],
            "setAccountList",
            List::class.java,
            Boolean::class.java,
            Boolean::class.java,
            Boolean::class.java,
            HashMap::class.java,
            Int::class.java
        ) {
            val accountList = it.args[0] as List<*> // 资产列表
            if (!resumed) {
                resumed = true
                continuation.resume(accountList)
            }
            true
        }

        //触发加载资产列表
        XposedHelpers.callMethod(assetPreviewPresenterImplObj, "loadAssets", true, false)

    }

    /**
     * 同步资产列表
     */
    suspend fun syncAssets() = withContext(Dispatchers.IO) {
        val accounts =
            withContext(Dispatchers.Main) {
                getAssetsList()
            }
        val assets = arrayListOf<AssetsModel>()

        accounts.forEach {
            val asset = it!!
            val model = AssetsModel()
            manifest.logD("账户信息:${Gson().toJson(asset)}")

            val fields = asset::class.java.declaredFields

            val stypeField = fields.filter { field -> field.name == "stype" }.getOrNull(0)
            val typeField = fields.filter { field -> field.name == "type" }.getOrNull(0)
            if (typeField == null || stypeField == null) return@forEach
            stypeField.isAccessible = true
            val stype = stypeField.get(asset) as Int

            typeField.isAccessible = true
            val type = typeField.get(asset) as Int

            model.type = when (type) {
                QianJiAssetType.Type_Money -> AssetsType.NORMAL
                QianJiAssetType.Type_Credit -> AssetsType.CREDIT
                QianJiAssetType.Type_Recharge -> AssetsType.VIRTUAL
                QianJiAssetType.Type_Invest -> AssetsType.FINANCIAL
                QianJiAssetType.Type_DebtLoan -> when (stype) {
                    QianJiAssetType.SType_Loan -> AssetsType.BORROWER
                    else -> AssetsType.CREDITOR
                }

                else -> AssetsType.NORMAL
            }

            for (field in fields) {
                field.isAccessible = true
                val value = field.get(asset) ?: continue
                when (field.name) {
                    "name" -> model.name = value as String
                    "icon" -> model.icon = value as String
                    "sort" -> model.sort = value as Int
                    "currency" -> model.currency = Currency.valueOf(value as String)
                    "loanInfo" -> model.extras = Gson().toJson(value)
                    "extra" -> {
                        if (model.extras == "{}") {
                            model.extras = Gson().toJson(value)
                        }
                    }
                }
            }
            assets.add(model)
        }
        val sync = Gson().toJson(assets)
        val md5 = App.md5(sync)
        val server = SettingModel.get(Setting.HASH_ASSET, "")

        if (server == md5 || assets.isEmpty()) { //资产为空也不同步
            manifest.log("资产信息未发生变化，无需同步, 服务端md5:${server} 本地md5:${md5}")
            return@withContext
        }
        manifest.log("同步账户信息:${Gson().toJson(assets)}")
        AssetsModel.put(assets, md5)
        withContext(Dispatchers.Main) {
            App.toast("已同步资产信息到自动记账")
        }
    }

    private var assets: List<*>? = null

    suspend fun getAssetByName(name: String): Any? = withContext(Dispatchers.IO) {
        if (assets == null) {
            assets = withContext(Dispatchers.Main){
                getAssetsList()
            }
        }
        return@withContext assets?.find {  XposedHelpers.getObjectField(it, "name") as String == name }
    }

    val assetClazz  by lazy {
        XposedHelpers.findClass(
            "com.mutangtech.qianji.data.model.AssetAccount",
            classLoader
        )
    }

    //   public static AssetAccount newInstance(int v, int v1) {
    //        AssetAccount assetAccount0 = new AssetAccount();
    //        assetAccount0.type = v;
    //        assetAccount0.stype = v1;
    //        return assetAccount0;
    //    }

    suspend fun getOrCreateAssetByName(name: String,type:Int,sType:Int): Any? = withContext(Dispatchers.IO) {
        var asset = getAssetByName(name)
        if (asset == null) {
            asset = XposedHelpers.callStaticMethod(assetClazz, "newInstance", type, sType)
            XposedHelpers.setObjectField(asset, "name", name)

            /**
             * {
             *     "color": "E06966",
             *     "createtime": 1726152536,
             *     "icon": "null",
             *     "id": 1726152536966,
             *     "incount": 1,
             *     "lastPayTime": 0,
             *     "loan": {
             *         "accountId": 0,
             *         "startdate": "2024-09-12",
             *         "enddate": "",
             *         "money": 12,
             *         "totalpay": 0
             *     },
             *     "money": 12,
             *     "name": "12",
             *     "sort": 0,
             *     "status": 0,
             *     "stype": 52,
             *     "type": 5,
             *     "usecount": 0,
             *     "userid": "200104405e109647c18e9"
             * }
             */
            XposedHelpers.setObjectField(asset, "createtime", 0)
            XposedHelpers.setObjectField(asset, "id", -1L)
            XposedHelpers.setObjectField(asset, "incount", 1)
            XposedHelpers.setObjectField(asset, "lastPayTime", 0)
            XposedHelpers.setObjectField(asset, "money", 0.0)
            XposedHelpers.setObjectField(asset, "stype", sType)
            XposedHelpers.setObjectField(asset, "type", type)
            XposedHelpers.setObjectField(asset, "usecount", 0)

            XposedHelpers.setObjectField(asset, "sort", 0)
            XposedHelpers.setObjectField(asset, "status", 0)


        }
        return@withContext asset
    }

    /**
     * 更新资产
     * asset: 资产对象
     */
    suspend fun updateAsset(asset: Any) = withContext(Dispatchers.IO) {
        // assetSqlHelperClazz
        //   com.mutangtech.qianji.data.db.convert.a aVar = new com.mutangtech.qianji.data.db.convert.a();
        //                AssetAccount assetAccount = this.AssetAccountName2222;
        //                if (assetAccount != null) {
        //                    aVar.insertOrReplace(assetAccount, false);
        //                }
        val assetSqlHelper = XposedHelpers.newInstance(assetSqlHelperClazz)
        XposedHelpers.callMethod(assetSqlHelper, "insertOrReplace", asset, false)
    }


}