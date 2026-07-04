package com.example.viewmodel

data class RiskConfig(
    val riskPercent: Float = 1.0f,
    val maxPreciseEntries: Int = 4,
    val stopLossMode: String = "WICK", // "WICK" or "HTF_PERCENT"
    val stopLossHtfPercent: Float = 0.5f,
    val trailingTpMode: String = "ATR", // "ATR" or "MIDPOINT" or "NONE"
    val trailingSensitivity: Float = 1.5f,
    val positionSizingMode: String = "AUTO", // "AUTO" or "FIXED"
    val fixedLotSize: Double = 0.1
)
