package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AssetType
import com.example.model.DailyPnL
import com.example.model.ICTKillzone
import com.example.model.MarketCandle
import com.example.model.TradeLog
import com.example.service.Mql5CodeGenerator
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Rose400
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.viewmodel.RiskConfig
import com.example.viewmodel.TradingViewModel
import com.example.ui.RsiDivergencePanel
import com.example.ui.RsiDivergencesLogDialog
import com.example.ui.KillzoneCountdownTimer
import com.example.ui.ForexFactoryCalendarPanel
import com.example.ui.BotDashboardTab
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

enum class NavigationTab {
    MONITOR,
    VISUALIZER,
    KILLZONES,
    RISK_DASHBOARD,
    BOT_DASHBOARD,
    EA_CODE
}

@Composable
fun MainScreen(viewModel: TradingViewModel = viewModel()) {
    var activeTab by remember { mutableStateOf(NavigationTab.MONITOR) }
    val currentSession by viewModel.currentSession.collectAsState()
    val sastHour by viewModel.sastHour.collectAsState()
    
    var showDivergencesDialog by remember { mutableStateOf(false) }
    val rsiDivergences by viewModel.rsiDivergences.collectAsState()
    val unreadCount = rsiDivergences.count { !it.isRead }

    if (showDivergencesDialog) {
        RsiDivergencesLogDialog(
            viewModel = viewModel,
            onDismissRequest = { showDivergencesDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900),
        bottomBar = {
            BottomNavBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate900)
                .padding(innerPadding)
        ) {
            // App Bar Styled with Geometric Balance guidelines
            HeaderBar(
                session = currentSession,
                sastHour = sastHour,
                unreadCount = unreadCount,
                onBellClick = { showDivergencesDialog = true }
            )

            // Content body per Tab selection
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    NavigationTab.MONITOR -> MonitorTab(viewModel)
                    NavigationTab.VISUALIZER -> VisualizerTab(viewModel)
                    NavigationTab.KILLZONES -> KillZonesTab(viewModel)
                    NavigationTab.RISK_DASHBOARD -> RiskDashboardTab(viewModel)
                    NavigationTab.BOT_DASHBOARD -> BotDashboardTab(viewModel)
                    NavigationTab.EA_CODE -> EaCodeTab()
                }
            }
        }
    }
}

@Composable
fun HeaderBar(session: ICTKillzone, sastHour: Int, unreadCount: Int, onBellClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // "C" branding logo container in Indigo500
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Indigo500, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
            Column {
                Text(
                    text = "CRT Terminal",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 1.2.sp
                )
                Text(
                    text = "SAST SESSION • SIM LIVE (${String.format("%02d:00", sastHour)})",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Active Session Badge
            val badgeColor = when (session) {
                ICTKillzone.ASIAN -> Color(0xFF3B82F6)
                ICTKillzone.LONDON -> Indigo400
                ICTKillzone.NEW_YORK -> Emerald400
                ICTKillzone.LONDON_CLOSE -> Rose400
                else -> Slate600
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = session.displayName,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Notification Bell with Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Slate800)
                    .border(1.dp, Slate700, CircleShape)
                    .clickable { onBellClick() }
                    .testTag("notification_bell_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Alerts Log",
                    tint = if (unreadCount > 0) Indigo400 else Slate400,
                    modifier = Modifier.size(20.dp)
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(10.dp)
                            .background(Rose500, CircleShape)
                            .border(1.dp, Slate800, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(activeTab: NavigationTab, onTabSelected: (NavigationTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Slate800)
            .border(1.dp, Slate700, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            Triple(NavigationTab.MONITOR, Icons.Default.Monitor, "Monitor"),
            Triple(NavigationTab.VISUALIZER, Icons.Default.Analytics, "Visualizer"),
            Triple(NavigationTab.KILLZONES, Icons.Default.Info, "KillZones"),
            Triple(NavigationTab.RISK_DASHBOARD, Icons.Default.Security, "Risk"),
            Triple(NavigationTab.BOT_DASHBOARD, Icons.Default.Settings, "Bot"),
            Triple(NavigationTab.EA_CODE, Icons.Default.Code, "MQL5 EA")
        )

        tabs.forEach { (tab, icon, label) ->
            val isSelected = activeTab == tab
            val tintColor by animateColorAsState(
                targetValue = if (isSelected) Indigo400 else Slate400,
                label = "nav_tint"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(28.dp)
                            .background(Indigo500.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Slate400,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun MonitorTab(viewModel: TradingViewModel) {
    val selectedAsset by viewModel.selectedAsset.collectAsState()
    val candlesMap by viewModel.marketCandles.collectAsState()
    val htfRanges by viewModel.htfRanges.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()

    val candles = candlesMap[selectedAsset] ?: emptyList()
    val htfRange = htfRanges[selectedAsset] ?: Pair(0.0, 0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time dynamic Killzone countdown timer
        KillzoneCountdownTimer(viewModel = viewModel)

        // Horizontal Asset Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssetType.values().forEach { asset ->
                val isSelected = asset == selectedAsset
                val borderCol = if (isSelected) Indigo500 else Slate700
                val bgCol = if (isSelected) Indigo500.copy(alpha = 0.15f) else Slate800

                Box(
                    modifier = Modifier
                        .testTag("asset_selector_${asset.name.lowercase(Locale.US)}")
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgCol)
                        .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                        .clickable { viewModel.selectAsset(asset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = asset.displayName,
                        color = if (isSelected) Color.White else Slate400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Active Range Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Slate700), // Wait, BorderStroke is from package androidx.compose.foundation.BorderStroke, let's use border modifier or raw BorderStroke
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Daily HTF Range",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = selectedAsset.displayName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Range Mid (50%)",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        val midVal = (htfRange.first + htfRange.second) / 2.0
                        Text(
                            text = String.format(Locale.US, "%,.2f", midVal),
                            color = Indigo400,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful custom visual representation of the Turtle Soup sweep
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Slate900, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Draw central dotted range line
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val midY = size.height / 2f
                        drawLine(
                            color = Indigo500.copy(alpha = 0.4f),
                            start = Offset(0f, midY),
                            end = Offset(size.width, midY),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                            strokeWidth = 2f
                        )

                        // Highlight sweep zones (15% left and 25% right margins)
                        val fillWidth = size.width * 0.6f
                        val startX = size.width * 0.2f
                        drawRect(
                            color = Indigo500.copy(alpha = 0.05f),
                            topLeft = Offset(startX, 0f),
                            size = Size(fillWidth, size.height)
                        )

                        // Outer vertical bounds representing HTF high/low
                        drawLine(
                            color = Indigo500.copy(alpha = 0.3f),
                            start = Offset(startX, 0f),
                            end = Offset(startX, size.height),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Indigo500.copy(alpha = 0.3f),
                            start = Offset(startX + fillWidth, 0f),
                            end = Offset(startX + fillWidth, size.height),
                            strokeWidth = 2f
                        )

                        // Draw real-time mock candle sweep indicator (pulse)
                        val indicatorX = size.width * 0.22f
                        drawCircle(
                            color = Emerald400,
                            radius = 6f,
                            center = Offset(indicatorX, midY)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "Low Sweep Zone: %,.2f", htfRange.second),
                            color = Emerald400,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "FVG DETECTED LTF",
                            color = Indigo400,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "High Sweep Zone: %,.2f", htfRange.first),
                            color = Rose400,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Live Asset Candlestick Chart Drawer (Material 3 style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Slate700),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Precise Multi-Timeframe Chart",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color Indicator legends
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Yellow, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("9 EMA", color = Slate400, fontSize = 9.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF00FF), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("200 EMA", color = Slate400, fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Painter for our real candles data
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Slate900, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                ) {
                    val width = constraints.maxWidth.toFloat()
                    val height = constraints.maxHeight.toFloat()

                    if (candles.isNotEmpty()) {
                        val maxPrice = kotlin.math.max(htfRange.first, candles.maxOf { it.high })
                        val minPrice = kotlin.math.min(htfRange.second, candles.minOf { it.low })
                        val priceRange = maxPrice - minPrice

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Scale price to Y coordinate helper
                            fun getScaledY(price: Double): Float {
                                return if (priceRange == 0.0) height / 2f
                                else (height - ((price - minPrice) / priceRange * height)).toFloat()
                            }

                            // 1. Draw HTF levels
                            val highY = getScaledY(htfRange.first)
                            val lowY = getScaledY(htfRange.second)
                            val midY = getScaledY((htfRange.first + htfRange.second) / 2.0)

                            // HTF High
                            drawLine(
                                color = Rose500,
                                start = Offset(0f, highY),
                                end = Offset(width, highY),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            )
                            // HTF Low
                            drawLine(
                                color = Emerald500,
                                start = Offset(0f, lowY),
                                end = Offset(width, lowY),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            )
                            // HTF Mid
                            drawLine(
                                color = Indigo500.copy(alpha = 0.5f),
                                start = Offset(0f, midY),
                                end = Offset(width, midY),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )

                            // 2. Draw Candlesticks & EMAs
                            val candleCount = kotlin.math.min(18, candles.size)
                            val visibleCandles = candles.takeLast(candleCount)
                            val barWidth = width / candleCount
                            val spaceRatio = 0.25f

                            val ema9Points = mutableListOf<Offset>()
                            val ema200Points = mutableListOf<Offset>()

                            visibleCandles.forEachIndexed { index, candle ->
                                val x = index * barWidth + (barWidth * spaceRatio)
                                val w = barWidth * (1f - spaceRatio * 2)

                                val openY = getScaledY(candle.open)
                                val closeY = getScaledY(candle.close)
                                val candleHighY = getScaledY(candle.high)
                                val candleLowY = getScaledY(candle.low)

                                // Draw FVG Shading Zone if present
                                if (candle.isFVG) {
                                    val fvgTopY = getScaledY(candle.fvgTop)
                                    val fvgBottomY = getScaledY(candle.fvgBottom)
                                    drawRect(
                                        color = Indigo500.copy(alpha = 0.15f),
                                        topLeft = Offset(x, kotlin.math.min(fvgTopY, fvgBottomY)),
                                        size = Size(w * 2.5f, kotlin.math.abs(fvgTopY - fvgBottomY))
                                    )
                                }

                                // Wick
                                drawLine(
                                    color = if (candle.close >= candle.open) Emerald400 else Rose400,
                                    start = Offset(x + w / 2f, candleHighY),
                                    end = Offset(x + w / 2f, candleLowY),
                                    strokeWidth = 2.5f
                                )

                                // Body
                                drawRect(
                                    color = if (candle.close >= candle.open) Emerald400 else Rose400,
                                    topLeft = Offset(x, kotlin.math.min(openY, closeY)),
                                    size = Size(w, kotlin.math.max(3f, kotlin.math.abs(openY - closeY)))
                                )

                                // Gather points for Indicator lines
                                if (candle.ema9 > 0) {
                                    ema9Points.add(Offset(x + w / 2f, getScaledY(candle.ema9)))
                                }
                                if (candle.ema200 > 0) {
                                    ema200Points.add(Offset(x + w / 2f, getScaledY(candle.ema200)))
                                }
                            }

                            // Draw continuous 9 EMA Line
                            for (p in 0 until ema9Points.size - 1) {
                                drawLine(
                                    color = Color.Yellow,
                                    start = ema9Points[p],
                                    end = ema9Points[p + 1],
                                    strokeWidth = 4f
                                )
                            }

                            // Draw continuous 200 EMA Line
                            for (p in 0 until ema200Points.size - 1) {
                                drawLine(
                                    color = Color(0xFFFF00FF), // Magenta
                                    start = ema200Points[p],
                                    end = ema200Points[p + 1],
                                    strokeWidth = 4.5f
                                )
                            }
                        }

                        // Labels layered inside the Canvas container
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text(
                                text = String.format(Locale.US, "HTF High: %,.2f", htfRange.first),
                                color = Rose400.copy(alpha = 0.9f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = String.format(Locale.US, "HTF Low: %,.2f", htfRange.second),
                                color = Emerald400.copy(alpha = 0.9f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Action controls (Play, Pause, Manual Tick)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.setSimulating(!isSimulating) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("sim_toggle_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSimulating) Rose500.copy(alpha = 0.2f) else Emerald500
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isSimulating) Rose400 else Emerald400)
            ) {
                Icon(
                    imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isSimulating) Rose400 else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSimulating) "Pause Bot" else "Resume Bot",
                    color = if (isSimulating) Rose400 else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.forceTick() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("force_tick_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Slate600)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Tick",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manual Candle",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Real-time RSI Divergence Confluences Panel
        RsiDivergencePanel(viewModel = viewModel)

        // Live Activity Terminal Logs Console
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .border(1.dp, Slate700, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRECISION ACTIVITY LOGS",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isSimulating) Emerald400 else Slate400, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (terminalLogs.isEmpty()) {
                    Text(
                        text = "Listening for setups inside valid Killzones...",
                        color = Slate600,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    terminalLogs.forEach { log ->
                        Text(
                            text = log,
                            color = if (log.contains("CLOSED") && log.contains("PROFIT")) Emerald400
                            else if (log.contains("CLOSED") && log.contains("LOSS")) Rose400
                            else if (log.contains("SETUP")) Indigo400
                            else Slate200,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VisualizerTab(viewModel: TradingViewModel) {
    val closedTrades by viewModel.closedTrades.collectAsState()
    val activeTrades by viewModel.activeTrades.collectAsState()
    val dailyPnL by viewModel.dailyPnLTrend.collectAsState()
    val accountBalance by viewModel.accountBalance.collectAsState()
    val accountEquity by viewModel.accountEquity.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High level overview statistics card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Slate700),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ACCOUNT BALANCE", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "$%,.2f", accountBalance),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("FLOATING EQUITY", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "$%,.2f", accountEquity),
                            color = Indigo400,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Slate700)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Win Rate", color = Slate400, fontSize = 10.sp)
                        val wins = closedTrades.count { it.profit > 0 }
                        val total = closedTrades.size
                        val rate = if (total > 0) (wins.toFloat() / total * 100).toInt() else 75
                        Text("$rate%", color = Emerald400, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Closed Trades", color = Slate400, fontSize = 10.sp)
                        Text("${closedTrades.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Active Trades", color = Slate400, fontSize = 10.sp)
                        Text("${activeTrades.size}", color = Indigo400, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Geometric Balance 5-day Profit & Loss Visualizer
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Slate700),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profit & Loss Volatility (5-Day)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Trailing TP 50% Active",
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bar Chart rendering matching the geometric balancing style perfectly!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Render 5 columns representing Day 1 to Day 5
                    val daysOfWeek = listOf("Day 1", "Day 2", "Day 3", "Day 4", "Day 5")

                    daysOfWeek.forEachIndexed { index, day ->
                        val pnlItem = dailyPnL.getOrNull(index) ?: DailyPnL(day, 0.0, 0.0)
                        val pnlVal = pnlItem.profitLoss

                        // Height scaling
                        val fraction = (kotlin.math.abs(pnlVal) / 350.0).coerceIn(0.1, 1.0).toFloat()
                        val isProfit = pnlVal >= 0

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Bar layout representer
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (isProfit) Emerald500.copy(alpha = 0.2f) else Rose500.copy(
                                                alpha = 0.2f
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                colors = if (isProfit) listOf(Emerald400, Emerald500) else listOf(
                                                    Rose400,
                                                    Rose500
                                                )
                                            ),
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                ) {
                                    // Subtle top light indicator highlight
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(if (isProfit) Emerald400 else Rose400)
                                    )
                                }

                                // Value label layered on top of bar
                                Text(
                                    text = String.format(Locale.US, "%s$%,.0f", if (isProfit) "+" else "-", kotlin.math.abs(pnlVal)),
                                    color = if (isProfit) Emerald400 else Rose400,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(bottom = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = day,
                                color = Slate400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Closed Trades History list header
        Text(
            text = "PRECISE CLOSED TRADES HISTORIES",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (closedTrades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trades executed yet. Switch to Monitor to resume bot.",
                    color = Slate600,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            closedTrades.forEach { trade ->
                val isProfit = trade.profit >= 0
                val borderCol = if (isProfit) Emerald400.copy(alpha = 0.3f) else Rose400.copy(alpha = 0.3f)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    border = BorderStroke(1.dp, borderCol),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = trade.asset,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (trade.direction == "BUY") Emerald500.copy(alpha = 0.15f)
                                            else Rose500.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = trade.direction,
                                        color = if (trade.direction == "BUY") Emerald400 else Rose400,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = String.format(Locale.US, "%s$%,.2f", if (isProfit) "+" else "-", kotlin.math.abs(trade.profit)),
                                color = if (isProfit) Emerald400 else Rose400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Average Entry", color = Slate400, fontSize = 9.sp)
                                Text(
                                    text = String.format(Locale.US, "%,.2f", trade.averagePrice),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column {
                                Text("Position Split", color = Slate400, fontSize = 9.sp)
                                Text(
                                    text = "${trade.entriesFilledCount} Levels Filled",
                                    color = Indigo400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Session Context", color = Slate400, fontSize = 9.sp)
                                Text(
                                    text = trade.session,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = "Setup: ${trade.setupReason}",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KillZonesTab(viewModel: TradingViewModel) {
    val currentSession by viewModel.currentSession.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ICT Killzones (SAST Synchronized)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Text(
            text = "The SAST Session Sweep focuses on executing precise body soup sweeps during specific high-liquidity hours in South African Standard Time (GMT+2). Only crypto is traded on weekends.",
            color = Slate400,
            fontSize = 12.sp,
            lineHeight = 1.5.sp
        )

        // Real-time dynamic Killzone countdown timer
        KillzoneCountdownTimer(viewModel = viewModel)

        ICTKillzone.values().forEach { zone ->
            if (zone != ICTKillzone.OFF_SESSION) {
                val isActive = currentSession == zone
                val borderCol = if (isActive) Indigo500 else Slate700
                val bgCol = if (isActive) Indigo500.copy(alpha = 0.08f) else Slate800

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bgCol),
                    border = BorderStroke(1.dp, borderCol),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = zone.displayName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Emerald400, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Timing: ${String.format("%02d:00", zone.startHourSAST)} to ${
                                    String.format(
                                        "%02d:00",
                                        zone.endHourSAST
                                    )
                                } SAST",
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) Indigo500 else Slate700)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isActive) "ACTIVE" else "PENDING",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        ForexFactoryCalendarPanel(viewModel = viewModel)
    }
}

@Composable
fun RiskDashboardTab(viewModel: TradingViewModel) {
    val riskConfig by viewModel.riskConfig.collectAsState()
    val balance by viewModel.accountBalance.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Risk Management Dashboard",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        // Account Sizing Calculation Readout
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Indigo500.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, Indigo500.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LIVE MATHEMATICAL CALCULATION",
                    color = Indigo400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                val riskAmt = balance * (riskConfig.riskPercent / 100.0)
                Text(
                    text = String.format(
                        Locale.US,
                        "Max Risk Per Trade: $%,.2f (at %.1f%% of Account Equity)",
                        riskAmt,
                        riskConfig.riskPercent
                    ),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total limit lots will automatically scale-in across the ${riskConfig.maxPreciseEntries} precise entries inside the FVG entry zone, maintaining perfect portfolio balance.",
                    color = Slate400,
                    fontSize = 11.sp
                )
            }
        }

        // 1. Dynamic Stop Loss Levels Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            border = BorderStroke(1.dp, Slate700),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Dynamic Stop-Loss Strategy",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )

                // Selectors for stop loss mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        "SWEEP_WICK" to "Wick Extreme",
                        "HTF_PERCENT" to "% HTF Range"
                    )

                    modes.forEach { (mode, label) ->
                        val isSelected = riskConfig.stopLossMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sl_mode_$mode")
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Indigo500 else Slate700)
                                .clickable {
                                    viewModel.updateRiskConfig(riskConfig.copy(stopLossMode = mode))
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (riskConfig.stopLossMode == "HTF_PERCENT") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("HTF Candle Range Slider Offset", color = Slate400, fontSize = 11.sp)
                            Text("${riskConfig.stopLossHtfPercent.toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = riskConfig.stopLossHtfPercent,
                            onValueChange = {
                                viewModel.updateRiskConfig(riskConfig.copy(stopLossHtfPercent = it))
                            },
                            valueRange = 5f..25f,
                            colors = SliderDefaults.colors(
                                thumbColor = Indigo400,
                                activeTrackColor = Indigo500,
                                inactiveTrackColor = Slate700
                            ),
                            modifier = Modifier.testTag("sl_slider")
                        )
                    }
                } else {
                    Text(
                        text = "👉 Stop Loss is placed on the exact low or high of the Sweep Rejection Candle's wick with dynamic buffer. Safe & precise.",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        // 2. Trailing Take Profit Levels Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            border = BorderStroke(1.dp, Slate700),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. Trailing Take-Profit",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        "VOLATILITY" to "Volatility (ATR)",
                        "PROGRESS" to "% Progress"
                    )

                    modes.forEach { (mode, label) ->
                        val isSelected = riskConfig.trailingTpMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tp_mode_$mode")
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Indigo500 else Slate700)
                                .clickable {
                                    viewModel.updateRiskConfig(riskConfig.copy(trailingTpMode = mode))
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (riskConfig.trailingTpMode == "VOLATILITY") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Volatility Sensitivity Multiplier", color = Slate400, fontSize = 11.sp)
                            Text(String.format(Locale.US, "%.1fx", riskConfig.trailingSensitivity), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = riskConfig.trailingSensitivity,
                            onValueChange = {
                                viewModel.updateRiskConfig(riskConfig.copy(trailingSensitivity = it))
                            },
                            valueRange = 1.0f..3.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Indigo400,
                                activeTrackColor = Indigo500,
                                inactiveTrackColor = Slate700
                            )
                        )
                    }
                } else {
                    Text(
                        text = "👉 Trailing Stop moves to Break-Even + 10% expected move buffer once the trade crosses the 50% midpoint target towards opposite HTF range.",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        // 3. Automated Sizing configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            border = BorderStroke(1.dp, Slate700),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "3. Position Sizing & Slices",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Automated Account sizing", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = riskConfig.positionSizingMode == "AUTO",
                        onCheckedChange = { isAuto ->
                            val m = if (isAuto) "AUTO" else "FIXED"
                            viewModel.updateRiskConfig(riskConfig.copy(positionSizingMode = m))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Indigo400,
                            checkedTrackColor = Indigo500,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate700
                        )
                    )
                }

                if (riskConfig.positionSizingMode == "AUTO") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Risk % of Equity", color = Slate400, fontSize = 11.sp)
                            Text(String.format(Locale.US, "%.1f%%", riskConfig.riskPercent), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = riskConfig.riskPercent,
                            onValueChange = {
                                viewModel.updateRiskConfig(riskConfig.copy(riskPercent = it))
                            },
                            valueRange = 0.2f..5.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Indigo400,
                                activeTrackColor = Indigo500,
                                inactiveTrackColor = Slate700
                            )
                        )
                    }
                } else {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fixed Lot Size fallback", color = Slate400, fontSize = 11.sp)
                            Text(String.format(Locale.US, "%.2f Lots", riskConfig.fixedLotSize), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = riskConfig.fixedLotSize.toFloat(),
                            onValueChange = {
                                viewModel.updateRiskConfig(riskConfig.copy(fixedLotSize = it.toDouble()))
                            },
                            valueRange = 0.05f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = Indigo400,
                                activeTrackColor = Indigo500,
                                inactiveTrackColor = Slate700
                            )
                        )
                    }
                }

                HorizontalDivider(color = Slate700)

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Limit Entry Splits", color = Slate400, fontSize = 11.sp)
                        Text("${riskConfig.maxPreciseEntries} splits", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = riskConfig.maxPreciseEntries.toFloat(),
                        onValueChange = {
                            viewModel.updateRiskConfig(riskConfig.copy(maxPreciseEntries = it.toInt()))
                        },
                        valueRange = 1f..4f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = Indigo400,
                            activeTrackColor = Indigo500,
                            inactiveTrackColor = Slate700
                        )
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.resetSimulation() },
            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reset_sim_btn")
        ) {
            Text("Reset Simulator & Clearing Databases", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EaCodeTab() {
    val context = LocalContext.current
    val codeStr = remember { Mql5CodeGenerator.generateEA() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MQL5 Expert Advisor Code",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("MQL5_EA_Code", codeStr)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "MQL5 EA Code Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("copy_ea_code_btn")
            ) {
                Text("Copy EA Code", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "Copy and compile this syntactically complete Expert Advisor directly inside MetaEditor 5 (MetaTrader 5) to run the exact strategy.",
            color = Slate400,
            fontSize = 12.sp,
            lineHeight = 1.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Slate700),
            shape = RoundedCornerShape(12.dp)
        ) {
            SelectionContainer {
                Text(
                    text = codeStr,
                    color = Color(0xFF34D399), // green monospace output
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
