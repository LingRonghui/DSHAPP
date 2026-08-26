package com.dsh.harness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dsh.harness.data.local.PrefsRepository
import kotlinx.coroutines.flow.first
import com.dsh.harness.ui.HarnessApp
import com.dsh.harness.ui.theme.HarnessTheme
import com.dsh.harness.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 应用唯一 Activity，承载整个 Compose 体系。
 * 实现 edge-to-edge、SplashScreen 与主题跟随。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: PrefsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        // 等待首个偏好值到达后再让 Splash 消失（很短）
        var ready = false
        splash.setKeepOnScreenCondition { !ready }

        setContent {
            val themeMode by produceState(initialValue = ThemeMode.System) {
                // first() 只取首个值即返回；collect{} 在 DataStore 无限流上会永远挂起，
                // 导致 ready 永不置真、启动屏永久停留（表现为 App 打不开）。
                value = try { prefs.themeMode.first() } catch (e: Exception) { ThemeMode.System }
                ready = true
            }
            HarnessTheme(themeMode = themeMode) {
                HarnessApp()
            }
        }
    }
}
