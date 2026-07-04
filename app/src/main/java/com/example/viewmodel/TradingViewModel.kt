package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TradeRepository
import com.example.model.AssetType
import com.example.model.DailyPnL
import com.example.model.ICTKillzone
import com.example.model.MarketCandle
import com.example.model.StrategySetup
import com.example.model.TradeDirection
import com.example.model.TradeLog
import com.example.model.EconomicEvent
import com.example.model.ImpactLevel
import com.example.model.EventStatus
import com.example.model.StrategyConfig
import com.example.service.StrategyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.random.Random

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TradeRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TradeRepository(database)
        
        // Populate initial P&L simulation trends if empty
        viewModelScope.launch(Dispatchers.IO) {
            initDailyPnL()
            initInitialCandles()
            initRsiDivergences()
            initEconomicEvents()
            startSimulationLoop()
        }
    }

    // Tab navigation
    private val _currentTab = MutableStateFlow("Monitor")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    // Active asset selection
    private val _selectedAsset = MutableStateFlow(AssetType.BTC_USD)
    val selectedAsset: StateFlow<AssetType> = _selectedAsset.asStateFlow()

    fun selectAsset(asset: AssetType) {
        _selectedAsset.value = asset
    }

    // Account Balance & Stats
    private val _accountBalance = MutableStateFlow(10000.0)
    val accountBalance: StateFlow<Double> = _accountBalance.asStateFlow()

    private val _equity = MutableStateFlow(10000.0)
    val equity: StateFlow<Double> = _equity.asStateFlow()
    val accountEquity: StateFlow<Double> = _equity.asStateFlow()

    // Real-time prices for all assets to keep the dashboard live and engaging
    private val _assetPrices = MutableStateFlow<Map<AssetType, Double>>(
        AssetType.values().associateWith { StrategyEngine.getBasePrice(it) }
    )
    val assetPrices: StateFlow<Map<AssetType, Double>> = _assetPrices.asStateFlow()

    // Candles for the selected asset
    private val _candlesMap = MutableStateFlow<Map<AssetType, List<MarketCandle>>>(emptyMap())
    val marketCandles: StateFlow<Map<AssetType, List<MarketCandle>>> = _candlesMap.asStateFlow()

    val selectedAssetCandles: StateFlow<List<MarketCandle>> = combine(
        _selectedAsset, _candlesMap
    ) { asset, map ->
        map[asset] ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // HTF Ranges for all assets
    val htfRanges: StateFlow<Map<AssetType, Pair<Double, Double>>> = _candlesMap.map { map ->
        map.mapValues { entry -> StrategyEngine.findHTFRange(entry.value) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // HTF Range for the selected asset (D1/H4 High and Low)
    val selectedHTFRange: StateFlow<Pair<Double, Double>> = selectedAssetCandles.map { candles ->
        StrategyEngine.findHTFRange(candles)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    // Active/Pending/Closed Trades from DB
    val allTrades: StateFlow<List<TradeLog>> = repository.allTrades.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val activeTrades: StateFlow<List<TradeLog>> = repository.activeTrades.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val pendingTrades: StateFlow<List<TradeLog>> = repository.pendingTrades.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val closedTrades: StateFlow<List<TradeLog>> = repository.closedTrades.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Daily Profit & Loss Trend (5-day trend for Visualizer bar chart)
    val dailyPnLTrend: StateFlow<List<DailyPnL>> = repository.dailyPnLTrend.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // SAST Session status
    private val _sastHour = MutableStateFlow(14) // Start in NY KillZone for excitement!
    val sastHour: StateFlow<Int> = _sastHour.asStateFlow()

    val currentSession: StateFlow<ICTKillzone> = _sastHour.map { hour ->
        ICTKillzone.getCurrentSession(hour)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ICTKillzone.NEW_YORK)

    // Is Auto Trading Bot active
    private val _isBotActive = MutableStateFlow(true)
    val isBotActive: StateFlow<Boolean> = _isBotActive.asStateFlow()

    // Simulation toggle (isSimulating)
    private val _isSimulating = MutableStateFlow(true)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    // Risk configuration
    private val _riskConfig = MutableStateFlow(RiskConfig())
    val riskConfig: StateFlow<RiskConfig> = _riskConfig.asStateFlow()

    // Strategy configuration
    private val _strategyConfig = MutableStateFlow(StrategyConfig())
    val strategyConfig: StateFlow<StrategyConfig> = _strategyConfig.asStateFlow()

    // Recent Detected Strategy Setups (signals feed)
    private val _recentSetups = MutableStateFlow<List<StrategySetup>>(emptyList())
    val recentSetups: StateFlow<List<StrategySetup>> = _recentSetups.asStateFlow()

    // RSI Divergences Log
    private val _rsiDivergences = MutableStateFlow<List<com.example.model.RsiDivergenceEvent>>(emptyList())
    val rsiDivergences: StateFlow<List<com.example.model.RsiDivergenceEvent>> = _rsiDivergences.asStateFlow()

    // Forex Factory Economic Events State
    private val _economicEvents = MutableStateFlow<List<EconomicEvent>>(emptyList())
    val economicEvents: StateFlow<List<EconomicEvent>> = _economicEvents.asStateFlow()

    // Active alert banner state for Red events/speeches
    private val _latestEconomicAlert = MutableStateFlow<EconomicEvent?>(null)
    val latestEconomicAlert: StateFlow<EconomicEvent?> = _latestEconomicAlert.asStateFlow()

    // Terminal log mechanism
    private val _terminalLogs = MutableStateFlow<List<String>>(
        listOf(
            "CRT Terminal v1.4 initialized.",
            "SAST GMT+2 timezone sync completed.",
            "Listening for liquidity sweeps..."
        )
    )
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    fun toggleBot() {
        _isBotActive.value = !_isBotActive.value
        addLog("Trading bot ${if (_isBotActive.value) "ENABLED" else "DISABLED"}")
    }

    fun setSimulating(value: Boolean) {
        _isSimulating.value = value
        addLog("Market simulator ${if (value) "RESUMED" else "PAUSED"}")
    }

    fun updateRiskConfig(config: RiskConfig) {
        _riskConfig.value = config
        addLog("Risk config updated: Risk=${String.format(Locale.US, "%.1f%%", config.riskPercent)} | Splits=${config.maxPreciseEntries} | SL=${config.stopLossMode}")
    }

    fun updateStrategyConfig(config: StrategyConfig) {
        _strategyConfig.value = config
        addLog("Strategy params updated: FVG lookback=${config.fvgLookback} | Turtle lookback=${config.turtleLookback} | Timeframe=${config.timeFrameMinutes}m | Wick=${config.turtleWickThresholdPct} | BoS=${config.enableBosConfirmation} | OB=${config.enableObMitigation}")
    }

    fun forceTick() {
        viewModelScope.launch(Dispatchers.IO) {
            simulateTick()
        }
    }

    fun resetSimulation() {
        resetAccount()
    }

    fun addLog(message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+2") // SAST
        }
        val timestamp = sdf.format(Date())
        val formattedMsg = "[$timestamp] $message"
        _terminalLogs.value = (_terminalLogs.value + formattedMsg).takeLast(60)
    }

    private suspend fun initDailyPnL() {
        repository.clearDailyPnL()
        val initialPnL = listOf(
            DailyPnL("MON", 120.0, 120.0, System.currentTimeMillis() - 4 * 24 * 3600 * 1000),
            DailyPnL("TUE", -40.0, 80.0, System.currentTimeMillis() - 3 * 24 * 3600 * 1000),
            DailyPnL("WED", 280.0, 360.0, System.currentTimeMillis() - 2 * 24 * 3600 * 1000),
            DailyPnL("THU", 90.0, 450.0, System.currentTimeMillis() - 1 * 24 * 3600 * 1000),
            DailyPnL("FRI", 150.0, 600.0, System.currentTimeMillis())
        )
        repository.insertDailyPnL(initialPnL)
    }

    private suspend fun initInitialCandles() {
        val initialMap = AssetType.values().associateWith { asset ->
            StrategyEngine.generateInitialCandles(asset, 30)
        }
        _candlesMap.value = initialMap
    }

    private fun startSimulationLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(3000) // fast ticks to make the terminal visually dynamic and fun!
                if (_isSimulating.value) {
                    simulateTick()
                }
            }
        }
    }

    private suspend fun simulateTick() {
        // 1. Advance SAST clock slightly
        var nextHour = _sastHour.value + 1
        if (nextHour >= 24) nextHour = 0
        _sastHour.value = nextHour

        // Check for scheduled economic calendar releases
        checkEconomicReleases(nextHour)

        val isWeekend = false // Typically weekend

        // 2. Generate small fluctuations for all asset prices
        val updatedPrices = _assetPrices.value.toMutableMap()
        val currentMap = _candlesMap.value.toMutableMap()

        AssetType.values().forEach { asset ->
            val candles = currentMap[asset] ?: return@forEach
            val lastCandle = candles.lastOrNull() ?: return@forEach

            // Randomly determine if we create a complete new candle (e.g., 25% chance per simulation tick)
            val isNewCandle = Random.nextDouble() < 0.25
            
            val updatedCandles = if (isNewCandle) {
                // Check if there is an active setup bias to steer the next candle towards profit/loss
                val activeTradesOnAsset = repository.activeTrades.stateIn(viewModelScope).value
                    .filter { it.asset == asset.displayName }
                val bias = if (activeTradesOnAsset.isNotEmpty()) {
                    val trade = activeTradesOnAsset.first()
                    if (trade.direction == "BUY") TradeDirection.BUY else TradeDirection.SELL
                } else {
                    null
                }
                
                val nextCandle = StrategyEngine.generateNextCandle(asset, candles, bias)
                val newList = (candles + nextCandle).takeLast(40) // Keep standard window size
                currentMap[asset] = newList
                newList
            } else {
                // Micro-tick price update within the current candle
                val tickVolatility = StrategyEngine.getVolatility(asset) * 0.15
                val change = (Random.nextDouble() - 0.5) * tickVolatility
                val currentPrice = lastCandle.close + change
                
                val updatedLast = lastCandle.copy(
                    close = currentPrice,
                    high = maxOf(lastCandle.high, currentPrice),
                    low = minOf(lastCandle.low, currentPrice)
                )
                val newList = candles.dropLast(1) + updatedLast
                currentMap[asset] = newList
                newList
            }

            val latestPrice = updatedCandles.last().close
            updatedPrices[asset] = latestPrice

            // Run real-time RSI divergence detection on the updated candles
            checkRsiDivergence(asset, updatedCandles)

            // 3. Bot Logic: Scan for setup and trigger entries
            if (_isBotActive.value) {
                val currentSessionVal = ICTKillzone.getCurrentSession(_sastHour.value)
                
                // Weekend Trading check: Non-crypto is frozen on weekends
                val canTrade = asset.isWeekendTraded || !isWeekend
                
                if (canTrade && currentSessionVal != ICTKillzone.OFF_SESSION) {
                    val htfRange = htfRanges.value[asset] ?: StrategyEngine.findHTFRange(updatedCandles)
                    val setup = StrategyEngine.detectSetup(asset, updatedCandles, htfRange, currentSessionVal, _strategyConfig.value)
                    
                    if (setup != null) {
                        // Store setup in recent signals feed
                        _recentSetups.value = (listOf(setup) + _recentSetups.value).take(20)

                        // Check if we already have an active/pending trade for this asset to avoid over-exposure
                        val existingTrades = repository.allTrades.stateIn(viewModelScope).value
                            .any { it.asset == asset.displayName && it.status != "CLOSED" }

                        if (!existingTrades) {
                            triggerPreciseFourEntries(setup)
                        }
                    }
                }
            }
        }

        _assetPrices.value = updatedPrices
        _candlesMap.value = currentMap

        // 4. Update and manage active & pending trades
        updateActiveTrades(updatedPrices)
    }

    /**
     * Executes the "precise scaled entries" pattern for the detected strategy setup.
     */
    private suspend fun triggerPreciseFourEntries(setup: StrategySetup) {
        val config = _riskConfig.value
        val scaledPrices = StrategyEngine.calculateScaledEntries(setup)
        
        // Count entries we want to place
        val entryCount = config.maxPreciseEntries // e.g. 1 to 4 entries
        
        // Calculate dynamic stop loss based on stopLossMode
        val finalStopLoss = if (config.stopLossMode == "HTF_PERCENT") {
            val htfHeight = abs(setup.htfHigh - setup.htfLow)
            val slDistance = htfHeight * config.stopLossHtfPercent
            if (setup.direction.name == "BUY") {
                setup.fvgMidpoint - slDistance
            } else {
                setup.fvgMidpoint + slDistance
            }
        } else {
            setup.stopLoss // "WICK" mode (default sweep rejection extreme)
        }
        
        // Calculate lot size per entry
        val totalLotSize = if (config.positionSizingMode == "AUTO") {
            // Dynamic position sizing
            val balance = _accountBalance.value
            val riskAmt = balance * (config.riskPercent / 100.0)
            val pointsRisk = abs(setup.fvgMidpoint - finalStopLoss)
            
            // Typical pip/tick values for forex, indices, crypto
            val pipValue = when {
                setup.asset.category == "Crypto" -> 1.0
                setup.asset.category == "Indices" -> 5.0
                else -> 10000.0 // Forex
            }
            if (pointsRisk > 0) {
                riskAmt / (pointsRisk * pipValue)
            } else {
                config.fixedLotSize
            }
        } else {
            config.fixedLotSize
        }
        
        val sizePerEntry = (totalLotSize / entryCount).coerceAtLeast(0.01)
        
        addLog("🎯 [SETUP] ${setup.direction} detected on ${setup.asset.displayName} in ${setup.session.displayName}")
        addLog("   Sweep Price: ${String.format(Locale.US, "%,.4f", setup.sweepPrice)} | RSI: ${String.format(Locale.US, "%.1f", setup.rsiAtSweep)}")

        // Place limit orders
        for (index in 0 until entryCount) {
            val limitPrice = scaledPrices[index]
            val trade = TradeLog(
                asset = setup.asset.displayName,
                direction = setup.direction.name,
                entryPrice = limitPrice,
                averagePrice = limitPrice,
                size = sizePerEntry,
                profit = 0.0,
                stopLoss = finalStopLoss,
                takeProfit = setup.takeProfit,
                status = "PENDING",
                session = setup.session.name,
                setupReason = "CRT Sweep + LTF MSS + RSI Div [Entry ${index + 1}/$entryCount]",
                entriesFilledCount = index + 1
            )
            repository.insertTrade(trade)
            addLog("   Limit order #${index + 1} placed at ${String.format(Locale.US, "%,.4f", limitPrice)} | Lot: ${String.format(Locale.US, "%.2f", sizePerEntry)}")
        }
    }

    /**
     * Process ongoing active/pending trades based on new real-time price ticks.
     */
    private suspend fun updateActiveTrades(currentPrices: Map<AssetType, Double>) {
        val allCurrentTrades = repository.allTrades.stateIn(viewModelScope).value
        var unrealizedProfitSum = 0.0

        for (trade in allCurrentTrades) {
            val assetType = AssetType.values().firstOrNull { it.displayName == trade.asset } ?: continue
            val price = currentPrices[assetType] ?: continue

            if (trade.status == "PENDING") {
                // Check if price crossed the entry trigger to fill the pending limit order
                val filled = if (trade.direction == "BUY") {
                    price <= trade.entryPrice
                } else {
                    price >= trade.entryPrice
                }

                if (filled) {
                    val activated = trade.copy(status = "ACTIVE", timestamp = System.currentTimeMillis())
                    repository.updateTrade(activated)
                    addLog("🟢 [FILLED] limit order #${trade.entriesFilledCount} filled on ${trade.asset} at ${String.format(Locale.US, "%,.4f", trade.entryPrice)}")
                }
            } else if (trade.status == "ACTIVE") {
                // Calculate PnL
                val pnlMultiplier = if (trade.direction == "BUY") 1.0 else -1.0
                // Calculate point/pip value based on asset category
                val rawDiff = (price - trade.entryPrice) * pnlMultiplier
                val pipValue = when {
                    assetType.category == "Crypto" -> 1.0
                    assetType.category == "Indices" -> 5.0
                    else -> 10000.0 // Forex
                }
                val profit = rawDiff * trade.size * pipValue
                unrealizedProfitSum += profit

                // Check Stop Loss & Take Profit
                val isStoppedOut = if (trade.direction == "BUY") {
                    price <= trade.stopLoss
                } else {
                    price >= trade.stopLoss
                }

                // Trailing TP or Dynamic SL based on RiskConfig
                val config = _riskConfig.value
                val htfRangesMap = htfRanges.value
                val htfRange = htfRangesMap[assetType] ?: Pair(0.0, 0.0)
                val htfMidpoint = (htfRange.first + htfRange.second) / 2.0
                
                // If trailing TP mode is PROGRESS, once price crosses midpoint, lock in some profits or adjust
                var activeStopLoss = trade.stopLoss
                if (config.trailingTpMode == "PROGRESS") {
                    if (trade.direction == "BUY" && price >= htfMidpoint) {
                        // Move SL to Breakeven
                        if (activeStopLoss < trade.entryPrice) {
                            activeStopLoss = trade.entryPrice
                            addLog("🛡️ [TRAILING SL] Moved Stop Loss to Break-Even for BUY on ${trade.asset}")
                        }
                    } else if (trade.direction == "SELL" && price <= htfMidpoint) {
                        // Move SL to Breakeven
                        if (activeStopLoss > trade.entryPrice) {
                            activeStopLoss = trade.entryPrice
                            addLog("🛡️ [TRAILING SL] Moved Stop Loss to Break-Even for SELL on ${trade.asset}")
                        }
                    }
                }

                val isTargetHit = if (trade.direction == "BUY") {
                    price >= trade.takeProfit
                } else {
                    price <= trade.takeProfit
                }

                if (isStoppedOut || isTargetHit) {
                    val finalProfit = if (isStoppedOut) {
                        val slDiff = (activeStopLoss - trade.entryPrice) * pnlMultiplier
                        slDiff * trade.size * pipValue
                    } else {
                        val tpDiff = (trade.takeProfit - trade.entryPrice) * pnlMultiplier
                        tpDiff * trade.size * pipValue
                    }

                    val closed = trade.copy(
                        status = "CLOSED",
                        profit = finalProfit,
                        closedTimestamp = System.currentTimeMillis(),
                        finalBalance = _accountBalance.value + finalProfit
                    )
                    repository.updateTrade(closed)

                    // Update local balance state and persistent P&L trend
                    withContext(Dispatchers.Main) {
                        _accountBalance.value += finalProfit
                        updateDailyPnLWithClosedTrade(finalProfit)
                    }
                    
                    if (isStoppedOut) {
                        addLog("🛑 [STOP LOSS] ${trade.direction} on ${trade.asset} hit SL at ${String.format(Locale.US, "%,.4f", price)} | Net: -$${String.format(Locale.US, "%,.2f", abs(finalProfit))}")
                    } else {
                        addLog("🎉 [TAKE PROFIT] ${trade.direction} on ${trade.asset} hit TP at ${String.format(Locale.US, "%,.4f", price)} | Net: +$${String.format(Locale.US, "%,.2f", finalProfit)}")
                    }
                } else {
                    // Update current live profit and stopLoss (if trailed)
                    val updated = trade.copy(profit = profit, stopLoss = activeStopLoss)
                    repository.updateTrade(updated)
                }
            }
        }

        // Live account equity
        _equity.value = _accountBalance.value + unrealizedProfitSum
    }

    private suspend fun updateDailyPnLWithClosedTrade(profit: Double) {
        val currentTrend = repository.dailyPnLTrend.stateIn(viewModelScope).value.toMutableList()
        if (currentTrend.isNotEmpty()) {
            val lastDay = currentTrend.last()
            val updatedLast = lastDay.copy(
                profitLoss = lastDay.profitLoss + profit,
                cumulativePnL = lastDay.cumulativePnL + profit
            )
            currentTrend[currentTrend.lastIndex] = updatedLast
            repository.insertDailyPnL(currentTrend)
        }
    }

    fun resetAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllTrades()
            initDailyPnL()
            initInitialCandles()
            initRsiDivergences()
            withContext(Dispatchers.Main) {
                _accountBalance.value = 10000.0
                _equity.value = 10000.0
                _terminalLogs.value = listOf(
                    "Terminal databases cleared.",
                    "Account balance reset to $10,000.00.",
                    "Simulating new market cycle..."
                )
            }
        }
    }

    private fun initRsiDivergences() {
        val now = System.currentTimeMillis()
        val initialEvents = listOf(
            com.example.model.RsiDivergenceEvent(
                asset = AssetType.BTC_USD,
                direction = TradeDirection.BUY,
                priceBefore = 62100.0,
                priceAfter = 61850.0,
                rsiBefore = 28.5,
                rsiAfter = 34.2,
                timestamp = now - 45 * 60 * 1000,
                isRead = true
            ),
            com.example.model.RsiDivergenceEvent(
                asset = AssetType.GBP_USD,
                direction = TradeDirection.SELL,
                priceBefore = 1.27550,
                priceAfter = 1.27620,
                rsiBefore = 72.1,
                rsiAfter = 66.8,
                timestamp = now - 32 * 60 * 1000,
                isRead = true
            ),
            com.example.model.RsiDivergenceEvent(
                asset = AssetType.NAS100,
                direction = TradeDirection.BUY,
                priceBefore = 18520.0,
                priceAfter = 18490.0,
                rsiBefore = 22.1,
                rsiAfter = 29.8,
                timestamp = now - 18 * 60 * 1000,
                isRead = false
            ),
            com.example.model.RsiDivergenceEvent(
                asset = AssetType.ETH_USD,
                direction = TradeDirection.SELL,
                priceBefore = 3445.0,
                priceAfter = 3462.0,
                rsiBefore = 78.3,
                rsiAfter = 69.5,
                timestamp = now - 5 * 60 * 1000,
                isRead = false
            )
        )
        _rsiDivergences.value = initialEvents
    }

    private fun checkRsiDivergence(asset: AssetType, candles: List<MarketCandle>) {
        if (candles.size < 6) return

        val valleys = mutableListOf<MarketCandle>()
        val peaks = mutableListOf<MarketCandle>()

        for (i in 2 until candles.size - 1) {
            val prev = candles[i - 1]
            val curr = candles[i]
            val next = candles[i + 1]

            if (curr.low < prev.low && curr.low < next.low) {
                valleys.add(curr)
            }
            if (curr.high > prev.high && curr.high > next.high) {
                peaks.add(curr)
            }
        }

        var detectedEvent: com.example.model.RsiDivergenceEvent? = null

        // 1. Bullish Divergence
        if (valleys.size >= 2) {
            val v1 = valleys.last()
            val v2 = valleys[valleys.size - 2]
            if (v1.timestamp > v2.timestamp) {
                if (v1.low <= v2.low && v1.rsi > v2.rsi + 1.5) {
                    val v1Index = candles.indexOf(v1)
                    if (candles.size - v1Index <= 5) {
                        detectedEvent = com.example.model.RsiDivergenceEvent(
                            asset = asset,
                            direction = TradeDirection.BUY,
                            priceBefore = v2.low,
                            priceAfter = v1.low,
                            rsiBefore = v2.rsi,
                            rsiAfter = v1.rsi,
                            timestamp = v1.timestamp
                        )
                    }
                }
            }
        }

        // 2. Bearish Divergence
        if (peaks.size >= 2 && detectedEvent == null) {
            val p1 = peaks.last()
            val p2 = peaks[peaks.size - 2]
            if (p1.timestamp > p2.timestamp) {
                if (p1.high >= p2.high && p1.rsi < p2.rsi - 1.5) {
                    val p1Index = candles.indexOf(p1)
                    if (candles.size - p1Index <= 5) {
                        detectedEvent = com.example.model.RsiDivergenceEvent(
                            asset = asset,
                            direction = TradeDirection.SELL,
                            priceBefore = p2.high,
                            priceAfter = p1.high,
                            rsiBefore = p2.rsi,
                            rsiAfter = p1.rsi,
                            timestamp = p1.timestamp
                        )
                    }
                }
            }
        }

        if (detectedEvent != null) {
            val currentList = _rsiDivergences.value
            val alreadyLogged = currentList.any { 
                it.asset == detectedEvent.asset && 
                it.direction == detectedEvent.direction && 
                (it.timestamp == detectedEvent.timestamp || abs(it.timestamp - detectedEvent.timestamp) < 5 * 60 * 1000)
            }
            if (!alreadyLogged) {
                _rsiDivergences.value = (listOf(detectedEvent) + currentList).take(50)
                addLog("📡 [RSI DIV] ${if (detectedEvent.direction == TradeDirection.BUY) "BULLISH" else "BEARISH"} Divergence on ${asset.displayName}! Price: ${String.format(Locale.US, "%.4f", detectedEvent.priceAfter)} | RSI: ${String.format(Locale.US, "%.1f", detectedEvent.rsiAfter)}")
            }
        }
    }

    fun markAllDivergencesAsRead() {
        _rsiDivergences.value = _rsiDivergences.value.map { it.copy(isRead = true) }
    }

    fun clearDivergencesLog() {
        _rsiDivergences.value = emptyList()
    }

    private fun initEconomicEvents() {
        val events = listOf(
            EconomicEvent(
                title = "RBA Rate Statement & Interest Rate",
                currency = "AUD",
                impact = ImpactLevel.HIGH,
                scheduledHourSAST = 3,
                scheduledMinuteSAST = 0,
                forecast = "4.35%",
                previous = "4.35%",
                impactExplanation = "High impact on AUD pairs. Higher rate is bullish for AUD.",
                targetAssetCategory = "Forex"
            ),
            EconomicEvent(
                title = "ECB President Christine Lagarde Speech",
                currency = "EUR",
                impact = ImpactLevel.SPEECH,
                scheduledHourSAST = 8,
                scheduledMinuteSAST = 15,
                forecast = "N/A",
                previous = "N/A",
                impactExplanation = "Lagarde's speech expected to address policy easing. Major EUR volatility.",
                targetAssetCategory = "Forex"
            ),
            EconomicEvent(
                title = "BOE Monetary Policy Summary & Rate",
                currency = "GBP",
                impact = ImpactLevel.HIGH,
                scheduledHourSAST = 10,
                scheduledMinuteSAST = 30,
                forecast = "5.00%",
                previous = "5.25%",
                impactExplanation = "Red Folder event. Rate decisions shape the medium-term cable trend.",
                targetAssetCategory = "Forex"
            ),
            EconomicEvent(
                title = "USD Core CPI m/m (Inflation)",
                currency = "USD",
                impact = ImpactLevel.HIGH,
                scheduledHourSAST = 14,
                scheduledMinuteSAST = 30,
                forecast = "0.3%",
                previous = "0.2%",
                impactExplanation = "Primary inflation index. High deviations shock Indices and Crypto.",
                targetAssetCategory = "All"
            ),
            EconomicEvent(
                title = "Federal Reserve Chair Powell Speech",
                currency = "USD",
                impact = ImpactLevel.SPEECH,
                scheduledHourSAST = 15,
                scheduledMinuteSAST = 30,
                forecast = "N/A",
                previous = "N/A",
                impactExplanation = "Powell's commentary on neutral rates. High risk of multi-asset sweeps.",
                targetAssetCategory = "All"
            ),
            EconomicEvent(
                title = "USD ISM Services PMI",
                currency = "USD",
                impact = ImpactLevel.HIGH,
                scheduledHourSAST = 16,
                scheduledMinuteSAST = 0,
                forecast = "51.2",
                previous = "50.8",
                impactExplanation = "Leading economic activity indicator. Above 50 is bullish for USD.",
                targetAssetCategory = "Indices"
            ),
            EconomicEvent(
                title = "FOMC Statement & Interest Rate",
                currency = "USD",
                impact = ImpactLevel.HIGH,
                scheduledHourSAST = 19,
                scheduledMinuteSAST = 0,
                forecast = "5.25%",
                previous = "5.25%",
                impactExplanation = "Primary USD interest rate release. Fills or sweeps ICT Htf orderblocks.",
                targetAssetCategory = "All"
            ),
            EconomicEvent(
                title = "FOMC Press Conference & Speeches",
                currency = "USD",
                impact = ImpactLevel.SPEECH,
                scheduledHourSAST = 19,
                scheduledMinuteSAST = 30,
                forecast = "N/A",
                previous = "N/A",
                impactExplanation = "Live questions regarding path of inflation. Sweeps Asia/London ranges.",
                targetAssetCategory = "All"
            ),
            EconomicEvent(
                title = "BTC Dev Core Network Speech",
                currency = "BTC",
                impact = ImpactLevel.SPEECH,
                scheduledHourSAST = 21,
                scheduledMinuteSAST = 0,
                forecast = "N/A",
                previous = "N/A",
                impactExplanation = "Key blockchain upgrade discussion. Directly influences Crypto liquidity.",
                targetAssetCategory = "Crypto"
            )
        )
        _economicEvents.value = events
    }

    private fun checkEconomicReleases(hour: Int) {
        val currentList = _economicEvents.value.map { event ->
            if (event.scheduledHourSAST == hour && event.status == EventStatus.UPCOMING) {
                val actualVal = when {
                    event.forecast == "N/A" -> "Live Speech"
                    event.forecast.contains("%") -> {
                        val forecastNum = event.forecast.replace("%", "").toDoubleOrNull() ?: 5.0
                        val deviation = (Random.nextDouble() - 0.5) * 0.3
                        val result = forecastNum + deviation
                        String.format(Locale.US, "%.2f%%", result)
                    }
                    event.forecast.contains(".") -> {
                        val forecastNum = event.forecast.toDoubleOrNull() ?: 50.0
                        val deviation = (Random.nextDouble() - 0.5) * 1.8
                        val result = forecastNum + deviation
                        String.format(Locale.US, "%.1f", result)
                    }
                    else -> "Released"
                }

                val triggeredEvent = event.copy(
                    actual = actualVal,
                    status = EventStatus.RELEASED
                )

                _latestEconomicAlert.value = triggeredEvent

                val emoji = if (event.impact == ImpactLevel.HIGH) "🔴" else "🎤"
                addLog("$emoji [FOREX FACTORY] RELEASE: ${event.currency} - ${event.title}")
                if (event.forecast != "N/A") {
                    addLog("   Actual: $actualVal | Forecast: ${event.forecast} | Previous: ${event.previous}")
                } else {
                    addLog("   Speech Active: ${event.impactExplanation}")
                }

                viewModelScope.launch {
                    injectMarketEventVolatility(triggeredEvent)
                }

                triggeredEvent
            } else {
                event
            }
        }
        _economicEvents.value = currentList
    }

    private fun injectMarketEventVolatility(event: EconomicEvent) {
        val updatedPrices = _assetPrices.value.toMutableMap()
        val currentMap = _candlesMap.value.toMutableMap()

        AssetType.values().forEach { asset ->
            val matches = event.targetAssetCategory == "All" ||
                    (event.targetAssetCategory == "Forex" && asset.category == "Forex") ||
                    (event.targetAssetCategory == "Crypto" && asset.category == "Crypto") ||
                    (event.targetAssetCategory == "Indices" && asset.category == "Indices")

            if (matches) {
                val candles = currentMap[asset] ?: return@forEach
                val lastCandle = candles.lastOrNull() ?: return@forEach

                val directionFactor = if (Random.nextBoolean()) 1.0 else -1.0
                val spikeAmount = StrategyEngine.getVolatility(asset) * directionFactor * (1.8 + Random.nextDouble())
                val updatedPrice = lastCandle.close + spikeAmount

                val updatedCandle = lastCandle.copy(
                    close = updatedPrice,
                    high = maxOf(lastCandle.high, updatedPrice),
                    low = minOf(lastCandle.low, updatedPrice)
                )

                val newList = candles.dropLast(1) + updatedCandle
                currentMap[asset] = newList
                updatedPrices[asset] = updatedPrice

                addLog("💥 Forex Factory Volatility Shock on ${asset.displayName}: Price moved to ${String.format(Locale.US, "%,.4f", updatedPrice)}")
            }
        }
        _candlesMap.value = currentMap
        _assetPrices.value = updatedPrices
    }

    fun dismissEconomicAlert() {
        _latestEconomicAlert.value = null
    }

    fun manuallyTriggerEconomicEvent(event: EconomicEvent) {
        val updatedEvents = _economicEvents.value.map { e ->
            if (e.id == event.id) {
                val actualVal = if (e.forecast == "N/A") "Live Speech" else {
                    if (e.forecast.contains("%")) "0.45%" else "52.1"
                }
                val triggered = e.copy(
                    actual = actualVal,
                    status = EventStatus.RELEASED
                )
                _latestEconomicAlert.value = triggered
                val emoji = if (e.impact == ImpactLevel.HIGH) "🔴" else "🎤"
                addLog("$emoji [MANUAL TRIGGER] ${e.currency} - ${e.title}")
                viewModelScope.launch {
                    injectMarketEventVolatility(triggered)
                }
                triggered
            } else {
                e
            }
        }
        _economicEvents.value = updatedEvents
    }

    fun resetEconomicEvents() {
        initEconomicEvents()
        _latestEconomicAlert.value = null
        addLog("📅 Economic calendar and event notifications reset.")
    }
}
