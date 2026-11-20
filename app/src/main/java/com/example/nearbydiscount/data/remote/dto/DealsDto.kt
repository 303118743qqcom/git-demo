package com.example.nearbydiscount.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 优惠数据来自「自建桥接服务」，由服务端持有美团授权凭证调用官方接口。
 * 客户端不接触 appSecret / accessToken，避免密钥随 APK 泄漏。
 */
@Serializable
data class DealsBridgeResponse(
    val code: Int = -1,
    val message: String = "",
    val serverTime: Long = 0L,
    val promotions: List<PromotionDto> = emptyList()
)

@Serializable
data class PromotionDto(
    val id: String = "",
    val poiId: String = "",
    val merchantPoiId: String = "",
    val title: String = "",
    val discountText: String? = null,
    /** 官方活动状态：ACTIVE/ONLINE/VALID/EFFECTIVE/ENDED/DELETED/INVALID 等 */
    val status: String? = null,
    val enabled: Boolean = true,
    /** 时间字段支持 ISO-8601 字符串或秒/毫秒时间戳 */
    val startAt: String? = null,
    val endAt: String? = null,
    val startsAt: Long? = null,
    val endsAt: Long? = null,
    val stock: Int? = null,
    val claimed: Int? = null,
    val url: String? = null
) {
    val resolvedPoiId: String get() = poiId.ifBlank { merchantPoiId }
}
