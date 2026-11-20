package com.example.nearbydiscount.domain

import com.example.nearbydiscount.data.remote.dto.PromotionDto
import com.example.nearbydiscount.domain.model.Promotion
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale

/**
 * 优惠有效期与活性校验。任何一项不通过都会被剔除，不会展示给用户：
 *  - 未启用 / 状态非有效
 *  - 库存已售罄
 *  - 缺少明确结束时间（无法证明仍在有效期内）
 *  - 结束时间早于或等于当前时间
 *  - 开始时间晚于当前时间
 *  - 相同 ID 的活动去重
 */
object DiscountValidator {

    private val ACTIVE_STATUS = setOf("ACTIVE", "ONLINE", "VALID", "EFFECTIVE", "PUBLISHED", "1", "TRUE")
    private val CHINA_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    fun filterValid(
        items: List<PromotionDto>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Promotion> = items.asSequence()
        .filter { it.id.isNotBlank() }
        .filter { it.enabled }
        .filter { it.status.isNullOrBlank() || it.status.trim().uppercase(Locale.US) in ACTIVE_STATUS }
        .filterNot { stockExhausted(it) }
        .mapNotNull { dto ->
            val start = dto.startsAt?.let(::normalizeEpoch) ?: parseTime(dto.startAt)
            val end = dto.endsAt?.let(::normalizeEpoch) ?: parseTime(dto.endAt)
            if (end == null || end <= nowMillis) return@mapNotNull null
            if (start != null && start > nowMillis) return@mapNotNull null
            Promotion(
                id = dto.id,
                title = dto.title.ifBlank { "限时优惠" },
                discountText = dto.discountText?.takeIf { it.isNotBlank() },
                validUntil = end,
                url = dto.url?.takeIf { it.isNotBlank() }
            )
        }
        .distinctBy { it.id }
        .sortedBy { it.validUntil }
        .toList()

    private fun stockExhausted(dto: PromotionDto): Boolean {
        val stock = dto.stock ?: return false
        val claimed = dto.claimed ?: 0
        return stock <= 0 || claimed >= stock
    }

    private fun normalizeEpoch(value: Long): Long =
        if (kotlin.math.abs(value) < 100_000_000_000L) value * 1_000L else value

    private fun parseTime(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        raw.toLongOrNull()?.let { return normalizeEpoch(it) }
        runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()?.let { return it }
        return runCatching { CHINA_FORMAT.parse(raw)?.time }.getOrNull()
    }
}
