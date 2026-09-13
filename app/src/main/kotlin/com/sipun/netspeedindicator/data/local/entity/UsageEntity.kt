package com.sipun.netspeedindicator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing daily usage data
 * Maps to 'usage' table in database
 */
@Entity(tableName = "usage")
data class UsageEntity(
    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
)
