package com.dsh.harness.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** 路由常量。 */
object Routes {
    const val MAIN = "main"
    const val PLUGIN_DETAIL = "plugin/{id}"
    const val PROVIDER_EDIT = "provider/{id}"
    const val SESSION_DETAIL = "session/{id}"

    fun pluginDetail(id: String) = "plugin/$id"
    fun providerEdit(id: String) = "provider/$id"
    fun sessionDetail(id: String) = "session/$id"
}

/** 全局导航图。主屏以外独立详情页都在这里。 */
@Composable
fun HarnessNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) { /* 主屏直接由 HarnessApp 渲染 */ }
        composable(Routes.PLUGIN_DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            com.dsh.harness.ui.screens.market.PluginDetailScreen(
                pluginId = id,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PROVIDER_EDIT) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            com.dsh.harness.ui.screens.settings.ProviderEditScreen(
                providerId = id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
