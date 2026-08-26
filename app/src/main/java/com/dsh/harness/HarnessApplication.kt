package com.dsh.harness

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口。注册 Hilt、通知渠道、全局未捕获异常处理。
 */
@HiltAndroidApp
class HarnessApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerNotificationChannels()
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler())
    }

    private fun registerNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                "Harness 后台任务",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Agent 后台运行状态通知" }
        )
        nm?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DOWNLOAD,
                "插件与产物下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "插件市场与文件下载进度" }
        )
    }

    companion object {
        const val CHANNEL_SERVICE = "harness.service"
        const val CHANNEL_DOWNLOAD = "harness.download"

        lateinit var instance: HarnessApplication
            private set
    }

    private class GlobalExceptionHandler :
        Thread.UncaughtExceptionHandler {
        private val previous: Thread.UncaughtExceptionHandler? =
            Thread.getDefaultUncaughtExceptionHandler()

        override fun uncaughtException(t: Thread, e: Throwable) {
            // 简单兜底，避免静默崩溃
            previous?.uncaughtException(t, e)
        }
    }
}
