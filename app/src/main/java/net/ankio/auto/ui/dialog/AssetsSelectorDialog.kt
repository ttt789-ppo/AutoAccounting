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

package net.ankio.auto.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.DialogBookSelectBinding
import net.ankio.auto.ui.adapter.AssetsSelectorAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.StatusPage
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.db.model.AssetsModel

class AssetsSelectorDialog(
    private val context: Context,
    private val callback: (AssetsModel) -> Unit
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBookSelectBinding
    private val dataItems = mutableListOf<AssetsModel>()

    private lateinit var statusPage: StatusPage

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBookSelectBinding.inflate(inflater)
        statusPage = binding.statusPage
        val recyclerView = statusPage.contentView!!
        statusPage.contentView!!.layoutManager = LinearLayoutManager(context)

        cardView = binding.cardView
        cardViewInner = statusPage

        recyclerView.adapter = AssetsSelectorAdapter(dataItems) { item ->
            callback(item)
            dismiss()
        }
        loadData()
        return binding.root
    }


    private fun loadData() {
        statusPage.showLoading()
        lifecycleScope.launch {
            dataItems.clear()
            val newData = AssetsModel.list()

            if (newData.isEmpty()) {
                statusPage.showEmpty()
                return@launch
            }
            dataItems.addAll(newData)
            //对dataItems进行排序，type为NORMAL的排在前面,债权人排在最后
            dataItems.sortWith(compareByDescending<AssetsModel> {
                when (it.type) {
                    AssetsType.NORMAL -> 6
                    AssetsType.CREDIT -> 5
                    AssetsType.FINANCIAL -> 4
                    AssetsType.VIRTUAL -> 3
                    AssetsType.BORROWER -> 2
                    AssetsType.CREDITOR -> 1
                }
            }.thenBy {
                it.name
            }
            )

            statusPage.contentView!!.adapter?.notifyItemInserted(0)
            statusPage.showContent()
        }
    }

}
