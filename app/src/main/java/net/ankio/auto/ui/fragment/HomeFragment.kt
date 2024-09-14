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

package net.ankio.auto.ui.fragment

import android.content.BroadcastReceiver
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.launch
import net.ankio.auto.App
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.broadcast.LocalBroadcastHelper
import net.ankio.auto.common.ActiveInfo
import net.ankio.auto.common.ServerInfo
import net.ankio.auto.databinding.AboutDialogBinding
import net.ankio.auto.databinding.FragmentHomeBinding
import net.ankio.auto.storage.ConfigUtils
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.dialog.AppDialog
import net.ankio.auto.ui.dialog.AssetsSelectorDialog
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.dialog.CategorySelectorDialog
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.ui.models.ToolbarMenuItem
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.update.AppUpdate
import net.ankio.auto.update.RuleUpdate
import net.ankio.auto.utils.CustomTabsHelper
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.BookNameModel
import org.ezbook.server.db.model.CategoryModel
import org.ezbook.server.db.model.SettingModel
import rikka.html.text.toHtml

/**
 * 主页
 */
class HomeFragment : BaseFragment() {
    private lateinit var binding: FragmentHomeBinding
    override val menuList: ArrayList<ToolbarMenuItem> =
        arrayListOf(
            ToolbarMenuItem(R.string.title_log, R.drawable.menu_item_log) {
                it.navigate(R.id.logFragment)
            },
            ToolbarMenuItem(R.string.title_setting, R.drawable.menu_item_setting) {
                it.navigate(R.id.systemSettingFragment)
            },
            ToolbarMenuItem(R.string.title_more, R.drawable.menu_item_more) {
                val binding =
                    AboutDialogBinding.inflate(LayoutInflater.from(requireContext()), null, false)
                binding.sourceCode.movementMethod = LinkMovementMethod.getInstance()
                binding.sourceCode.text =
                    getString(
                        R.string.about_view_source_code,
                        "<b><a href=\"https://github.com/AutoAccountingOrg/AutoAccounting\">GitHub</a></b>",
                    ).toHtml()
                binding.versionName.text = BuildConfig.VERSION_NAME
                MaterialAlertDialogBuilder(requireContext())
                    .setView(binding.root)
                    .show()
            },
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        bindingActiveEvents()

        bindBookAppEvents()

        bindRuleEvents()

        bindingCommunicationEvents()

        // 卡片部分颜色设置

        val cards = listOf(
            binding.infoCard,
            binding.groupCard,
            binding.ruleCard,
        )
        val color = SurfaceColors.SURFACE_1.getColor(requireContext())
        cards.forEach { it.setCardBackgroundColor(color) }

        scrollView = binding.scrollView

        // 检查记账软件
        checkBookApp()

        // app启动时检查自动记账服务的连通性
        checkAutoService()

        // 检查软件和规则更新
        checkUpdate()

        return binding.root
    }

    /**
     * 检查自动记账服务
     */
    private fun checkAutoService() {

        lifecycleScope.launch {
            if (!ServerInfo.isServerStart()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.title_cant_connect_service)
                    .setMessage(ServerInfo.getServerErrorMsg(requireContext()))
                    .show()
            }
        }
    }


    /**
     * 检查记账软件
     */
    private fun checkBookApp() {
        if (ConfigUtils.getString(Setting.BOOK_APP_ID, "").isEmpty()) {
            AppDialog(requireContext()).show(cancel = BuildConfig.DEBUG)
        }
    }

    /**
     * 刷新UI
     */
    private fun refreshUI() {
        bindActiveUI()
        bindBookAppUI()
        bindRuleUI()
    }

    /**
     * 绑定记账软件数据部分的UI
     */
    private fun bindBookAppUI() {
        binding.book.visibility =
            if (ConfigUtils.getBoolean(Setting.SETTING_BOOK_MANAGER,true)) View.VISIBLE else View.GONE
        binding.assets.visibility =
            if (ConfigUtils.getBoolean(Setting.SETTING_ASSET_MANAGER,true)) View.VISIBLE else View.GONE
        ConfigUtils.getString(Setting.BOOK_APP_ID, "").apply {
            if (this.isEmpty()) {
                binding.bookApp.text = getString(R.string.no_setting)
            } else {
                App.getAppInfoFromPackageName(this)?.apply {
                    binding.bookApp.text = this[0] as String
                }
            }
        }


        val bookName = ConfigUtils.getString(Setting.DEFAULT_BOOK_NAME, "")
        if (bookName.isEmpty()) {
            lifecycleScope.launch {
                val book = BookNameModel.getFirstBook()
                ConfigUtils.putString(Setting.DEFAULT_BOOK_NAME, book.name)
                SettingModel.set(Setting.DEFAULT_BOOK_NAME, book.name)
                binding.defaultBook.text = book.name
            }
        } else {
            binding.defaultBook.text = bookName
        }
    }

    /**
     * 绑定激活部分的UI
     */
    private fun bindActiveUI() {
        val colorPrimary =
            App.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary)

        if (!ActiveInfo.isModuleActive()) {
            setActive(
                SurfaceColors.SURFACE_3.getColor(requireContext()),
                colorPrimary,
                R.drawable.home_active_error,
            )
        } else {
            setActive(
                colorPrimary,
                App.getThemeAttrColor(
                    com.google.android.material.R.attr.colorOnPrimary,
                ),
                R.drawable.home_active_success,
            )
        }
    }


    /**
     * 绑定规则部分的UI
     */
    private fun bindRuleUI() {
        val ruleVersion = ConfigUtils.getString(Setting.RULE_VERSION, "None")
        binding.ruleVersion.text = ruleVersion
    }

    /**
     * 本地广播
     */
    private lateinit var broadcastReceiver: BroadcastReceiver

    /**
     * 绑定规则部分的事件
     */
    private fun bindRuleEvents() {

        broadcastReceiver =
            LocalBroadcastHelper.registerReceiver(LocalBroadcastHelper.ACTION_UPDATE_FINISH) { a, b ->
                refreshUI()
            }

        binding.categoryMap.setOnClickListener {
            findNavController().navigate(R.id.categoryMapFragment)
        }

        binding.categoryEdit.setOnClickListener {
            findNavController().navigate(R.id.categoryRuleFragment)
        }

        binding.checkRuleUpdate.setOnClickListener {
            ToastUtils.info(R.string.check_update)
            lifecycleScope.launch {
                checkRuleUpdate(true)
            }
        }
        //长按强制更新
        binding.checkRuleUpdate.setOnLongClickListener {
            ConfigUtils.putString(Setting.RULE_VERSION, "")
            ToastUtils.info(R.string.check_update)
            lifecycleScope.launch {
                checkRuleUpdate(true)
            }
            true
        }

    }

    /**
     * 检查规则更新
     */
    private suspend fun checkRuleUpdate(showResult: Boolean) {
        val ruleUpdate = RuleUpdate(requireContext())
        if (ruleUpdate.check(showResult)) {
            UpdateDialog(requireActivity(), ruleUpdate).show(false)
        }
    }

    /**
     * 检查应用更新
     */
    private suspend fun checkAppUpdate() {
        val appUpdate = AppUpdate(requireContext())
        if (appUpdate.check(false)) {
            UpdateDialog(requireActivity(), appUpdate).show(cancel = true)
        }
    }

    /**
     * 销毁时注销广播
     */
    override fun onDestroy() {
        super.onDestroy()
        if (::broadcastReceiver.isInitialized) {
            LocalBroadcastHelper.unregisterReceiver(broadcastReceiver)
        }
        if (::broadcastReceiverBook.isInitialized) {
            LocalBroadcastHelper.unregisterReceiver(broadcastReceiverBook)
        }
    }

    private lateinit var broadcastReceiverBook: BroadcastReceiver

    /**
     * 绑定记账软件数据部分的事件
     */
    private fun bindBookAppEvents() {
        /**
         * 获取主题Context，部分弹窗样式不含M3主题
         */
        val themeContext = App.getThemeContext(requireContext())
        broadcastReceiverBook =
            LocalBroadcastHelper.registerReceiver(LocalBroadcastHelper.ACTION_APP_CHANGED) { a, b ->
                bindBookAppUI()
            }
        binding.bookAppContainer.setOnClickListener {
            AppDialog(requireContext()).show(false)
        }
        // 资产映射
        binding.map.setOnClickListener {
            // 切换到MapFragment
            findNavController().navigate(R.id.assetMapFragment)
        }
        // 资产管理（只读）
        binding.readAssets.setOnClickListener {
            AssetsSelectorDialog(themeContext) {
                Logger.d("选择的资产是：${it.name}")
            }.show(cancel = true)
        }
        // 账本数据（只读）
        binding.book.setOnClickListener {
            BookSelectorDialog(themeContext) { book, _ ->
                Logger.d("选择的账本是：${book.name}")
                // defaultBook
                ConfigUtils.putString(Setting.DEFAULT_BOOK_NAME, book.name)
                lifecycleScope.launch {
                    SettingModel.set(Setting.DEFAULT_BOOK_NAME, book.name)
                }
                refreshUI()
            }.show(cancel = true)
        }
        // 分类数据（只读）
        binding.readCategory.setOnClickListener {
            BookSelectorDialog(themeContext, true) { book, type ->
                CategorySelectorDialog(
                    themeContext,
                    book.remoteId,
                    type
                ) { categoryModel1: CategoryModel?, categoryModel2: CategoryModel? ->
                    Logger.d("选择的分类是：${categoryModel1?.name ?: ""} - ${categoryModel2?.name ?: ""}")
                }.show(cancel = true)
            }.show(cancel = true)
        }

    }

    /**
     * 激活页面的事件
     */
    private fun bindingActiveEvents() {
        binding.active.setOnClickListener {

            if (!ActiveInfo.isModuleActive()) {
                //TODO 跳转帮助文档
            }

            //  findNavController().navigate(R.id.serviceFragment)
        }
    }

    /**
     * 自动记账讨论社区
     */
    private fun bindingCommunicationEvents() {
        binding.msgGeekbar.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.geekbar_uri))
        }

        binding.msgTelegram.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.telegram_url))
        }

        binding.msgQq.setOnClickListener {
            CustomTabsHelper.launchUrlOrCopy(requireContext(), getString(R.string.qq_url))
        }
    }

    /**
     * onResume时刷新UI
     */
    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    /**
     * 检查更新
     */
    private fun checkUpdate(showResult: Boolean = false) {
        if (ConfigUtils.getBoolean(Setting.CHECK_RULE_UPDATE, true)) {
            lifecycleScope.launch {
                checkRuleUpdate(showResult)
            }
        }
        if (ConfigUtils.getBoolean(Setting.CHECK_APP_UPDATE, true)) {
            lifecycleScope.launch {
                checkAppUpdate()
            }
        }
    }

    /**
     * 设置激活状态
     */
    private fun setActive(
        @ColorInt backgroundColor: Int,
        @ColorInt textColor: Int,
        @DrawableRes drawable: Int,
    ) {
        binding.active.setBackgroundColor(backgroundColor)
        binding.imageView.setImageDrawable(
            AppCompatResources.getDrawable(
                requireActivity(),
                drawable,
            ),
        )
        val versionName = BuildConfig.VERSION_NAME
        val names = versionName.split(" - ")
        binding.msgLabel.text = names[0].trim()
        binding.msgLabel2.text = ActiveInfo.getFramework()
        binding.imageView.setColorFilter(textColor)
        binding.msgLabel.setTextColor(textColor)
        binding.msgLabel2.setTextColor(textColor)
    }
}
