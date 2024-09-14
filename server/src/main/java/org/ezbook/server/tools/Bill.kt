/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package org.ezbook.server.tools

import android.content.Context
import org.ezbook.server.Server
import org.ezbook.server.constant.Currency
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.BillInfoModel

object Bill {
    /**
     * 检查重复性
     * @param bill 账单1
     * @param bill2 账单2
     */
    private fun checkRepeat(bill: BillInfoModel, bill2: BillInfoModel): Boolean {
        Server.log("重复性比较")
        Server.log("bill:$bill")
        Server.log("bill2:$bill2")

        Server.log("bill2.time == bill.time => ${bill2.time == bill.time}")
        Server.log("bill.money == bill2.money => ${bill.money == bill2.money}")
        Server.log("bill.type == bill2.type => ${bill.type == bill2.type}")
        Server.log("bill2.channel != bill.channel => ${bill2.channel != bill.channel}")

        if (bill.type == bill2.type) {
            if (bill2.time == bill.time) return true //时间一致，一定是同一笔交易
            if (bill2.channel != bill.channel) return true //渠道不一致，一定是不同的交易
            if (bill2.accountNameFrom == bill.accountNameFrom) return true //来源账户一致，一定是同一笔交易
            if (bill2.shopItem == bill.shopItem && bill.shopName == bill2.shopName) return true //商品名称和商户名称一致，一定是同一笔交易
        }
        return false
    }

    /**
     * 合并重复账单,将bill1的信息合并到bill2
     * @param bill 账单1
     * @param bill2 账单2
     */
    private fun mergeRepeatBill(bill: BillInfoModel, bill2: BillInfoModel, context: Context) {
        //合并支付方式
        if (bill2.accountNameFrom.length < bill.accountNameFrom.length) {
            bill2.accountNameFrom = bill.accountNameFrom
        }
        if (bill2.accountNameTo.length < bill.accountNameTo.length) {
            bill2.accountNameTo = bill.accountNameTo
        }
        //合并商户信息
        if (bill2.shopName.length < bill.shopName.length) {
            bill2.shopName = bill.shopName
        }
        //合并商品信息
        if (bill2.shopItem.length < bill.shopItem.length) {
            bill2.shopItem = bill.shopItem
        }

        //合并商品信息
        if (bill2.extendData.length < bill.extendData.length) {
            bill2.extendData = bill.extendData
        }

        if (bill2.shopItem.isEmpty()) {
            bill2.shopItem = bill2.extendData
        }



        //最后重新生成备注
        setRemark(bill2, context)
    }

    /**
     * 账单分组，用于检查重复账单
     */
    fun groupBillInfo(
        billInfoModel: BillInfoModel,
        context: Context
    ): BillInfoModel? {
        Server.isRunOnMainThread()
        val settingBillRepeat =
            Db.get().settingDao().query(Setting.AUTO_GROUP)?.value?.toBoolean() ?: false
        if (!settingBillRepeat) return null
        //第一要素，金钱一致，时间在5分钟以内
        val bills =
            Db.get().billInfoDao().query(billInfoModel.money, billInfoModel.time - 5 * 60 * 1000)
        bills.forEach {
            if (checkRepeat(billInfoModel, it)) {
                billInfoModel.groupId = it.groupId
                mergeRepeatBill(billInfoModel, it, context)
                //更新到数据库
                Db.get().billInfoDao().update(it)
                return it
            }
        }
        return null
    }


    /**
     * 获取备注
     * @param billInfoModel 账单信息
     * @param context 上下文
     */
    fun setRemark(billInfoModel: BillInfoModel, context: Context) {
        Server.isRunOnMainThread()
        val settingBillRemark =
            Db.get().settingDao().query(Setting.NOTE_FORMAT)?.value ?: "【商户名称】 - 【商品名称】"
        billInfoModel.remark = settingBillRemark
            .replace("【商户名称】", billInfoModel.shopName)
            .replace("【商品名称】", billInfoModel.shopItem)
            .replace("【币种类型】", Currency.valueOf(billInfoModel.currency).name(context))
            .replace("【金额】", billInfoModel.money.toString())
            .replace("【分类】", billInfoModel.cateName)
            .replace("【账本】", billInfoModel.bookName)
            .replace("【来源】", billInfoModel.app)
            .replace("【渠道】", billInfoModel.channel)
    }

    /**
     * 设置默认账本
     */
    fun setBookName(billInfoModel: BillInfoModel) {
        Server.isRunOnMainThread()
        val name = billInfoModel.bookName
        if (name.isEmpty()) {
            billInfoModel.bookName = "默认账本"
        }
        val defaultBookName =
            Db.get().settingDao().query(Setting.DEFAULT_BOOK_NAME)?.value ?: "默认账本"

        if (name == "默认账本") {
            billInfoModel.bookName = defaultBookName
        }
    }
}
