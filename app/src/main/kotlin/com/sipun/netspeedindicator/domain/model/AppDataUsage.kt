package com.sipun.netspeedindicator.domain.model

data class AppDataUsage(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val totalBytes: Long,
    val wifiBytes: Long,
    val mobileBytes: Long,
    val downloadBytes: Long,
    val uploadBytes: Long
)