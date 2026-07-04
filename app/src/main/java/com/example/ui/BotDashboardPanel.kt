package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StrategyConfig
import com.example.model.StrategySetup
import com.example.model.TradeDirection
import com.example.ui.theme.*
import com.example.viewmodel.TradingViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BotDashboardTab(viewModel: TradingViewModel) {
    val strategyConfig by viewModel.strategyConfig.collectAsState()
    val recentSetups by viewModel.recentSetups.collectAsState()
    val isBotActive by viewModel.isBotActive.collectAsState()
    
    var fvgLookback by remember(strategyConfig) { mutableStateOf(strategyConfig.fvgLookback.toFloat()) }
    var turtleLookback by remember(strategyConfig) { mutableStateOf(strategyConfig.turtleLookback.toFloat()) }
    var turtleWickThresholdPct by remember(strategyConfig) { mutableStateOf(strategyConfig.turtleWickThresholdPct.toFloat()) }
    var timeFrameMinutes by remember(strategyConfig) { mutableStateOf(strategyConfig.timeFrameMinutes) }
    var enableBosConfirmation by remember(strategyConfig) { mutableStateOf(strategyConfig.enableBosConfirmation) }
    var enableObMitigation by remember(strategyConfig) { mutableStateOf(strategyConfig.enableObMitigation) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp)
            .testTag("bot_dashboard_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Bot Controller Header Panel
        item {
            BotControlHeader(
                isBotActive = isBotActive,
                onToggleBot = { viewModel.toggleBot() },
                recentSetupsCount = recentSetups.size
            )
        }

        // Section 2: Strategy Configuration Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("strategy_config_panel"),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Config Icon",
                                tint = Indigo400,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Strategy Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                // Reset to default
                                val default = StrategyConfig()
                                fvgLookback = default.fvgLookback.toFloat()
                                turtleLookback = default.turtleLookback.toFloat()
                                turtleWickThresholdPct = default.turtleWickThresholdPct.toFloat()
                                timeFrameMinutes = default.timeFrameMinutes
                                enableBosConfirmation = default.enableBosConfirmation
                                enableObMitigation = default.enableObMitigation
                                viewModel.updateStrategyConfig(default)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Divider(color = Slate700, modifier = Modifier.padding(vertical = 12.dp))

                    // 1. FVG Search Lookback Period
                    Text(
                        text = "FVG Search Lookback: ${fvgLookback.toInt()} Candles",
                        color = Slate300,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = fvgLookback,
                        onValueChange = { fvgLookback = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Indigo400,
                            activeTrackColor = Indigo500,
                            inactiveTrackColor = Slate600
                        ),
                        modifier = Modifier.testTag("fvg_lookback_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Turtle Entry Threshold (Lookback)
                    Text(
                        text = "Turtle Swing extreme Lookback: ${turtleLookback.toInt()} Candles",
                        color = Slate300,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = turtleLookback,
                        onValueChange = { turtleLookback = it },
                        valueRange = 5f..40f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = Indigo400,
                            activeTrackColor = Indigo500,
                            inactiveTrackColor = Slate600
                        ),
                        modifier = Modifier.testTag("turtle_lookback_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Turtle Soup Wick Threshold %
                    Text(
                        text = "Turtle Soup Wick Buffer: ${String.format(Locale.US, "%.2f", turtleWickThresholdPct)}x Volatility",
                        color = Slate300,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = turtleWickThresholdPct,
                        onValueChange = { turtleWickThresholdPct = it },
                        valueRange = 0.05f..0.50f,
                        colors = SliderDefaults.colors(
                            thumbColor = Indigo400,
                            activeTrackColor = Indigo500,
                            inactiveTrackColor = Slate600
                        ),
                        modifier = Modifier.testTag("turtle_threshold_slider")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Time Frame Selector
                    Text(
                        text = "Bot Execution Time Frame",
                        color = Slate300,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 15, 60, 240).forEach { mins ->
                            val label = when (mins) {
                                5 -> "M5"
                                15 -> "M15"
                                60 -> "H1"
                                240 -> "H4"
                                else -> "M15"
                            }
                            val isSelected = timeFrameMinutes == mins
                            val bg = if (isSelected) Indigo500 else Slate700
                            val borderCol = if (isSelected) Indigo400 else Slate600
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .background(bg, RoundedCornerShape(8.dp))
                                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                    .clickable { timeFrameMinutes = mins }
                                    .testTag("timeframe_${label}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Switches: BoS & OB confluences
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require BoS (Break of Structure)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ensures price prints a valid BOS on lower time frame",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = enableBosConfirmation,
                            onCheckedChange = { enableBosConfirmation = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Indigo500,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate600
                            ),
                            modifier = Modifier.testTag("bos_switch")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require OB (Order Block) Mitigation",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Demands price to mitigate the last high volume block",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = enableObMitigation,
                            onCheckedChange = { enableObMitigation = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Indigo500,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate600
                            ),
                            modifier = Modifier.testTag("ob_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val newConfig = StrategyConfig(
                                fvgLookback = fvgLookback.toInt(),
                                turtleLookback = turtleLookback.toInt(),
                                timeFrameMinutes = timeFrameMinutes,
                                turtleWickThresholdPct = turtleWickThresholdPct.toDouble(),
                                enableBosConfirmation = enableBosConfirmation,
                                enableObMitigation = enableObMitigation
                            )
                            viewModel.updateStrategyConfig(newConfig)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("apply_strategy_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Save", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Strategy Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: Live Bot Performance Dashboard Stats
        item {
            BotStatsPanel(recentSetups = recentSetups)
        }

        // Section 4: Real-time Signal Feed (Precise Entries)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Precise Setup Entries (Live Feed)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "MAX 20 SEC",
                        color = Amber400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (recentSetups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Slate800, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate700, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Bot Idle",
                            tint = Slate500,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scanning market ticks for liquidity sweeps...",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(recentSetups) { setup ->
                PreciseEntrySignalCard(setup = setup)
            }
        }
    }
}

@Composable
fun BotControlHeader(
    isBotActive: Boolean,
    onToggleBot: () -> Unit,
    recentSetupsCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bot_status_header"),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isBotActive) Emerald500.copy(alpha = 0.5f) else Slate700)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulse Green Indicator
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBotActive) Emerald500.copy(alpha = pulseAlpha) else Slate500
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Turtle Soup Trading Bot",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBotActive) "AUTO COPILOT SCALPING ACTIVE" else "AUTOPILOT SUSPENDED",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBotActive) Emerald400 else Slate400,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = onToggleBot,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBotActive) Rose500 else Emerald500
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("toggle_bot_btn")
            ) {
                Text(
                    text = if (isBotActive) "PAUSE" else "ACTIVATE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun BotStatsPanel(recentSetups: List<StrategySetup>) {
    val totalSignals = recentSetups.size
    val crtConfirmed = recentSetups.count { it.isCrtConfirmed }
    val bosCount = recentSetups.count { it.tradedAtBos }
    val obCount = recentSetups.count { it.tradedAtOb }
    val fvgCount = recentSetups.count { it.tradedAtFvg }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Bot Confluence Analytics",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "CRT Ratio",
                    value = if (totalSignals > 0) "${(crtConfirmed * 100) / totalSignals}%" else "0%",
                    color = Indigo400,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "BoS Swept",
                    value = "$bosCount",
                    color = Emerald400,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "OB Mitigated",
                    value = "$obCount",
                    color = Amber400,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "FVG Displacement",
                    value = "$fvgCount",
                    color = Rose400,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Slate700.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Slate400,
                fontSize = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PreciseEntrySignalCard(setup: StrategySetup) {
    val isBuy = setup.direction == TradeDirection.BUY
    val entryPrices = remember(setup) {
        val entry1 = setup.fvgMidpoint
        val entry2 = setup.mssPrice
        val entry3 = (setup.fvgMidpoint + setup.mssPrice) / 2.0
        val entry4 = (setup.fvgMidpoint + setup.stopLoss) / 2.0
        listOf(entry1, entry2, entry3, entry4)
    }

    val timeFormat = SimpleDateFormat("HH:mm:ss 'SAST'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT+2")
    }
    val timeStr = timeFormat.format(Date(setup.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("signal_card_${setup.asset.name}"),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isBuy) Emerald500.copy(alpha = 0.3f) else Rose500.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Asset + Direction + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isBuy) Emerald500.copy(alpha = 0.15f) else Rose500.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = setup.asset.displayName,
                            color = if (isBuy) Emerald400 else Rose400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBuy) "BULLISH CRAB REVERSION" else "BEARISH CRAB REVERSION",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = timeStr,
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CRT & Confluences Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // CRT Confirmed Badge
                if (setup.isCrtConfirmed) {
                    Box(
                        modifier = Modifier
                            .background(Indigo500.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, Indigo400.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Confirmed",
                                tint = Indigo400,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("CRT CONFIRMED", color = Indigo400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Turtle Body Soup checklist/indicators
                ConfluenceChip(label = "Turtle Soup", enabled = true)
                ConfluenceChip(label = "BoS", enabled = setup.tradedAtBos)
                ConfluenceChip(label = "OB Mit", enabled = setup.tradedAtOb)
                ConfluenceChip(label = "FVG", enabled = setup.tradedAtFvg)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details
            Text(
                text = "Reason: ${setup.crtDetails}",
                color = Slate300,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Precise Scale-in Limit Entry levels
            Text(
                text = "PRECISE ENTRY SPLITS (SCALED ORDER BOOK)",
                color = Slate400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entryPrices.forEachIndexed { index, price ->
                    val orderLabel = when (index) {
                        0 -> "Core FVG Fill Level"
                        1 -> "MSS Structure Shift Re-test"
                        2 -> "FVG / MSS Midpoint Zone"
                        3 -> "Discount Optimization Point"
                        else -> "Entry ${index + 1}"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate700.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .border(1.dp, Slate700, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isBuy) Emerald500 else Rose500),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(orderLabel, color = Color.White, fontSize = 11.sp)
                        }
                        Text(
                            text = String.format(Locale.US, "%,.4f", price),
                            color = if (isBuy) Emerald400 else Rose400,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Risk Boundaries: Stop Loss & Take Profit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Slate900.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("STOP LOSS (SAFETY)", color = Slate400, fontSize = 9.sp)
                        Text(
                            text = String.format(Locale.US, "%,.4f", setup.stopLoss),
                            color = Rose400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Slate900.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("TAKE PROFIT (TARGET)", color = Slate400, fontSize = 9.sp)
                        Text(
                            text = String.format(Locale.US, "%,.4f", setup.takeProfit),
                            color = Emerald400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfluenceChip(label: String, enabled: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (enabled) Emerald500.copy(alpha = 0.12f) else Slate700.copy(alpha = 0.4f),
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (enabled) Emerald400.copy(alpha = 0.3f) else Slate600.copy(alpha = 0.2f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (enabled) Emerald400 else Slate400)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (enabled) Emerald400 else Slate400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
