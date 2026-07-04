package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AssetType(val displayName: String, val category: String, val isWeekendTraded: Boolean) {
    BTC_USD("BTC/USD", "Crypto", true),
    ETH_USD("ETH/USD", "Crypto", true),
    US500("US500", "Indices", false),
    NAS100("NAS100", "Indices", false),
    GBP_USD("GBP/USD", "Forex", false),
    EUR_USD("EUR_USD", "Forex", false)
}

enum class ICTKillzone(val displayName: String, val startHourSAST: Int, val endHourSAST: Int) {
    ASIAN("Asian Killzone", 2, 9),
    LONDON("London Killzone", 9, 12),
    NEW_YORK("New York Killzone", 14, 17),
    LONDON_CLOSE("London Close Killzone", 18, 20),
    OFF_SESSION("Off Session", 0, 0);

    companion object {
        fun getCurrentSession(hourSAST: Int): ICTKillzone {
            return values().firstOrNull { 
                if (it == OFF_SESSION) false
                else hourSAST >= it.startHourSAST && hourSAST < it.endHourSAST
            } ?: OFF_SESSION
        }
    }
}

data class MarketCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val rsi: Double = 50.0,
    val ema9: Double = 0.0,
    val ema200: Double = 0.0,
    val isFVG: Boolean = false,
    val fvgTop: Double = 0.0,
    val fvgBottom: Double = 0.0,
    val isSweepCandle: Boolean = false,
    val isMssTrigger: Boolean = false
)

enum class TradeStatus {
    PENDING, ACTIVE, CLOSED
}

enum class TradeDirection {
    BUY, SELL
}

@Entity(tableName = "trade_logs")
data class TradeLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val asset: String,
    val direction: String, // "BUY" or "SELL"
    val entryPrice: Double,
    val averagePrice: Double,
    val size: Double,
    val profit: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "PENDING", "ACTIVE", "CLOSED"
    val session: String, // e.g. "LONDON", "NEW_YORK"
    val setupReason: String, // e.g. "D1 High Sweep + LTF MSS + RSI Div"
    val entriesFilledCount: Int, // 1 to 4 scaling levels
    val closedTimestamp: Long = 0,
    val initialBalance: Double = 10000.0,
    val finalBalance: Double = 10000.0
)

@Entity(tableName = "daily_pnl")
data class DailyPnL(
    @PrimaryKey val date: String, // e.g. "Day 1", "Day 2", etc. or "2026-07-01"
    val profitLoss: Double,
    val cumulativePnL: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class StrategySetup(
    val asset: AssetType,
    val htfHigh: Double,
    val htfLow: Double,
    val direction: TradeDirection,
    val sweepPrice: Double,
    val mssPrice: Double,
    val fvgMidpoint: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val rsiAtSweep: Double,
    val rsiDivergenceConfirmed: Boolean,
    val session: ICTKillzone,
    val timestamp: Long = System.currentTimeMillis(),
    val isCrtConfirmed: Boolean = true,
    val tradedAtBos: Boolean = true,
    val tradedAtOb: Boolean = true,
    val tradedAtFvg: Boolean = true,
    val crtDetails: String = "CRT Confirmed via Turtle Soup + MSS"
)
