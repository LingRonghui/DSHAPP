package com.dsh.harness.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dsh.harness.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 【回退标记位】隐藏 WebView 仅作 HTTPS 传输的开关。
 * 若该方案无法解决问题，把这里改为 false，即整体回退到原生 OkHttp 传输，无任何残留影响。
 */
object ServerTransport {
    @Volatile var useWebView: Boolean = true
}

/**
 * 隐藏 WebView 仅做网络传输（UI 仍是纯原生 Compose，不是把网页嵌成界面）。
 *
 * 用途：部分服务器/网络会对"非浏览器 TLS 指纹"的握手直接 `Connection reset by peer`，
 * 而原生 OkHttp/系统 TLS 与 Chrome 的 ClientHello 指纹不同，浏览器能连、App 连不上。
 * 这里改让请求走系统 WebView 的 TLS/HTTP 栈（与浏览器同源），用同源 fetch 把原始响应体取回，
 * 与 DshRpcClient 的解析逻辑完全复用，因此 JSON 解析没有重写、也无接入偏差。
 */
@Singleton
class WebViewServerClient @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val main = Dispatchers.Main.immediate

    @Volatile private var origin: String = BuildConfig.HARNESS_BASE_URL.trimEnd('/')

    fun setOrigin(url: String?) {
        val u = url?.trim()?.trimEnd('/')
        if (!u.isNullOrBlank()) origin = u
    }

    private var wv: WebView? = null
    private var pageLoaded = false

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun ensureWebView(): WebView {
        wv?.let { return it }
        return withCtxMain {
            val v = WebView(ctx)
            v.settings.javaScriptEnabled = true
            v.settings.domStorageEnabled = true
            v.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            v.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            v.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    pageLoaded = true
                }
            }
            wv = v
            v
        }
    }

    private suspend fun <T> withCtxMain(block: () -> T): T = withContext(main) { block() }

    private suspend fun evalJs(js: String): String = withContext(main) {
        suspendCancellableCoroutine { cont ->
            val view = wv ?: return@suspendCancellableCoroutine
            view.evaluateJavascript(js) { v ->
                val raw = (v ?: "").let { s ->
                    if (s.length >= 2) {
                        try {
                            org.json.JSONTokener(s).nextValue().toString()
                        } catch (e: Exception) {
                            s
                        }
                    } else {
                        s
                    }
                }
                if (cont.isActive) cont.resume(raw)
            }
        }
    }

    /** 确保 WebView 已加载到服务器源（同源 fetch 需要 origin 一致，规避跨域 CORS）。 */
    suspend fun ensureOrigin(): Boolean {
        val originUrl = origin
        val v = ensureWebView()
        withCtxMain {
            if (v.url == null || v.url!!.isBlank() || !v.url!!.startsWith(originUrl)) {
                pageLoaded = false
                v.loadUrl(originUrl)
            }
        }
        repeat(50) {
            if (pageLoaded) {
                val o = runCatching { evalJs("location.origin") }.getOrDefault("")
                if (o == originUrl || o.startsWith(originUrl)) return true
            }
            delay(300)
        }
        return pageLoaded
    }

    /** 通过 WebView 的同源 fetch 发起 POST，取回原始响应体。 */
    suspend fun post(method: String, frame: String): String {
        if (!ensureOrigin()) throw IOException("webview transport: 服务器源未加载 ${origin}")
        val safeFrame = frame
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
        val js = """
            (function(){
              window.__rpc = {__done:0};
              try {
                fetch('$origin/api/$method',{method:'POST',headers:{'Content-Type':'application/json','X-Client':'DSH mobile/Android'},body:'$safeFrame'})
                  .then(function(r){return r.text();})
                  .then(function(t){window.__rpc.__done=1; window.__rpc.__body=t;})
                  .catch(function(e){window.__rpc.__done=1; window.__rpc.__err=String(e);});
              } catch(e) { window.__rpc.__done=1; window.__rpc.__err=String(e); }
            })();
        """.trimIndent()
        evalJs(js)
        repeat(120) {
            val done = runCatching { evalJs("String(window.__rpc ? window.__rpc.__done : 0)") }.getOrDefault("")
            if (done == "1") {
                val err = runCatching { evalJs("window.__rpc.__err ? String(window.__rpc.__err) : ''") }.getOrDefault("")
                if (err.isNotBlank()) throw IOException("webview fetch error: $err")
                return runCatching { evalJs("window.__rpc.__body ? String(window.__rpc.__body) : ''") }.getOrDefault("")
            }
            if (done.startsWith("ERR")) throw IOException(done)
            delay(250)
        }
        throw IOException("webview fetch timeout: $method")
    }
}