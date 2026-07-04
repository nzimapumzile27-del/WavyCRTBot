package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EconomicEvent
import com.example.model.EventStatus
import com.example.model.ImpactLevel
import com.example.viewmodel.TradingViewModel
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ForexFactoryCalendarPanel(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val economicEvents by viewModel.economicEvents.collectAsState()
    val latestAlert by viewModel.latestEconomicAlert.collectAsState()
    var selectedFilter by remember { mutableStateOf("RED_FOLDERS") } // Default to high-impact Red folders for traders!
    var expandedEventId by remember { mutableStateOf<String?>(null) }

    // Filter list
    val filteredEvents = remember(economicEvents, selectedFilter) {
        when (selectedFilter) {
            "RED_FOLDERS" -> economicEvents.filter { it.impact == ImpactLevel.HIGH }
            "SPEECHES" -> economicEvents.filter { it.impact == ImpactLevel.SPEECH }
            "ALL" -> economicEvents
            else -> economicEvents
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("forex_factory_panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Notification Banner Overlay for recent High-Impact releases
        AnimatedVisibility(
            visible = latestAlert != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            latestAlert?.let { alert ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (alert.impact == ImpactLevel.HIGH) Rose500.copy(alpha = 0.15f)
                            else Indigo500.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (alert.impact == ImpactLevel.HIGH) Rose500.copy(alpha = 0.4f)
                            else Indigo500.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Flash Ring
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (alert.impact == ImpactLevel.HIGH) Rose500.copy(alpha = 0.2f)
                                        else Indigo500.copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (alert.impact == ImpactLevel.HIGH) Icons.Default.Warning
                                                  else Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = if (alert.impact == ImpactLevel.HIGH) Rose400 else Indigo400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "LIVE FOREX FACTORY ALERT",
                                    color = if (alert.impact == ImpactLevel.HIGH) Rose400 else Indigo400,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "${alert.currency}: ${alert.title}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (alert.forecast != "N/A") {
                                        "Actual: ${alert.actual} (Forecast: ${alert.forecast} | Prev: ${alert.previous})"
                                    } else {
                                        alert.impactExplanation
                                    },
                                    color = Slate200,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        // Close/Dismiss Button
                        IconButton(
                            onClick = { viewModel.dismissEconomicAlert() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("dismiss_news_alert_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Alert",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Main Calendar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, Slate700),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with custom title & reset button
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
                                    .background(Rose500, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Forex Factory Economic Calendar",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Red Folders & market-shaping speeches feeding volatility",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Reset button
                    IconButton(
                        onClick = { viewModel.resetEconomicEvents() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("reset_economic_calendar_btn"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Slate400)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset calendar",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom impact/event filter tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        "RED_FOLDERS" to "RED FOLDERS",
                        "SPEECHES" to "SPEECHES",
                        "ALL" to "ALL IMPACTS"
                    )
                    filters.forEach { (key, title) ->
                        val isSelected = key == selectedFilter
                        val bgCol = if (isSelected) {
                            if (key == "RED_FOLDERS") Rose500 else if (key == "SPEECHES") Indigo500 else Slate600
                        } else {
                            Slate700.copy(alpha = 0.4f)
                        }
                        val contentCol = if (isSelected) Color.White else Slate400

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgCol)
                                .clickable { selectedFilter = key }
                                .testTag("news_filter_chip_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = contentCol,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Events
                if (filteredEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No events match this filter.",
                            color = Slate500,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredEvents.forEach { event ->
                            val isExpanded = event.id == expandedEventId
                            EconomicEventRow(
                                event = event,
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedEventId = if (isExpanded) null else event.id
                                },
                                onTriggerClick = {
                                    viewModel.manuallyTriggerEconomicEvent(event)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EconomicEventRow(
    event: EconomicEvent,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onTriggerClick: () -> Unit
) {
    val isRedFolder = event.impact == ImpactLevel.HIGH
    val impactColor = if (isRedFolder) Rose400 else if (event.impact == ImpactLevel.SPEECH) Indigo400 else Amber400
    val statusColor = when (event.status) {
        EventStatus.RELEASED -> Emerald400
        EventStatus.TRIGGERED -> Amber400
        EventStatus.UPCOMING -> Slate400
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Slate900.copy(alpha = 0.4f))
            .border(
                1.dp,
                if (isExpanded) impactColor.copy(alpha = 0.4f) else Slate700.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Currency Badge & Details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Currency Badge (Color coded to differentiate USD, EUR, GBP etc.)
                    val badgeBg = when (event.currency) {
                        "USD" -> Color(0xFF1E3A8A)
                        "EUR" -> Color(0xFF065F46)
                        "GBP" -> Color(0xFF5B21B6)
                        "AUD" -> Color(0xFF78350F)
                        else -> Slate800
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = event.currency,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // SAST release Hour badge
                            Text(
                                text = String.format(Locale.US, "%02d:%02d SAST", event.scheduledHourSAST, event.scheduledMinuteSAST),
                                color = Slate400,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Impact Folder color dot representing Red folder or Speech
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(impactColor, CircleShape)
                            )

                            Text(
                                text = if (isRedFolder) "HIGH IMPACT (RED)" else if (event.impact == ImpactLevel.SPEECH) "SPEECH" else "MED IMPACT",
                                color = impactColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }

                        Text(
                            text = event.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Side: Economic Values vs Forecast
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    if (event.status == EventStatus.RELEASED) {
                        Text(
                            text = "ACT: ${event.actual}",
                            color = statusColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "FCST: ${event.forecast}",
                            color = Slate400,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        // Action Trigger or Countdown helper
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate800)
                                .border(1.dp, Slate700, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "UPCOMING",
                                color = Slate400,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Expandable Area
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Divider(color = Slate700.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "IMPLICATIONS & DETAIL:",
                        color = impactColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = event.impactExplanation,
                        color = Slate300,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AFFECTS: ${event.targetAssetCategory.uppercase()}",
                                color = Slate400,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (event.forecast != "N/A") {
                                Text(
                                    text = "PREVIOUS VALUE: ${event.previous}",
                                    color = Slate500,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Manual Trigger CTA
                        if (event.status != EventStatus.RELEASED) {
                            Button(
                                onClick = onTriggerClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = impactColor.copy(alpha = 0.15f),
                                    contentColor = impactColor
                                ),
                                border = BorderStroke(1.dp, impactColor.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("force_trigger_${event.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "FORCE VOLATILITY",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold
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
