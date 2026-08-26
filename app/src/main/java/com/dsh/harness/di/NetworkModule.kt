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
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 可动态切换 baseUrl 的 Retrofit 工厂：
 * - 用拦截器在每次请求时重写 URL（宿主为 PrefsRepository 的当前 baseUrl），
 *   避免设置页改地址后必须重建 Retrofit 单例。
 */
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
    fun provideOkHttp(prefs: PrefsRepository): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // 流式响应需要长读超时
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            // 动态 baseUrl 拦截器：用 Prefs 当前值重写请求 host
            .addInterceptor { chain ->
                val original = chain.request()
                val dynamicBase = runCatching {
                    runBlocking { prefs.baseUrl.first() }
                }.getOrDefault(BuildConfig.HARNESS_BASE_URL).trimEnd('/') + "/"
                val defaultBase = BuildConfig.HARNESS_BASE_URL.trimEnd('/') + "/"
                val newUrl: HttpUrl = try {
                    val baseHttp = runCatching { HttpUrl.get(dynamicBase) }
                        .getOrElse { HttpUrl.get(defaultBase) }
                    original.url.newBuilder()
                        .scheme(baseHttp.scheme)
                        .host(baseHttp.host)
                        .port(baseHttp.port)
                        .build()
                } catch (_: Throwable) {
                    original.url
                }
                val req: Request = original.newBuilder()
                    .url(newUrl)
                    .header("Accept", "application/json")
                    .header("X-Client", "DeepSeekHarness/Android")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json
    ): Retrofit {
        // baseUrl 用编译时默认值（仅用于 Retrofit 内部构建），真正的 host 由拦截器重写
        val defaultBase = BuildConfig.HARNESS_BASE_URL.trimEnd('/') + "/"
        return Retrofit.Builder()
            .baseUrl(defaultBase)
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
