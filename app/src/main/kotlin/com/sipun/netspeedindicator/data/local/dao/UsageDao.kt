package com.sipun.netspeedindicator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sipun.netspeedindicator.data.local.entity.UsageEntity

/**
 * Data Access Object for usage data
 * Provides type-safe database operations
 */
@Dao
interface UsageDao {
    /**
     * Insert or update usage data
     * If date already exists, replace with new values
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: UsageEntity)

    /**
     * Get usage for a specific date
     */
    @Query("SELECT * FROM usage WHERE date = :date")
    suspend fun getByDate(date: String): UsageEntity?

    /**
     * Get usage for a date range (inclusive)
     */
    @Query("SELECT * FROM usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getByDateRange(startDate: String, endDate: String): List<UsageEntity>

    /**
     * Get all usage records for a specific month
     * @param yearMonth Format: yyyy-MM (e.g., "2024-01")
     */
    @Query("SELECT * FROM usage WHERE date LIKE :yearMonth || '%' ORDER BY date DESC")
    suspend fun getByMonth(yearMonth: String): List<UsageEntity>

}
