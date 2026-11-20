package com.example.nearbydiscount.domain

import com.example.nearbydiscount.data.remote.dto.PromotionDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscountValidatorTest {

    private val now = 1_700_000_000_000L
    private val future = now + 3_600_000L
    private val past = now - 3_600_000L

    private fun dto(
        id: String,
        status: String? = "ACTIVE",
        end: String? = (now + 86_400_000L).toString(),
        start: String? = past.toString(),
        enabled: Boolean = true,
        stock: Int? = 10,
        claimed: Int? = 0
    ) = PromotionDto(
        id = id,
        poiId = "poi-1",
        title = "测试优惠",
        status = status,
        enabled = enabled,
        startAt = start,
        endAt = end,
        stock = stock,
        claimed = claimed
    )

    @Test
    fun `filters expired inactive disabled and sold out promotions`() {
        val items = listOf(
            dto("valid"),
            dto("ended", status = "ENDED"),
            dto("expired", end = past.toString()),
            dto("disabled", enabled = false),
            dto("soldout", stock = 0),
            dto("no-end", end = null)
        )

        val result = DiscountValidator.filterValid(items, now)

        assertEquals(listOf("valid"), result.map { it.id })
    }

    @Test
    fun `accepts future end time and rejects not yet started`() {
        val items = listOf(
            dto("future-end", end = future.toString()),
            dto("not-started", start = future.toString())
        )

        val result = DiscountValidator.filterValid(items, now)

        assertEquals(listOf("future-end"), result.map { it.id })
    }

    @Test
    fun `deduplicates by id and sorts by expiration`() {
        val items = listOf(
            dto("a", end = (future + 10_000).toString()),
            dto("a", end = future.toString()),
            dto("b", end = (future + 5_000).toString())
        )

        val result = DiscountValidator.filterValid(items, now)

        assertEquals(listOf("b", "a"), result.map { it.id })
    }

    @Test
    fun `handles iso 8601 end time`() {
        val result = DiscountValidator.filterValid(
            listOf(dto("iso", end = "2030-01-01T00:00:00Z")),
            now
        )
        assertTrue(result.isNotEmpty())
    }
}
