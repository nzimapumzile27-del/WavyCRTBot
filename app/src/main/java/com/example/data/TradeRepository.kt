package com.example.data

import com.example.model.DailyPnL
import com.example.model.TradeLog
import kotlinx.coroutines.flow.Flow

class TradeRepository(private val db: AppDatabase) {
    private val tradeDao = db.tradeDao()
    private val dailyPnLDao = db.dailyPnLDao()

    val allTrades: Flow<List<TradeLog>> = tradeDao.getAllTrades()
    val activeTrades: Flow<List<TradeLog>> = tradeDao.getTradesByStatus("ACTIVE")
    val pendingTrades: Flow<List<TradeLog>> = tradeDao.getTradesByStatus("PENDING")
    val closedTrades: Flow<List<TradeLog>> = tradeDao.getTradesByStatus("CLOSED")

    val dailyPnLTrend: Flow<List<DailyPnL>> = dailyPnLDao.getAllDailyPnL()

    suspend fun insertTrade(trade: TradeLog): Long {
        return tradeDao.insertTrade(trade)
    }

    suspend fun updateTrade(trade: TradeLog) {
        tradeDao.updateTrade(trade)
    }

    suspend fun clearAllTrades() {
        tradeDao.clearAllTrades()
    }

    suspend fun insertDailyPnL(pnlList: List<DailyPnL>) {
        dailyPnLDao.insertAll(pnlList)
    }

    suspend fun clearDailyPnL() {
        dailyPnLDao.clearDailyPnL()
    }
}
