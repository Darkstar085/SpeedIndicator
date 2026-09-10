package com.sipun.netspeedindicator.domain.model

data class UsageInfo(
    val date: String = "",
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
) {
    val totalBytes: Long get() = wifiRxBytes + wifiTxBytes + mobileRxBytes + mobileTxBytes
    val wifiTotalBytes: Long get() = wifiRxBytes + wifiTxBytes
    val mobileTotalBytes: Long get() = mobileRxBytes + mobileTxBytes
    val totalDownloadBytes: Long get() = wifiRxBytes + mobileRxBytes
    val totalUploadBytes: Long get() = wifiTxBytes + mobileTxBytes
}
