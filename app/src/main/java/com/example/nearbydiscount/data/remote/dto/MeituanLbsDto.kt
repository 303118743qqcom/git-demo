package com.example.nearbydiscount.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 美团地图开放平台「周边搜索」响应。
 * 文档：https://lbs.meituan.com/support/search
 */
@Serializable
data class PoiSearchResponse(
    val status: Int = -1,
    val msg: String = "",
    val count: Int = 0,
    val pois: List<PoiEnvelope> = emptyList()
)

/**
 * 兼容平台不同版本的包装层级：
 * { pois: [{ entity: {...} }] } / { pois: [{ poi: {...} }] } / { pois: [{ id: ... }] }
 */
@Serializable
data class PoiEnvelope(
    val entity: PoiEntity? = null,
    val poi: PoiEnvelope? = null,
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val address: String? = null,
    val distance: String? = null,
    val location: String? = null
) {
    fun unwrap(): PoiEntity = entity
        ?: poi?.unwrap()
        ?: PoiEntity(id, name, type, address, distance, location)
}

@Serializable
data class PoiEntity(
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val address: String? = null,
    val distance: String? = null,
    val location: String? = null
)
