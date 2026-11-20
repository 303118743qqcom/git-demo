package com.example.nearbydiscount.data.repository

import com.example.nearbydiscount.BuildConfig
import com.example.nearbydiscount.core.AppResult
import com.example.nearbydiscount.data.remote.MeituanLbsApi
import com.example.nearbydiscount.data.remote.dto.PoiEntity
import java.util.Locale

class NearbyRepository(private val api: MeituanLbsApi) {

    /** 调用美团地图开放平台的官方周边搜索接口，不带任何绕过或抓取逻辑。 */
    suspend fun searchNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        keyword: String
    ): AppResult<List<PoiEntity>> = try {
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

    private fun format(value: Double): String = String.format(Locale.US, "%.6f", value)
}
