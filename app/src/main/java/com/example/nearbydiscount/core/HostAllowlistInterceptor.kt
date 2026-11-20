package com.example.nearbydiscount.core

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 网络出口白名单：只允许访问官方 API 域名与已授权的自建桥接域名。
 * 从代码层面杜绝访问任何非官方/未授权的数据源（爬虫、抓包回放、第三方聚合接口）。
 */
class HostAllowlistInterceptor(private val allowedHosts: Set<String>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host.lowercase()
        require(host in allowedHosts) { "未授权的请求域名：$host" }
        return chain.proceed(request)
    }
}
