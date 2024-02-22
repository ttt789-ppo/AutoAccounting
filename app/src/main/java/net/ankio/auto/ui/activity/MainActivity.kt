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

package net.ankio.auto.ui.activity


import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tobey.dialogloading.DialogUtil
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.AboutDialogBinding
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.ui.fragment.BaseFragment
import net.ankio.auto.utils.ActiveUtils
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.BookSyncUtils
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.Github
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.SpUtils
import net.ankio.auto.utils.UpdateUtils
import rikka.html.text.toHtml


class MainActivity : BaseActivity() {

    //视图绑定
    private lateinit var binding: ActivityMainBinding

    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment

    private fun onLogin() {
        val uri = intent.data
        if (uri != null) {
            val dialog = DialogUtil.createLoadingDialog(this, getString(R.string.auth_waiting))

            val code = uri.getQueryParameter("code")

            Github.parseAuthCode(code, {

            }, {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                }
                DialogUtil.closeDialog(dialog)
            })
        }
    }


    private val barList = arrayListOf(
        arrayListOf(R.id.homeFragment, R.drawable.select_home, R.drawable.unselect_home),
        arrayListOf(R.id.dataFragment, R.drawable.select_data, R.drawable.unselect_data),
        arrayListOf(R.id.settingFragment, R.drawable.select_setting, R.drawable.unselect_setting),
        arrayListOf(R.id.ruleFragment, R.drawable.select_rule, R.drawable.unselect_rule),
        arrayListOf(R.id.orderFragment, R.drawable.select_order, R.drawable.unselect_order),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onLogin()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragmentContainerView = binding.navHostFragment
        bottomNavigationView = binding.bottomNavigation

        toolbarLayout = binding.toolbarLayout
        toolbar = binding.toolbar
        bottomNavigationView.addNavigationBarBottomPadding()
        scrollView = binding.scrollView
        navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?)!!


        val appBarConfiguration = AppBarConfiguration.Builder(*barList.map { it[0] }.toIntArray()).build()

        NavigationUI.setupWithNavController(
            toolbar,
            navHostFragment.navController,
            appBarConfiguration
        )
        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.navController)


        navHostFragment.navController.addOnDestinationChangedListener { controller, navDestination, _ ->

            // 重置底部导航栏图标
            barList.forEach {
                bottomNavigationView.menu.findItem(it[0]).setIcon(it[2])
                if (it[0] == navDestination.id){
                    bottomNavigationView.menu.findItem(it[0]).setIcon(it[1])
                }
            }


            // 如果目标不在barList里面则隐藏底部导航栏


            if (barList.any { it[0] == navDestination.id }) {
                bottomNavigationView.visibility = View.VISIBLE
            } else {
                bottomNavigationView.visibility = View.GONE
            }

        }

        onViewCreated()

    }


    override fun onViewCreated() {
      super.onViewCreated()
        //除了执行父页面的办法之外还要执行检查更新
      runCatching {
          checkAutoService()
          checkBookApp()
          checkUpdate()
      }.onFailure {
            Logger.e("检查更新失败",it)
      }
    }

    private fun checkAutoService(){
        runCatching {
            AppUtils.setService(AppUtils.getApplication())
        }.onFailure {
            //如果服务没启动，则跳转到服务未启动界面
            Logger.e("自动记账服务未连接",it)
            start<ServiceActivity>()
        }
    }

    private fun checkBookApp(){
        //判断是否设置了记账软件
        if (SpUtils.getString("bookApp", "").isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_book_app)
                .setMessage(R.string.msg_book_app)
                .setPositiveButton(R.string.sure_book_app) { _, _ ->
                    CustomTabsHelper.launchUrlOrCopy(this,getString(R.string.book_app_url))
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    //finish()
                }
                .show()
        }
    }


    override fun onResume() {
        super.onResume()
        ActiveUtils.onStartApp(this)
        lifecycleScope.launch {
            BookSyncUtils.sync(this@MainActivity)
        }
    }

    private fun checkUpdate() {
        val updateUtils = UpdateUtils()
        //检查版本更新
        if (SpUtils.getBoolean("checkAppUpdate", true)) {
            updateUtils.checkAppUpdate { version, log, date, download ->

            }
        }
        //检查规则更新
        if (SpUtils.getBoolean("checkRuleUpdate", true)) {
            updateUtils.checkRuleUpdate { version, log, date, category, rule ->
            }
        }
    }

    fun getBinding(): ActivityMainBinding {
        return binding
    }

    fun getNavController(): NavController {
        return navHostFragment.navController
    }


}