package com.example.nearbydiscount.domain.model

data class Promotion(
    val id: String,
    val title: String,
    val discountText: String?,
    val validUntil: Long,
    val url: String?
)

data class Merchant(
    val id: String,
    val name: String,
    val category: String?,
    val address: String?,
    val distanceMeters: Int?,
    val promotions: List<Promotion>
)
