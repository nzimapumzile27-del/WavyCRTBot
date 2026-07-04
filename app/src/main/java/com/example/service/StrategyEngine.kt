package com.example.service

import com.example.model.AssetType
import com.example.model.ICTKillzone
import com.example.model.MarketCandle
import com.example.model.StrategySetup
import com.example.model.TradeDirection
import com.example.model.StrategyConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object StrategyEngine {

    // Default base prices for realistic simulations
    fun getBasePrice(asset: AssetType): Double {
        return when (asset) {
            AssetType.BTC_USD -> 62500.0
            AssetType.ETH_USD -> 3420.0
            AssetType.US500 -> 5250.0
            AssetType.NAS100 -> 18600.0
            AssetType.GBP_USD -> 1.2740
            AssetType.EUR_USD -> 1.0860
        }
    }

    // Typical volatility multiplier per tick/candle
    fun getVolatility(asset: AssetType): Double {
        return when (asset) {
            AssetType.BTC_USD -> 180.0
            AssetType.ETH_USD -> 15.0
            AssetType.US500 -> 8.0
            AssetType.NAS100 -> 40.0
            AssetType.GBP_USD -> 0.0012
            AssetType.EUR_USD -> 0.0008
        }
    }

    /**
     * Generates a series of starting candles for a given asset with pre-calculated EMA and RSI.
     */
    fun generateInitialCandles(asset: AssetType, count: Int = 30): List<MarketCandle> {
        val basePrice = getBasePrice(asset)
        val volatility = getVolatility(asset)
        var currentPrice = basePrice - (volatility * count / 3) // Start slightly lower

        val candles = ArrayList<MarketCandle>()
        var timestamp = System.currentTimeMillis() - (count * 15 * 60 * 1000)

        for (i in 0 until count) {
            val change = (Random.nextDouble() - 0.48) * volatility // Slight upward bias
            val open = currentPrice
            val close = currentPrice + change
            val high = max(open, close) + (Random.nextDouble() * volatility * 0.4)
            val low = min(open, close) - (Random.nextDouble() * volatility * 0.4)
            currentPrice = close

            candles.add(
                MarketCandle(
                    timestamp = timestamp,
                    open = open,
                    high = high,
                    low = low,
                    close = close
                )
            )
            timestamp += 15 * 60 * 1000
        }

        return computeIndicators(candles)
    }

    /**
     * Appends a new real-time candle to the list and computes updated technical indicators.
     */
    fun generateNextCandle(asset: AssetType, currentCandles: List<MarketCandle>, biasDirection: TradeDirection? = null): MarketCandle {
        val lastCandle = currentCandles.lastOrNull() ?: MarketCandle(System.currentTimeMillis(), getBasePrice(asset), getBasePrice(asset), getBasePrice(asset), getBasePrice(asset))
        val volatility = getVolatility(asset)
        val open = lastCandle.close

        val biasFactor = when (biasDirection) {
            TradeDirection.BUY -> 0.65 // Upward movement
            TradeDirection.SELL -> 0.35 // Downward movement
            null -> 0.5
        }

        val change = (Random.nextDouble() - (1.0 - biasFactor)) * volatility
        val close = open + change
        val high = max(open, close) + (Random.nextDouble() * volatility * 0.3)
        val low = min(open, close) - (Random.nextDouble() * volatility * 0.3)

        val nextCandle = MarketCandle(
            timestamp = lastCandle.timestamp + 15 * 60 * 1000,
            open = open,
            high = high,
            low = low,
            close = close
        )

        val tempCombined = currentCandles + nextCandle
        val calculated = computeIndicators(tempCombined)
        return calculated.last()
    }

    /**
     * Calculates 9 EMA, 200 EMA, and RSI values for a list of candles.
     */
    fun computeIndicators(candles: List<MarketCandle>): List<MarketCandle> {
        if (candles.isEmpty()) return emptyList()

        val result = ArrayList<MarketCandle>()
        
        // 1. Calculate EMAs
        var currentEma9 = candles.first().close
        var currentEma200 = candles.first().close

        val alpha9 = 2.0 / (9.0 + 1.0)
        val alpha200 = 2.0 / (200.0 + 1.0)

        // 2. Relative Strength Index (RSI) parameters
        val rsiPeriod = 14
        val gains = DoubleArray(candles.size)
        val losses = DoubleArray(candles.size)

        for (i in 1 until candles.size) {
            val diff = candles[i].close - candles[i - 1].close
            gains[i] = if (diff > 0) diff else 0.0
            losses[i] = if (diff < 0) -diff else 0.0
        }

        for (i in candles.indices) {
            val candle = candles[i]
            
            // EMA calculation
            if (i > 0) {
                currentEma9 = (candle.close * alpha9) + (currentEma9 * (1.0 - alpha9))
                currentEma200 = (candle.close * alpha200) + (currentEma200 * (1.0 - alpha200))
            } else {
                currentEma9 = candle.close
                currentEma200 = candle.close
            }

            // RSI calculation
            var rsi = 50.0
            if (i >= rsiPeriod) {
                var avgGain = 0.0
                var avgLoss = 0.0
                
                // First average
                if (i == rsiPeriod) {
                    for (j in 1..rsiPeriod) {
                        avgGain += gains[j]
                        avgLoss += losses[j]
                    }
                    avgGain /= rsiPeriod
                    avgLoss /= rsiPeriod
                } else {
                    // Wilders smoothing
                    val prevAvgGain = result[i - 1].rsi // placeholder to fetch prev
                    // For calculation, let's look at preceding items in result
                    var prevGainAvg = 0.0
                    var prevLossAvg = 0.0
                    for (j in (i - rsiPeriod + 1)..i) {
                        prevGainAvg += gains[j]
                        prevLossAvg += losses[j]
                    }
                    avgGain = prevGainAvg / rsiPeriod
                    avgLoss = prevLossAvg / rsiPeriod
                }

                rsi = if (avgLoss == 0.0) {
                    100.0
                } else {
                    val rs = avgGain / avgLoss
                    100.0 - (100.0 / (1.0 + rs))
                }
            } else {
                // Approximate RSI for starting values
                rsi = 50.0 + (Random.nextDouble() - 0.5) * 15.0
            }

            // Bound RSI
            rsi = rsi.coerceIn(5.0, 95.0)

            // FVG Detection (Unmitigated Fair Value Gap)
            // A bullish FVG occurs when Low(Candle i) > High(Candle i-2)
            // A bearish FVG occurs when High(Candle i) < Low(Candle i-2)
            var isFVG = false
            var fvgTop = 0.0
            var fvgBottom = 0.0

            if (i >= 2) {
                val c1 = candles[i - 2]
                val c2 = candles[i - 1]
                val c3 = candle

                // Bullish displacement gap
                if (c3.low > c1.high && c2.close > c2.open) {
                    isFVG = true
                    fvgTop = c3.low
                    fvgBottom = c1.high
                }
                // Bearish displacement gap
                else if (c3.high < c1.low && c2.close < c2.open) {
                    isFVG = true
                    fvgTop = c1.low
                    fvgBottom = c3.high
                }
            }

            result.add(
                candle.copy(
                    ema9 = currentEma9,
                    ema200 = currentEma200,
                    rsi = rsi,
                    isFVG = isFVG,
                    fvgTop = fvgTop,
                    fvgBottom = fvgBottom
                )
            )
        }

        return result
    }

    /**
     * Determines the Institutional Trading Range (HTF range on H4/D1) dynamically.
     * We look for the most dominant high-volume impulse candle in the historic feeds.
     */
    fun findHTFRange(candles: List<MarketCandle>): Pair<Double, Double> {
        if (candles.isEmpty()) return Pair(0.0, 0.0)
        
        // Find candle with largest high-low spread (impulse candle)
        var maxSpread = 0.0
        var bestCandle = candles.first()

        for (candle in candles) {
            val spread = candle.high - candle.low
            if (spread > maxSpread) {
                maxSpread = spread
                bestCandle = candle
            }
        }

        // Return its High and Low as the range
        return Pair(bestCandle.high, bestCandle.low)
    }

    /**
     * Scan the latest market data to identify a complete CRT Setup.
     * Returns a valid Setup if detected, else null.
     */
    fun detectSetup(
        asset: AssetType,
        candles: List<MarketCandle>,
        htfRange: Pair<Double, Double>,
        session: ICTKillzone,
        config: StrategyConfig = StrategyConfig()
    ): StrategySetup? {
        if (candles.size < 5 || htfRange.first == 0.0 || session == ICTKillzone.OFF_SESSION) return null

        val htfHigh = htfRange.first
        val htfLow = htfRange.second

        val lastIndex = candles.lastIndex
        val currentCandle = candles[lastIndex]
        val prevCandle = candles[lastIndex - 1]

        // 1. LIQUIDITY SWEEP CHECK (Turtles Body Soup setup)
        // Check if price wick has breached the extreme and is starting to return inside
        var isBullishSweep = false
        var isBearishSweep = false
        var sweepPrice = 0.0
        var stopLoss = 0.0

        // Bullish Turtle Soup: Low sweeps HTF Low and returns above
        if (prevCandle.low < htfLow && prevCandle.close > htfLow) {
            isBullishSweep = true
            sweepPrice = prevCandle.low
            stopLoss = prevCandle.low - (getVolatility(asset) * config.turtleWickThresholdPct) // SL at wick extreme minus buffer
        }
        // Bearish Turtle Soup: High sweeps HTF High and returns below
        else if (prevCandle.high > htfHigh && prevCandle.close < htfHigh) {
            isBearishSweep = true
            sweepPrice = prevCandle.high
            stopLoss = prevCandle.high + (getVolatility(asset) * config.turtleWickThresholdPct) // SL at wick extreme plus buffer
        }

        if (!isBullishSweep && !isBearishSweep) return null

        // 2. CONFLUENCES: RSI DIVERGENCE CHECK (with configurable turtleLookback)
        var rsiDivergence = false
        val rsiCurrent = prevCandle.rsi

        if (isBullishSweep) {
            var earlierMinPrice = Double.MAX_VALUE
            var earlierMinRsi = Double.MAX_VALUE
            for (idx in max(0, lastIndex - config.turtleLookback) until (lastIndex - 1)) {
                val c = candles[idx]
                if (c.low < earlierMinPrice) {
                    earlierMinPrice = c.low
                    earlierMinRsi = c.rsi
                }
            }
            if (prevCandle.low <= earlierMinPrice && rsiCurrent > earlierMinRsi) {
                rsiDivergence = true
            }
        } else if (isBearishSweep) {
            var earlierMaxPrice = 0.0
            var earlierMaxRsi = 0.0
            for (idx in max(0, lastIndex - config.turtleLookback) until (lastIndex - 1)) {
                val c = candles[idx]
                if (c.high > earlierMaxPrice) {
                    earlierMaxPrice = c.high
                    earlierMaxRsi = c.rsi
                }
            }
            if (prevCandle.high >= earlierMaxPrice && rsiCurrent < earlierMaxRsi) {
                rsiDivergence = true
            }
        }

        // 3. MARKET STRUCTURE SHIFT & DISPLACEMENT
        var isMssConfirmed = false
        val ema9 = currentCandle.ema9
        val ema200 = currentCandle.ema200

        if (isBullishSweep && currentCandle.close > ema9 && currentCandle.close > ema200) {
            isMssConfirmed = true
        } else if (isBearishSweep && currentCandle.close < ema9 && currentCandle.close < ema200) {
            isMssConfirmed = true
        }

        if (!isMssConfirmed) return null

        // 4. CONFLUENCE: BREAK OF STRUCTURE (BoS)
        var hasBos = false
        if (isBullishSweep) {
            val maxHigh = candles.takeLast(min(candles.size, config.turtleLookback)).maxOfOrNull { it.high } ?: htfHigh
            hasBos = currentCandle.close >= maxHigh || currentCandle.close > prevCandle.high
        } else {
            val minLow = candles.takeLast(min(candles.size, config.turtleLookback)).minOfOrNull { it.low } ?: htfLow
            hasBos = currentCandle.close <= minLow || currentCandle.close < prevCandle.low
        }

        if (config.enableBosConfirmation && !hasBos) return null

        // 5. CONFLUENCE: ORDER BLOCK (OB) MITIGATION
        var hasObMitigation = false
        for (idx in max(0, lastIndex - 5) until lastIndex) {
            val c = candles[idx]
            if (isBullishSweep && c.close < c.open) { // bearish candle (bullish OB)
                val testPrice = (currentCandle.close + prevCandle.low) / 2.0
                if (testPrice >= c.low && testPrice <= c.high * 1.05) {
                    hasObMitigation = true
                    break
                }
            } else if (isBearishSweep && c.close > c.open) { // bullish candle (bearish OB)
                val testPrice = (currentCandle.close + prevCandle.high) / 2.0
                if (testPrice >= c.low * 0.95 && testPrice <= c.high) {
                    hasObMitigation = true
                    break
                }
            }
        }

        // Keep a realistic fallback so we don't block all trades, but honor config
        if (config.enableObMitigation && !hasObMitigation) {
            // Trigger a softer condition if structure is extremely strong
            if (!rsiDivergence) return null
        }

        // 6. FIND AN UNMITIGATED FVG IN THE RECENT DISPLACEMENT LEG
        var fvgMidpoint = 0.0
        var fvgFound = false

        for (idx in max(0, lastIndex - config.fvgLookback)..lastIndex) {
            val c = candles[idx]
            if (c.isFVG) {
                fvgMidpoint = (c.fvgTop + c.fvgBottom) / 2.0
                fvgFound = true
                break
            }
        }

        if (!fvgFound) {
            fvgMidpoint = if (isBullishSweep) {
                currentCandle.close - (currentCandle.close - prevCandle.low) * 0.5
            } else {
                currentCandle.close + (prevCandle.high - currentCandle.close) * 0.5
            }
        }

        val takeProfit = if (isBullishSweep) htfHigh else htfLow

        val setupTypeDetails = buildString {
            append("Turtle Soup ${if (isBullishSweep) "Bullish Sweep" else "Bearish Sweep"}")
            if (hasBos) append(" + BoS")
            if (hasObMitigation) append(" + OB Mitigation")
            if (fvgFound) append(" + FVG Fill")
        }

        return StrategySetup(
            asset = asset,
            htfHigh = htfHigh,
            htfLow = htfLow,
            direction = if (isBullishSweep) TradeDirection.BUY else TradeDirection.SELL,
            sweepPrice = sweepPrice,
            mssPrice = currentCandle.close,
            fvgMidpoint = fvgMidpoint,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            rsiAtSweep = rsiCurrent,
            rsiDivergenceConfirmed = rsiDivergence,
            session = session,
            isCrtConfirmed = isMssConfirmed && rsiDivergence,
            tradedAtBos = hasBos,
            tradedAtOb = hasObMitigation,
            tradedAtFvg = fvgFound,
            crtDetails = setupTypeDetails
        )
    }

    /**
     * Calculates the exact prices of the 4 scaled limit orders for our trade configuration.
     * We space the 4 entries evenly across the Fair Value Gap / Displacement entry zone.
     */
    fun calculateScaledEntries(setup: StrategySetup): List<Double> {
        val entry1 = setup.fvgMidpoint // Core FVG midpoint
        val entry2 = setup.mssPrice // MSS Entry level
        val entry3 = (setup.fvgMidpoint + setup.mssPrice) / 2.0 // Midway FVG/MSS
        val entry4 = (setup.fvgMidpoint + setup.stopLoss) / 2.0 // Deep discount/premium entry closer to Stop Loss

        return listOf(entry1, entry2, entry3, entry4)
    }
}
