package com.example.model

data class StrategyConfig(
    val fvgLookback: Int = 3,                 // Recent candles to search for unmitigated FVG
    val turtleLookback: Int = 15,             // Lookback period to find swing extremes (Turtle soup threshold)
    val timeFrameMinutes: Int = 15,           // Candle interval in minutes (5, 15, 60, 240)
    val turtleWickThresholdPct: Double = 0.15, // Wick/StopLoss buffer beyond high/low (volatility factor)
    val enableBosConfirmation: Boolean = true, // Whether Break of Structure (BoS) is required for CRT
    val enableObMitigation: Boolean = true      // Whether Order Block (OB) mitigation is verified
)
