package com.dsh.harness.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 实机网络单测：直接用本机 JVM + OkHttp 走真实 DshRpcClient 协议打线上实例，
 * 验证「协议封装 + 方法名 + DTO 解析」这套代码是否在真实网络下正确返回工作区。
 */
class DshRpcLiveTest {
    @Test
    fun liveWorkspaceList() = runBlocking {
        val http = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val rpc = DshRpcClient(http).apply { setBaseUrl("https://47.110.78.97.sslip.io/") }

        val value = rpc.callValue(DshRpcClient.Rpcs.workspaceList)
        val workspaces = rpc.parseWorkspaces(value)

        println("DshRpcLiveTest: got ${workspaces.size} workspaces")
        workspaces.forEach { println("  - ${it.workspaceId} | title=${it.title} | path=${it.path}") }
        assertTrue("expected >=1 workspace, got ${workspaces.size}", workspaces.isNotEmpty())
    }
}