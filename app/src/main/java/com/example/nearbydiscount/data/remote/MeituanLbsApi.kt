package com.example.nearbydiscount.data.remote

import com.example.nearbydiscount.data.remote.dto.PoiSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MeituanLbsApi {
    /** 官方周边搜索：https://lbsapi.meituan.com/v1/search/nearby */
    @GET("v1/search/nearby")
    suspend fun searchNearby(
        @Query("key") key: String,
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("keywords") keywords: String? = null,
        @Query("page") page: Int = 1,
        @Query("pagesize") pageSize: Int = 20,
        @Query("city") city: String? = null
    ): PoiSearchResponse
}
