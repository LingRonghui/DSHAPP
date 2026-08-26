package com.dsh.harness.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * 连接自检：在设备上分步实测到服务器的连通性，输出每步真实结果，用于精确定位
 * DNS / TCP / TLS1.2 / TLS1.3 / HTTP / POST 哪一层被重置或失败（不猜测，直接给证据）。
 */
object ServerDiagnostic {

    /** 每一步的测试结果。 */
    data class Step(val label: String, val ok: Boolean, val detail: String)

    suspend fun run(baseUrl: String): List<Step> = withContext(Dispatchers.IO) {
        val base = baseUrl.trim().trimEnd('/')
        val steps = mutableListOf<Step>()
        val url = try {
            URL(base)
        } catch (e: Exception) {
            return@withContext listOf(Step("URL 解析", false, "无法解析服务器地址：${e.message}"))
        }
        val host = url.host
        val port = if (url.port != -1) url.port else 443

        // 1. DNS
        try {
            val ips = InetAddress.getAllByName(host).map { it.hostAddress }.distinct()
            steps += Step("DNS 解析 $host", true, ips.joinToString(" "))
        } catch (e: Exception) {
            steps += Step("DNS 解析 $host", false, e.javaClass.simpleName + ": " + (e.message ?: "未知错误"))
            return@withContext steps
        }

        // 2. TCP 连接
        try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), 10000)
                steps += Step("TCP 连接 $host:$port", true, "连接成功")
            }
        } catch (e: Exception) {
            steps += Step("TCP 连接 $host:$port", false, e.javaClass.simpleName + ": " + (e.message ?: "未知错误"))
            return@withContext steps
        }

        // 3. TLS 1.2 握手
        try {
            val ctx = javax.net.ssl.SSLContext.getInstance("TLS")
            ctx.init(null, null, null)
            val factory = ctx.socketFactory
            (factory.createSocket() as SSLSocket).use { s ->
                s.connect(InetSocketAddress(host, port), 10000)
                s.enabledProtocols = arrayOf("TLSv1.2")
                s.startHandshake()
                val cn = (s.session.peerCertificates.firstOrNull() as? java.security.cert.X509Certificate)?.subjectX500Principal?.name ?: "?"
                steps += Step("TLS 1.2 握手", true, "成功，证书CN=${cn.take(80)}")
            }
        } catch (e: Exception) {
            steps += Step("TLS 1.2 握手", false, e.javaClass.simpleName + ": " + (e.message ?: "未知错误"))
        }

        // 4. TLS 1.3 握手
        try {
            val ctx = javax.net.ssl.SSLContext.getInstance("TLS")
            ctx.init(null, null, null)
            val factory = ctx.socketFactory
            (factory.createSocket() as SSLSocket).use { s ->
                s.connect(InetSocketAddress(host, port), 10000)
                s.enabledProtocols = arrayOf("TLSv1.3")
                s.startHandshake()
                val proto = s.session.protocol
                steps += Step("TLS 1.3 握手", true, "成功，协议=$proto")
            }
        } catch (e: Exception) {
            steps += Step("TLS 1.3 握手", false, e.javaClass.simpleName + ": " + (e.message ?: "未知错误"))
        }

        // 5. HTTP GET /（浏览器同款请求）
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val req = Request.Builder().url("$base/").build()
            client.newCall(req).execute().use { r ->
                val len = (r.body?.string()?.length ?: 0)
                steps += Step("HTTP GET /", r.isSuccessful || r.code in 300..499, "HTTP ${r.code}，正文约 $len 字符")
            }
        } catch (e: Exception) {
            steps += Step("HTTP GET /", false, e.javaClass.simpleName + ": " + (e.message ?: "未知错误"))
        }

        // 6. POST /api/workspace.list（真实 RPC）
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            val frame = """{"type":"client-request","rpcId":"diag","method":"workspace.list","payload":{}}"""
            val req = Request.Builder()
                .url("$base/api/workspace.list")
                .addHeader("Accept", "application/json")
                .post(frame.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(req).execute().use { r ->
                val body = r.body?.string().orEmpty()
                steps += Step("POST /api/workspace.list", r.isSuccessful, "HTTP ${r.code}，响应前 120 字符: ${body.take(120)}")
            }
        } catch (e: Exception) {
            steps += Step("POST /api/workspace.list", false, e.javaClass.simpleName + ": " + (e.message ?: "未知错误"))
        }

        steps
    }
}