package com.example.nearbydiscount.data.remote

import com.example.nearbydiscount.data.remote.dto.DealsBridgeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DealsBridgeApi {
    /** 自建服务端接口：按门店 ID 批量返回官方授权优惠。 */
    @GET("promotions")
    suspend fun getPromotions(@Query("poiIds") poiIds: String): DealsBridgeResponse
}
