package com.alaotach.eli5.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @JvmSuppressWildcards
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @JvmSuppressWildcards
    @Delete
    suspend fun deleteHistory(history: HistoryEntity): Int

    @JvmSuppressWildcards
    @Query("DELETE FROM history")
    suspend fun deleteAllHistory(): Int

    @Query("SELECT COUNT(*) FROM history WHERE timestamp >= :startOfDay")
    fun getCountForToday(startOfDay: Long): Flow<Int>
}
