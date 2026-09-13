package com.sipun.netspeedindicator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sipun.netspeedindicator.data.local.dao.UsageDao
import com.sipun.netspeedindicator.data.local.entity.UsageEntity

/**
 * Room database configuration
 * Single source of truth for local data persistence
 */
@Database(
    entities = [UsageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    
    companion object {
        const val DATABASE_NAME = "net_speed_indicator.db"
    }
}
