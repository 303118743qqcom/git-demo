package com.example.nearbydiscount.core

import android.os.Debug
import com.example.nearbydiscount.BuildConfig
import java.io.File

sealed interface ComplianceCheck {
    data object Passed : ComplianceCheck
    data class Blocked(val reason: String) : ComplianceCheck
}

/**
 * 合规前置校验：本应用只允许通过官方开放平台接口获取数据。
 * 任何 root / 调试器附加等篡改环境，直接拒绝发起请求。
 */
object ComplianceGuard {

    fun check(demoMode: Boolean): ComplianceCheck {
        // 正式包严格校验运行环境；debug 包放宽 root 与调试器检测，
        // 便于在模拟器或已 root 的测试机上调试。域名白名单、HTTPS 与官方 Key 校验在两种包中均生效。
        if (!BuildConfig.DEBUG) {
            if (Debug.isDebuggerConnected()) {
                return ComplianceCheck.Blocked("检测到调试器附加，已停止访问数据接口")
            }
            if (isRooted()) {
                return ComplianceCheck.Blocked("检测到设备环境不可信，已停止访问数据接口")
            }
        }
        if (!demoMode && BuildConfig.MEITUAN_LBS_KEY.isBlank()) {
            return ComplianceCheck.Blocked("尚未配置美团地图开放平台官方 Key")
        }
        return ComplianceCheck.Passed
    }

    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        if (paths.any { File(it).exists() }) return true

        return runCatching {
            ProcessBuilder(listOf("/system/bin/getprop", "ro.debuggable"))
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .use { it.readText().trim() == "1" }
        }.getOrDefault(false)
    }
}
