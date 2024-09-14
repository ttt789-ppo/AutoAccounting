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
import net.ankio.auto.databinding.DialogBillMoreBinding
import net.ankio.auto.ui.api.BaseSheetDialog
//import net.ankio.auto.ui.adapter.OrderItemAdapter
import org.ezbook.server.db.model.BillInfoModel

class BillMoreDialog(
    private val context: Context,
    private val billInfoModel: BillInfoModel,
) :
    BaseSheetDialog(context) {
    private lateinit var binding: DialogBillMoreBinding
    private val dataItems = mutableListOf<BillInfoModel>()
    //private val adapter = OrderItemAdapter(dataItems, null, null,context)

    override fun onCreateView(inflater: LayoutInflater): View {
        binding = DialogBillMoreBinding.inflate(inflater)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager

        cardView = binding.cardView
        cardViewInner = binding.innerView

        //     binding.recyclerView.adapter = adapter

        return binding.root
    }

    override fun show(
        float: Boolean,
        cancel: Boolean,
    ) {
        super.show(float, cancel)
        lifecycleScope.launch {
            //   val config = AppUtils.getService().config()
            /*  BillInfo.getBillByGroup(billInfo.id).apply {
                  adapter.notifyConfig(config)
                  dataItems.clear()
                  dataItems.addAll(this)
                  adapter.notifyDataSetChanged()
              }*/
        }
    }
}
