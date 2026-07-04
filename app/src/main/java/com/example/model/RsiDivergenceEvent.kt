package com.example.model

import java.util.UUID

data class RsiDivergenceEvent(
    val id: String = UUID.randomUUID().toString(),
    val asset: AssetType,
    val direction: TradeDirection, // BUY is Bullish, SELL is Bearish
    val priceBefore: Double,
    val priceAfter: Double,
    val rsiBefore: Double,
    val rsiAfter: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
