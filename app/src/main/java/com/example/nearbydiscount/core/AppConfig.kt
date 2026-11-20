package com.example.nearbydiscount.core

import com.example.nearbydiscount.BuildConfig

object AppConfig {
    /** 演示数据仅在 debug 包且显式开启时可用，正式包强制关闭。 */
    val demoMode: Boolean get() = BuildConfig.DEBUG && BuildConfig.ENABLE_DEMO_DATA

    val radiusOptionsKm: List<Int> = listOf(1, 2, 3, 5)
    const val DEFAULT_RADIUS_KM = 3
    const val MAX_RADIUS_KM = 5
}
