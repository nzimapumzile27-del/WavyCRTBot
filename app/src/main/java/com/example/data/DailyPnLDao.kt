package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.DailyPnL
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPnLDao {
    @Query("SELECT * FROM daily_pnl ORDER BY timestamp ASC")
    fun getAllDailyPnL(): Flow<List<DailyPnL>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPnL(pnl: DailyPnL)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pnlList: List<DailyPnL>)

    @Query("DELETE FROM daily_pnl")
    suspend fun clearDailyPnL()
}
