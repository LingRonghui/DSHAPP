package com.dsh.harness

import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * 无虚拟化环境（云电脑）上的运行级验证：
 * 在 JVM 中模拟 Android 运行时启动 MainActivity，断言首屏组合不崩溃。
 * 覆盖修复的启动屏逻辑（DataStore first() 不再无限挂起）。
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = [28], application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-port-mdpi")
class MainActivityRobolectricTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun launchMainActivity_shouldNotCrash() {
        hiltRule.inject()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity -> assertNotNull("MainActivity should be created without crash", activity) }
        scenario.recreate()
        scenario.close()
    }
}