package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ICTKillzone
import com.example.viewmodel.TradingViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun KillzoneCountdownTimer(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val sastHour by viewModel.sastHour.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()

    // Smoothly interpolate minutes based on the 3-second simulation tick
    var currentMinute by remember { mutableStateOf(0) }
    var rawProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(sastHour, isSimulating) {
        if (!isSimulating) {
            currentMinute = 0
            rawProgress = 0f
            return@LaunchedEffect
        }
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed % 3000) / 3000f
            rawProgress = progress
            currentMinute = (progress * 59).toInt().coerceIn(0, 59)
            delay(50)
        }
    }

    // Pulse animation for the "ACTIVE" sessions
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("killzone_countdown_panel"),
        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, Slate700),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer Icon",
                            tint = Indigo400,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Killzone Countdown Tracker",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Precise entry windows in simulated GMT+2 time",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Current Simulated Time Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate900)
                        .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "SAST: %02d:%02d", sastHour, currentMinute),
                        color = Emerald400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful Custom 24-Hour Session Timeline
            Text(
                text = "24-HOUR SESSION TIMELINE",
                color = Slate400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Slate900, RoundedCornerShape(8.dp))
                    .border(1.dp, Slate700.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            ) {
                // We draw the killzones as solid horizontal blocks and a needles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Helper to map hour to X coordinate
                    fun hourToX(hour: Double): Float = ((hour / 24.0) * width).toFloat()

                    // Draw the Killzone intervals
                    ICTKillzone.values().forEach { zone ->
                        if (zone != ICTKillzone.OFF_SESSION) {
                            val xStart = hourToX(zone.startHourSAST.toDouble())
                            val xEnd = hourToX(zone.endHourSAST.toDouble())
                            val zoneColor = when (zone) {
                                ICTKillzone.ASIAN -> Color(0xFF3B82F6) // Blue
                                ICTKillzone.LONDON -> Indigo500
                                ICTKillzone.NEW_YORK -> Emerald400
                                ICTKillzone.LONDON_CLOSE -> Rose400
                                else -> Slate600
                            }
                            drawRect(
                                color = zoneColor.copy(alpha = 0.25f),
                                topLeft = Offset(xStart, 0f),
                                size = Size(xEnd - xStart, height)
                            )
                            // Draw thin borders at start and end of zone
                            drawLine(
                                color = zoneColor.copy(alpha = 0.5f),
                                start = Offset(xStart, 0f),
                                end = Offset(xStart, height),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = zoneColor.copy(alpha = 0.5f),
                                start = Offset(xEnd, 0f),
                                end = Offset(xEnd, height),
                                strokeWidth = 2f
                            )
                        }
                    }

                    // Draw grid markers for 6, 12, 18 hours
                    val hoursMarkers = listOf(6, 12, 18)
                    hoursMarkers.forEach { h ->
                        val mX = hourToX(h.toDouble())
                        drawLine(
                            color = Slate700.copy(alpha = 0.5f),
                            start = Offset(mX, 0f),
                            end = Offset(mX, height),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )
                    }

                    // Draw dynamic needle representing current SAST hour & progress
                    val currentSimTime = sastHour + rawProgress
                    val needleX = hourToX(currentSimTime.toDouble())

                    // Glow circle for needle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = 12f,
                        center = Offset(needleX, height / 2)
                    )
                    
                    // Main needle line
                    drawLine(
                        color = Color.White,
                        start = Offset(needleX, 0f),
                        end = Offset(needleX, height),
                        strokeWidth = 3f
                    )

                    // Top/Bottom indicators on needle
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(needleX, 0f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(needleX, height)
                    )
                }
            }

            // Labels under timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00:00", color = Slate500, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("06:00", color = Slate500, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("12:00", color = Slate500, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("18:00", color = Slate500, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("24:00", color = Slate500, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Countdown List
            Text(
                text = "UPCOMING & ACTIVE SESSIONS",
                color = Slate400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Focus on key KillZones
                val targetKillzones = listOf(
                    ICTKillzone.ASIAN,
                    ICTKillzone.LONDON,
                    ICTKillzone.NEW_YORK,
                    ICTKillzone.LONDON_CLOSE
                )

                targetKillzones.forEach { zone ->
                    val status = getKillzoneStatus(zone, sastHour, currentMinute, rawProgress)
                    val zoneColor = when (zone) {
                        ICTKillzone.ASIAN -> Color(0xFF3B82F6)
                        ICTKillzone.LONDON -> Indigo500
                        ICTKillzone.NEW_YORK -> Emerald400
                        ICTKillzone.LONDON_CLOSE -> Rose400
                        else -> Slate600
                    }

                    val isActive = status is KillzoneTimeStatus.Active
                    val borderCol = if (isActive) zoneColor.copy(alpha = 0.5f) else Slate700.copy(alpha = 0.4f)
                    val bgCol = if (isActive) zoneColor.copy(alpha = 0.05f) else Slate900.copy(alpha = 0.3f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgCol)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Side: Name and Time info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(zoneColor.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.HourglassBottom else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = zoneColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = zone.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.US, "Window: %02d:00 - %02d:00 SAST", zone.startHourSAST, zone.endHourSAST),
                                    color = Slate500,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Right Side: Countdown Readout
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            when (status) {
                                is KillzoneTimeStatus.Active -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Emerald400.copy(alpha = pulseAlpha), CircleShape)
                                        )
                                        Text(
                                            text = "ACTIVE",
                                            color = Emerald400,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "%dh %02dm left", status.hours, status.minutes),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = String.format(Locale.US, "T-minus %.1fs real", status.realSeconds),
                                        color = Slate400,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                is KillzoneTimeStatus.Upcoming -> {
                                    Text(
                                        text = "UPCOMING",
                                        color = Amber400,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "in %dh %02dm", status.hours, status.minutes),
                                        color = Slate300,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = String.format(Locale.US, "T-minus %.1fs real", status.realSeconds),
                                        color = Slate500,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class KillzoneTimeStatus {
    data class Active(val hours: Int, val minutes: Int, val realSeconds: Double) : KillzoneTimeStatus()
    data class Upcoming(val hours: Int, val minutes: Int, val realSeconds: Double) : KillzoneTimeStatus()
}

fun getKillzoneStatus(
    zone: ICTKillzone,
    currentHour: Int,
    currentMinute: Int,
    progress: Float
): KillzoneTimeStatus {
    val currentSimTime = currentHour + progress
    
    val startHour = zone.startHourSAST.toDouble()
    val endHour = zone.endHourSAST.toDouble()

    val isActive = currentHour >= zone.startHourSAST && currentHour < zone.endHourSAST

    return if (isActive) {
        val simHoursLeft = endHour - currentSimTime
        val realSecondsLeft = simHoursLeft * 3.0

        val currentTotalMin = currentHour * 60 + currentMinute
        val endTotalMin = zone.endHourSAST * 60
        val remainingMin = (endTotalMin - currentTotalMin).coerceAtLeast(0)

        KillzoneTimeStatus.Active(
            hours = remainingMin / 60,
            minutes = remainingMin % 60,
            realSeconds = (realSecondsLeft * 10).toInt() / 10.0
        )
    } else {
        val simHoursLeft = if (currentSimTime < startHour) {
            startHour - currentSimTime
        } else {
            (24.0 - currentSimTime) + startHour
        }
        val realSecondsLeft = simHoursLeft * 3.0

        val currentTotalMin = currentHour * 60 + currentMinute
        val startTotalMin = zone.startHourSAST * 60
        val diffMin = if (currentTotalMin < startTotalMin) {
            startTotalMin - currentTotalMin
        } else {
            (24 * 60 - currentTotalMin) + startTotalMin
        }

        KillzoneTimeStatus.Upcoming(
            hours = diffMin / 60,
            minutes = diffMin % 60,
            realSeconds = (realSecondsLeft * 10).toInt() / 10.0
        )
    }
}
