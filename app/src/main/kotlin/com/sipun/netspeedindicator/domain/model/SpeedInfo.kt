package com.sipun.netspeedindicator.domain.model

data class SpeedInfo(
    val downloadBytesPerSecond: Long = 0L,
    val uploadBytesPerSecond: Long = 0L,
    val totalBytesPerSecond: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
