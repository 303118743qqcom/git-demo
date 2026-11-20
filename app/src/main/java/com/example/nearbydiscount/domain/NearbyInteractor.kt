package com.example.nearbydiscount.domain

import com.example.nearbydiscount.core.AppConfig
import com.example.nearbydiscount.core.AppResult
import com.example.nearbydiscount.core.getOrDefault
import com.example.nearbydiscount.data.remote.PromotionProvider
import com.example.nearbydiscount.data.repository.NearbyRepository
import com.example.nearbydiscount.domain.model.Merchant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** 组合「官方 POI 检索 + 授权优惠 + 有效期过滤 + 半径二次校验」。 */
class NearbyInteractor(
    private val repository: NearbyRepository,
    private val promotionProvider: PromotionProvider
) {

    suspend fun load(
        latitude: Double,
        longitude: Double,
        radiusKm: Int,
        keyword: String
    ): AppResult<List<Merchant>> {
        val radiusMeters = radiusKm.coerceIn(1, AppConfig.MAX_RADIUS_KM) * 1_000

        val pois = when (val result = repository.searchNearby(latitude, longitude, radiusMeters, keyword)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        if (pois.isEmpty()) return AppResult.Success(emptyList())

        val promotionMap = promotionProvider.load(pois.mapNotNull { it.id })
            .getOrDefault(emptyMap())
        val now = System.currentTimeMillis()

        val merchants = pois.mapNotNull { poi ->
            val id = poi.id ?: return@mapNotNull null
            // 接口已按 radius 过滤；客户端再校验一次，避免服务端参数被忽略时越界展示。
            val distance = poi.distance?.toDoubleOrNull()
                ?: poi.location?.let { haversineMeters(latitude, longitude, it) }
            if (distance != null && distance > radiusMeters + 5) return@mapNotNull null

            Merchant(
                id = id,
                name = poi.name.orEmpty(),
                category = poi.type,
                address = poi.address,
                distanceMeters = distance?.toInt(),
                promotions = DiscountValidator.filterValid(promotionMap[id].orEmpty(), now)
            )
        }.sortedBy { it.distanceMeters ?: Int.MAX_VALUE }

        return AppResult.Success(merchants)
    }

    /** location 字段格式为「经度,纬度」。 */
    private fun haversineMeters(lat: Double, lng: Double, location: String): Double? {
        val parts = location.split(",")
        if (parts.size != 2) return null
        val pLng = parts[0].trim().toDoubleOrNull() ?: return null
        val pLat = parts[1].trim().toDoubleOrNull() ?: return null

        val radius = 6_371_000.0
        val dLat = Math.toRadians(pLat - lat)
        val dLng = Math.toRadians(pLng - lng)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(pLat)) * sin(dLng / 2).pow(2)
        return 2 * radius * asin(sqrt(a))
    }
}
