package com.example.nearbydiscount.data.repository

import com.example.nearbydiscount.BuildConfig
import com.example.nearbydiscount.core.AppResult
import com.example.nearbydiscount.data.remote.MeituanLbsApi
import com.example.nearbydiscount.data.remote.dto.PoiEntity
import kotlin.math.cos
import kotlin.math.sin
import java.util.Locale

class NearbyRepository(
    private val api: MeituanLbsApi,
    private val demoMode: Boolean = false
) {

    /** 调用美团地图开放平台的官方周边搜索接口，不带任何绕过或抓取逻辑。 */
    suspend fun searchNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        keyword: String
    ): AppResult<List<PoiEntity>> {
        // 演示模式：仅 debug 包且显式开启，返回内置样本商家，不发起任何网络请求。
        if (demoMode) return AppResult.Success(demoPois(latitude, longitude, radiusMeters, keyword))
        return try {
            val response = api.searchNearby(
                key = BuildConfig.MEITUAN_LBS_KEY,
                location = "${format(longitude)},${format(latitude)}",
                radius = radiusMeters,
                keywords = keyword.ifBlank { null }
            )
            when (response.status) {
                200 -> AppResult.Success(
                    response.pois
                        .map { it.unwrap() }
                        .filter { !it.id.isNullOrBlank() && !it.name.isNullOrBlank() }
                        .distinctBy { it.id }
                )
                else -> AppResult.Failure(response.msg.ifBlank { "接口返回状态 ${response.status}" })
            }
        } catch (e: Exception) {
            AppResult.Failure(e.message?.ifBlank { "网络请求失败" } ?: "网络请求失败", e)
        }
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.6f", value)

    /**
     * 仅用于本地演示的样本商家：以传入坐标为中心，按米级偏移生成带真实经纬度的 POI。
     * 不写死 distance，交由 NearbyInteractor 的 Haversine 按半径二次校验，
     * 因此切换 1/2/3/5 km 会真实改变列表数量，便于演示半径筛选。
     */
    private fun demoPois(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        keyword: String
    ): List<PoiEntity> {
        data class Seed(
            val id: String,
            val name: String,
            val type: String,
            val address: String,
            val northMeters: Double,
            val eastMeters: Double
        )

        val seeds = listOf(
            Seed("demo-poi-001", "星巴克咖啡（国贸店）", "咖啡厅", "建国门外大街 1 号", 180.0, 120.0),
            Seed("demo-poi-002", "海底捞火锅（国贸店）", "火锅", "建国路 88 号 SKP 6 层", -260.0, 340.0),
            Seed("demo-poi-003", "瑞幸咖啡（光华路店）", "咖啡厅", "光华路 SOHO 一层", 720.0, -410.0),
            Seed("demo-poi-004", "西贝莜面村（朝阳店）", "中餐厅", "朝阳大悦城 5 层", 1400.0, 900.0),
            Seed("demo-poi-005", "盒马鲜生（双井店）", "超市", "双井桥富力广场 B1", -2100.0, 1300.0),
            Seed("demo-poi-006", "万达影城（CBD 店）", "电影院", "万达广场 4 层", 3100.0, -1800.0),
            Seed("demo-poi-007", "全聚德烤鸭（前门店）", "中餐厅", "前门大街 30 号", -4300.0, -2600.0),
            Seed("demo-poi-008", "便利蜂（永安里店）", "便利店", "永安里地铁站旁", 4900.0, 800.0)
        )

        val kw = keyword.trim()
        val latPerMeter = 1.0 / 111_320.0
        val lngPerMeter = 1.0 / (111_320.0 * cos(Math.toRadians(latitude)))

        return seeds.mapNotNull { seed ->
            if (kw.isNotBlank() &&
                !seed.name.contains(kw, true) &&
                !seed.type.contains(kw, true) &&
                !seed.address.contains(kw, true)
            ) return@mapNotNull null

            val lat = latitude + seed.northMeters * latPerMeter
            val lng = longitude + seed.eastMeters * lngPerMeter
            val dist = distanceMeters(latitude, longitude, lat, lng)
            if (dist > radiusMeters + 5) return@mapNotNull null

            PoiEntity(
                id = seed.id,
                name = seed.name,
                type = seed.type,
                address = seed.address,
                distance = dist.toInt().toString(),
                location = "${format(lng)},${format(lat)}"
            )
        }
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * kotlin.math.asin(kotlin.math.sqrt(a))
    }
}
