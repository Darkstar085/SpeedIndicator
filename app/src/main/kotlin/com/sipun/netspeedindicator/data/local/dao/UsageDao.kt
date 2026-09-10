package com.sipun.netspeedindicator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sipun.netspeedindicator.data.local.entity.UsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: UsageEntity)

    @Query("SELECT * FROM usage WHERE date = :date")
    suspend fun getByDate(date: String): UsageEntity?

    @Query("SELECT * FROM usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getByDateRange(startDate: String, endDate: String): List<UsageEntity>

    @Query("SELECT * FROM usage WHERE date = :date")
    fun observeByDate(date: String): Flow<UsageEntity?>

    @Query("SELECT * FROM usage WHERE date LIKE :yearMonth || '%' ORDER BY date DESC")
    suspend fun getByMonth(yearMonth: String): List<UsageEntity>

    @Query("DELETE FROM usage WHERE date < :beforeDate")
    suspend fun deleteOldRecords(beforeDate: String)

    @Query("SELECT * FROM usage ORDER BY date DESC")
    suspend fun getAll(): List<UsageEntity>
}
