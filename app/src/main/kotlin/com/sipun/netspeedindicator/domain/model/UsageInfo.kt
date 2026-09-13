package com.sipun.netspeedindicator.domain.model

data class UsageInfo(
    val date: String = "", // Format: yyyy-MM-dd
    val wifiRxBytes: Long = 0L, // Wi-Fi received bytes
    val wifiTxBytes: Long = 0L, // Wi-Fi transmitted bytes
    val mobileRxBytes: Long = 0L, // Mobile data received bytes
    val mobileTxBytes: Long = 0L, // Mobile data transmitted bytes
) {
    val totalBytes: Long
        get() = wifiRxBytes + wifiTxBytes + mobileRxBytes + mobileTxBytes

    val wifiTotalBytes: Long
        get() = wifiRxBytes + wifiTxBytes

    val mobileTotalBytes: Long
        get() = mobileRxBytes + mobileTxBytes

    val totalDownloadBytes: Long
        get() = wifiRxBytes + mobileRxBytes

    val totalUploadBytes: Long
        get() = wifiTxBytes + mobileTxBytes
}
