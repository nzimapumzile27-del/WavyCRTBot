package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssetType
import com.example.model.RsiDivergenceEvent
import com.example.model.TradeDirection
import com.example.viewmodel.TradingViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RsiDivergencePanel(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val rsiDivergences by viewModel.rsiDivergences.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Filter events based on asset class
    val filteredEvents = remember(rsiDivergences, selectedFilter) {
        when (selectedFilter) {
            "CRYPTO" -> rsiDivergences.filter { it.asset.category == "Crypto" }
            "INDICES" -> rsiDivergences.filter { it.asset.category == "Indices" }
            "FOREX" -> rsiDivergences.filter { it.asset.category == "Forex" }
            else -> rsiDivergences
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rsi_divergence_panel"),
        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, Slate700),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with titles and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Emerald400, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RSI Divergence Confluences",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Real-time key confluences for Turtle Soup sweeps",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Control Actions with M3 standard sizes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.markAllDivergencesAsRead() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("mark_read_divergences_btn"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Slate400)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark all as read",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearDivergencesLog() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clear_divergences_btn"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Slate400)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear divergence logs",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Asset class filter tabs/chips with 48dp minimum click areas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf("ALL", "CRYPTO", "INDICES", "FOREX")
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    val bgCol = if (isSelected) Indigo500 else Slate700.copy(alpha = 0.4f)
                    val contentCol = if (isSelected) Color.White else Slate400

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgCol)
                            .clickable { selectedFilter = filter }
                            .testTag("filter_chip_$filter"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = contentCol,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notification content area
            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Slate900.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No divergence confluences detected",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Real-time alerts stream as market simulator ticks",
                            color = Slate600,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    filteredEvents.take(4).forEach { event ->
                        DivergenceEventRow(event)
                    }
                }
            }
        }
    }
}

@Composable
fun DivergenceEventRow(event: RsiDivergenceEvent) {
    val isBullish = event.direction == TradeDirection.BUY
    val accentColor = if (isBullish) Emerald400 else Rose400
    val borderCol = if (event.isRead) Slate700.copy(alpha = 0.4f) else accentColor.copy(alpha = 0.4f)
    val bgCol = if (event.isRead) Slate900.copy(alpha = 0.3f) else Slate900.copy(alpha = 0.7f)

    val timeString = remember(event.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            // Match GMT+2 SAST timezone
            timeZone = java.util.TimeZone.getTimeZone("GMT+2")
        }
        sdf.format(Date(event.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgCol)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Asset Info & Divergence Type Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Asset Icon Box
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBullish) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = if (isBullish) "Bullish Divergence" else "Bearish Divergence",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = event.asset.displayName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Time elapsed/badge
                        Text(
                            text = timeString,
                            color = Slate400,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (!event.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(accentColor, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isBullish) "BULLISH RSI DIVERGENCE (BUY CONFLUENCE)" else "BEARISH RSI DIVERGENCE (SELL CONFLUENCE)",
                        color = accentColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Right: Detailed numeric values
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Price shift representation
                Text(
                    text = if (isBullish) {
                        String.format(Locale.US, "Price: %,.4f ↘ %,.4f (Lower Low)", event.priceBefore, event.priceAfter)
                    } else {
                        String.format(Locale.US, "Price: %,.4f ↗ %,.4f (Higher High)", event.priceBefore, event.priceAfter)
                    },
                    color = Slate200,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
                
                // RSI shift representation
                Text(
                    text = if (isBullish) {
                        String.format(Locale.US, "RSI: %.1f ↗ %.1f (Higher Low)", event.rsiBefore, event.rsiAfter)
                    } else {
                        String.format(Locale.US, "RSI: %.1f ↘ %.1f (Lower High)", event.rsiBefore, event.rsiAfter)
                    },
                    color = accentColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * A beautiful sliding overlay dialog listing the complete historic log of RSI Divergences.
 */
@Composable
fun RsiDivergencesLogDialog(
    viewModel: TradingViewModel,
    onDismissRequest: () -> Unit
) {
    val rsiDivergences by viewModel.rsiDivergences.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(contentColor = Indigo400)
            ) {
                Text("Close Terminal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { 
                    viewModel.clearDivergencesLog()
                    onDismissRequest()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Rose400)
            ) {
                Text("Clear All Logs")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Indigo400
                )
                Text(
                    text = "RSI Divergences Logs",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "This log monitors the divergence pivots between price Action and RSI across indices, crypto, and currency assets, serving as a vital confluence filter for the CRT + FVG strategy.",
                    color = Slate400,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (rsiDivergences.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No divergence records located.",
                            color = Slate600,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(rsiDivergences) { event ->
                            DivergenceEventRow(event)
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Slate800,
        tonalElevation = 6.dp
    )
}
