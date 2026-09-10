package com.sipun.netspeedindicator.data.mapper

import com.sipun.netspeedindicator.data.local.entity.UsageEntity
import com.sipun.netspeedindicator.domain.model.UsageInfo

fun UsageEntity.toDomain() = UsageInfo(
    date = date,
    wifiRxBytes = wifiRxBytes,
    wifiTxBytes = wifiTxBytes,
    mobileRxBytes = mobileRxBytes,
    mobileTxBytes = mobileTxBytes
)

fun UsageInfo.toEntity() = UsageEntity(
    date = date,
    wifiRxBytes = wifiRxBytes,
    wifiTxBytes = wifiTxBytes,
    mobileRxBytes = mobileRxBytes,
    mobileTxBytes = mobileTxBytes,
    lastUpdated = System.currentTimeMillis()
)
