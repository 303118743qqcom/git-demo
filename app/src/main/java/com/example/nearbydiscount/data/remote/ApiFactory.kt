package com.example.nearbydiscount.data.remote

import com.example.nearbydiscount.core.HostAllowlistInterceptor
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** 所有网络客户端集中创建，统一施加域名白名单与超时。 */
object ApiFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    private val contentType = "application/json; charset=utf-8".toMediaType()

    private fun client(vararg hosts: String): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HostAllowlistInterceptor(hosts.toSet()))
        .build()

    private fun retrofit(baseUrl: String, vararg hosts: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client(*hosts))
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    fun lbsApi(): MeituanLbsApi = retrofit(
        "https://lbsapi.meituan.com/",
        "lbsapi.meituan.com"
    ).create(MeituanLbsApi::class.java)

    /** 桥接地址必须是 HTTPS；未配置或非法时返回 null，界面将不展示优惠。 */
    fun bridgeApi(baseUrl: String): DealsBridgeApi? {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val url = runCatching { normalized.toHttpUrlOrNull() }.getOrNull() ?: return null
        if (url.scheme != "https" || url.host.isBlank()) return null
        return retrofit(normalized, url.host).create(DealsBridgeApi::class.java)
    }
}
