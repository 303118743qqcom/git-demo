package com.example.nearbydiscount.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nearbydiscount.R
import com.example.nearbydiscount.core.AppConfig
import com.example.nearbydiscount.domain.model.Merchant
import com.example.nearbydiscount.domain.model.Promotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NearbyScreen(
    state: NearbyUiState,
    onRadiusSelected: (Int) -> Unit,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_nearby)) },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Text(stringResource(R.string.refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.demoMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.demo_banner),
                        color = Color(0xFF9A5B00),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.blockedReason != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.compliance_blocked, state.blockedReason),
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                return@Scaffold
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = state.statusText.ifBlank { stringResource(R.string.locating) },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.radius_label), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppConfig.radiusOptionsKm.forEach { km ->
                        FilterChip(
                            selected = state.radiusKm == km,
                            onClick = { onRadiusSelected(km) },
                            label = { Text("$km km") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.keyword,
                    onValueChange = onKeywordChange,
                    label = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() })
                )
            }

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                state.needPermission -> CenterMessage(stringResource(R.string.permission_needed)) {
                    TextButton(onClick = onRequestPermission) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }

                state.isLoading && state.merchants.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.error != null && state.merchants.isEmpty() -> CenterMessage(
                    stringResource(R.string.error_prefix, state.error)
                ) {
                    TextButton(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                }

                state.merchants.isEmpty() -> CenterMessage(
                    "${stringResource(R.string.empty_title)}\n${stringResource(R.string.empty_subtitle)}"
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.merchants, key = { it.id }) { merchant ->
                        MerchantCard(merchant)
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String, action: @Composable (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF555555))
            if (action != null) {
                Spacer(Modifier.height(8.dp))
                action()
            }
        }
    }
}

@Composable
private fun MerchantCard(merchant: Merchant) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = merchant.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                merchant.distanceMeters?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(distanceText(it), color = Color(0xFFD70A2A), fontSize = 13.sp)
                }
            }
            val subtitle = listOfNotNull(merchant.category, merchant.address).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            if (merchant.promotions.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_valid_promotion),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999)
                )
            } else {
                merchant.promotions.forEach { promotion ->
                    PromotionRow(promotion)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun PromotionRow(promotion: Promotion) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFFFFF1F0), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = promotion.discountText ?: "优惠",
            color = Color(0xFFC8161D),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(promotion.title, fontSize = 14.sp)
            Text(
                text = stringResource(R.string.valid_until, formatTime(promotion.validUntil)),
                fontSize = 11.sp,
                color = Color(0xFF888888)
            )
        }
    }
}

private fun distanceText(meters: Int): String =
    if (meters >= 1_000) String.format(Locale.CHINA, "%.1f km", meters / 1_000f) else "$meters m"

private fun formatTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(millis))

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun NearbyScreenPreview() {
    MaterialTheme {
        NearbyScreen(
            state = NearbyUiState(statusText = "已定位 · 检索半径 3 km"),
            onRadiusSelected = {},
            onKeywordChange = {},
            onSearch = {},
            onRefresh = {},
            onRequestPermission = {}
        )
    }
}
