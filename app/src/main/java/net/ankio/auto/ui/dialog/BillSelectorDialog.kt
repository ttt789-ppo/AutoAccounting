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

//import net.ankio.auto.ui.adapter.BillSelectorAdapter
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.databinding.DialogCategorySelectBinding
import net.ankio.auto.ui.adapter.BillSelectorAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.componets.StatusPage
import org.ezbook.server.db.model.BookBillModel

class BillSelectorDialog(
    private val context: Context,
    private val selectedBills: MutableList<String> = ArrayList(),
    private val callback: (MutableList<String>) -> Unit,
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogCategorySelectBinding
    private val dataItems = mutableListOf<BookBillModel>()
    private lateinit var statusPage: StatusPage
    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogCategorySelectBinding.inflate(inflater)
        val layoutManager = LinearLayoutManager(context)
        statusPage = binding.statusPage
        cardView = binding.cardView
        cardViewInner = binding.cardViewInner
        val recyclerView = statusPage.contentView!!
        recyclerView.layoutManager = layoutManager

        recyclerView.adapter = BillSelectorAdapter(dataItems, selectedBills)

        recyclerView.setPadding(0, 0, 0, 0)

        binding.button.setOnClickListener {
            Log.d("BillSelectorDialog", "selectedBills: $selectedBills")
            callback.invoke(selectedBills)
            dismiss()
        }

        lifecycleScope.launch {
            statusPage.showLoading()
            loadData()
        }


        return binding.root
    }


    private suspend fun loadData() {
        val list = BookBillModel.list()
        if (list.isEmpty()) {
            withContext(Dispatchers.Main) {
                statusPage.showEmpty()
            }
            return
        }
        dataItems.addAll(list)
        withContext(Dispatchers.Main) {
            statusPage.showContent()
            statusPage.contentView!!.adapter?.notifyItemRangeInserted(0, list.size)
        }
    }

}
