package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.TradeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_logs ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<TradeLog>>

    @Query("SELECT * FROM trade_logs WHERE status = :status ORDER BY timestamp DESC")
    fun getTradesByStatus(status: String): Flow<List<TradeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeLog): Long

    @Update
    suspend fun updateTrade(trade: TradeLog)

    @Query("DELETE FROM trade_logs")
    suspend fun clearAllTrades()
}
