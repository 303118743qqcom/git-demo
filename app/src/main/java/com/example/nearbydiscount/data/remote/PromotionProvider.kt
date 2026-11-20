package com.example.nearbydiscount.data.remote

import com.example.nearbydiscount.BuildConfig
import com.example.nearbydiscount.core.AppConfig
import com.example.nearbydiscount.core.AppResult
import com.example.nearbydiscount.data.remote.dto.DealsBridgeResponse
import com.example.nearbydiscount.data.remote.dto.PromotionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

interface PromotionProvider {
    suspend fun load(poiIds: List<String>): AppResult<Map<String, List<PromotionDto>>>
}

/** 正式路径：由服务端调用美团官方授权接口后返回数据。 */
class BridgePromotionProvider(private val api: DealsBridgeApi) : PromotionProvider {
    override suspend fun load(poiIds: List<String>): AppResult<Map<String, List<PromotionDto>>> {
        if (poiIds.isEmpty()) return AppResult.Success(emptyMap())
        return try {
            val response: DealsBridgeResponse = api.getPromotions(poiIds.joinToString(","))
            if (response.code != 0) {
                AppResult.Failure(response.message.ifBlank { "优惠服务返回异常" })
            } else {
                AppResult.Success(response.promotions.groupBy { it.resolvedPoiId })
            }
        } catch (e: Exception) {
            // 优惠服务不可用时不影响商家列表，但绝不降级为抓取等非官方手段
            AppResult.Failure(e.message?.ifBlank { "优惠服务不可用" } ?: "优惠服务不可用", e)
        }
    }
}

/** 未配置桥接服务时的安全空实现。 */
class EmptyPromotionProvider : PromotionProvider {
    override suspend fun load(poiIds: List<String>) = AppResult.Success(emptyMap<String, List<PromotionDto>>())
}

/**
 * 仅用于本地演示：构造包含有效、过期、失效、售罄的样本，用来验证过滤逻辑。
 * 正式包中 AppConfig.demoMode 恒为 false，不会启用。
 */
class DemoPromotionProvider : PromotionProvider {
    override suspend fun load(poiIds: List<String>) = withContext(Dispatchers.Default) {
        val now = Instant.now()
        val validUntil = now.plus(3, ChronoUnit.DAYS).toString()
        val expiredAt = now.minus(1, ChronoUnit.DAYS).toString()

        val data = poiIds.associateWith { id ->
            listOf(
                PromotionDto(
                    id = "demo-$id-valid",
                    poiId = id,
                    title = "到店立减 20 元（演示）",
                    discountText = "8.5折",
                    status = "ACTIVE",
                    startAt = now.minus(1, ChronoUnit.HOURS).toString(),
                    endAt = validUntil,
                    stock = 100,
                    claimed = 30
                ),
                PromotionDto(
                    id = "demo-$id-expired",
                    poiId = id,
                    title = "已过期活动（应被剔除）",
                    discountText = "5折",
                    status = "ACTIVE",
                    endAt = expiredAt,
                    stock = 50
                ),
                PromotionDto(
                    id = "demo-$id-ended",
                    poiId = id,
                    title = "已下线活动（应被剔除）",
                    discountText = "7折",
                    status = "ENDED",
                    endAt = validUntil,
                    stock = 50
                ),
                PromotionDto(
                    id = "demo-$id-soldout",
                    poiId = id,
                    title = "库存售罄（应被剔除）",
                    discountText = "6折",
                    status = "ACTIVE",
                    endAt = validUntil,
                    stock = 0,
                    claimed = 0
                )
            )
        }
        AppResult.Success(data)
    }
}

fun createPromotionProvider(): PromotionProvider = when {
    AppConfig.demoMode -> DemoPromotionProvider()
    BuildConfig.DEALS_BRIDGE_URL.isBlank() -> EmptyPromotionProvider()
    else -> ApiFactory.bridgeApi(BuildConfig.DEALS_BRIDGE_URL)
        ?.let(::BridgePromotionProvider)
        ?: EmptyPromotionProvider()
}
