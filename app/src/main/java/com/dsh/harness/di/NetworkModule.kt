package com.dsh.harness.di

import com.dsh.harness.BuildConfig
import com.dsh.harness.data.local.PrefsRepository
import com.dsh.harness.data.remote.HarnessApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** 网络层：OkHttp + Retrofit + kotlinx.serialization。 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // 流式响应需要长读超时
            .writeTimeout(60, TimeUnit.SECONDS)
            // 针对"浏览器可开、OkHttp 的 TLS 握手被 Connection reset by peer"：
            //  1) 现代 TLS 1.2/1.3（MODERN_TLS）
            //  2) 强制 HTTP/1.1，去掉 h2 ALPN，规避部分服务器/边缘对非浏览器 h2 协商直接 reset
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .protocols(listOf(Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("X-Client", "DeepSeekHarness/Android")
                    .build()
                chain.proceed(req)
            }
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
        prefs: PrefsRepository
    ): Retrofit {
        val baseUrl = runCatching {
            runBlocking { prefs.baseUrl.first() }
        }.getOrDefault(BuildConfig.HARNESS_BASE_URL)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): HarnessApi = retrofit.create(HarnessApi::class.java)
}
