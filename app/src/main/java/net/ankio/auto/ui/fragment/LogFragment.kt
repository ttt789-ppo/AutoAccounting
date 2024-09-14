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
package net.ankio.auto.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentLogBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.LogAdapter
import net.ankio.auto.ui.api.BasePageFragment
import net.ankio.auto.ui.models.ToolbarMenuItem
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.db.model.LogModel
import java.io.File


/**
 * 日志页面
 */
class LogFragment : BasePageFragment<LogModel>() {
    private lateinit var binding: FragmentLogBinding
    override val menuList: ArrayList<ToolbarMenuItem> =
        arrayListOf(
            ToolbarMenuItem(R.string.item_share, R.drawable.menu_icon_share) {
                runCatching {
                    val loadingUtils = LoadingUtils(requireActivity())
                    loadingUtils.show(R.string.loading_logs)

                    lifecycleScope.launch {
                        val cacheDir = requireContext().cacheDir
                        val file = File(cacheDir, "log.txt") // 确保这里路径是正确的，不要包含前置的 "/"

                        if (file.exists()) {
                            file.delete()
                        }
                        file.createNewFile()

                        // 循环20页日志，每页100条
                        for (i in 1..20) {
                            val list = LogModel.list(i, 100)
                            if (list.isEmpty()) {
                                break
                            }
                            file.appendText(list.joinToString("\n") {
                                "[${DateUtils.getTime(it.time)}] [ ${it.app} ] [ ${it.location} ] [ ${it.level} ] ${it.message}"
                            })
                        }

                        loadingUtils.close()

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/octet-stream"
                            val fileUri: Uri = FileProvider.getUriForFile(
                                requireContext(),
                                "${requireContext().packageName}.provider", // 使用动态获取包名
                                file
                            )
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                getString(R.string.share_file)
                            ) // 添加可选的文本标题
                        }

                        // 启动分享意图
                        requireContext().startActivity(
                            Intent.createChooser(
                                shareIntent,
                                getString(R.string.share_file)
                            )
                        )
                    }
                }.onFailure {
                    Logger.e("日志分享失败", it)
                }
            },
            ToolbarMenuItem(R.string.item_clear, R.drawable.menu_icon_clear) {
                runCatching {
                    lifecycleScope.launch {
                        LogModel.clear()
                        page = 1
                        loadDataInside()
                    }
                }.onFailure {
                    Logger.e("清除失败", it)
                }
            },
        )


    /**
     * 加载数据
     */
    override suspend fun loadData(callback: (resultData: List<LogModel>) -> Unit) {
        LogModel.list(page, pageSize).let { result ->
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    /**
     * 创建视图
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLogBinding.inflate(layoutInflater)
        statusPage = binding.statusPage
        val recyclerView = binding.statusPage.contentView
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = LogAdapter(pageData)
        scrollView = recyclerView!!
        loadDataEvent(binding.refreshLayout)
        return binding.root
    }


    override fun onResume() {
        super.onResume()
        statusPage.showLoading()
        loadDataInside()
    }

}
