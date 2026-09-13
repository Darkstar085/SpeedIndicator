package com.sipun.netspeedindicator.data.mapper

import com.sipun.netspeedindicator.data.local.entity.UsageEntity
import com.sipun.netspeedindicator.domain.model.UsageInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMapperTest {

    @Test
    fun `toDomain maps entity to domain model correctly`() {
        val entity = UsageEntity(
            date = "2024-01-15",
            wifiRxBytes = 1000L,
            wifiTxBytes = 500L,
            mobileRxBytes = 2000L,
            mobileTxBytes = 750L,
            lastUpdated = 1234567890L
        )

        val domain = entity.toDomain()

        assertEquals("2024-01-15", domain.date)
        assertEquals(1000L, domain.wifiRxBytes)
        assertEquals(500L, domain.wifiTxBytes)
        assertEquals(2000L, domain.mobileRxBytes)
        assertEquals(750L, domain.mobileTxBytes)
    }

    @Test
    fun `toEntity maps domain model to entity correctly`() {
        val domain = UsageInfo(
            date = "2024-01-15",
            wifiRxBytes = 1000L,
            wifiTxBytes = 500L,
            mobileRxBytes = 2000L,
            mobileTxBytes = 750L
        )

        val entity = domain.toEntity()

        assertEquals("2024-01-15", entity.date)
        assertEquals(1000L, entity.wifiRxBytes)
        assertEquals(500L, entity.wifiTxBytes)
        assertEquals(2000L, entity.mobileRxBytes)
        assertEquals(750L, entity.mobileTxBytes)
        assertTrue(entity.lastUpdated > 0L)
    }

    @Test
    fun `toDomain preserves zero values`() {
        val entity = UsageEntity(
            date = "2024-01-15",
            wifiRxBytes = 0L,
            wifiTxBytes = 0L,
            mobileRxBytes = 0L,
            mobileTxBytes = 0L
        )

        val domain = entity.toDomain()

        assertEquals(0L, domain.wifiRxBytes)
        assertEquals(0L, domain.wifiTxBytes)
        assertEquals(0L, domain.mobileRxBytes)
        assertEquals(0L, domain.mobileTxBytes)
    }

    @Test
    fun `toEntity sets lastUpdated to current time`() {
        val beforeMillis = System.currentTimeMillis()
        val domain = UsageInfo(date = "2024-01-15")
        val entity = domain.toEntity()
        val afterMillis = System.currentTimeMillis()

        assertTrue(entity.lastUpdated in beforeMillis..afterMillis)
    }

    @Test
    fun `round trip conversion preserves data`() {
        val original = UsageInfo(
            date = "2024-01-15",
            wifiRxBytes = 12345L,
            wifiTxBytes = 67890L,
            mobileRxBytes = 11111L,
            mobileTxBytes = 22222L
        )

        val entity = original.toEntity()
        val restored = entity.toDomain()

        assertEquals(original.date, restored.date)
        assertEquals(original.wifiRxBytes, restored.wifiRxBytes)
        assertEquals(original.wifiTxBytes, restored.wifiTxBytes)
        assertEquals(original.mobileRxBytes, restored.mobileRxBytes)
        assertEquals(original.mobileTxBytes, restored.mobileTxBytes)
    }
}

private fun assertTrue(condition: Boolean) {
    org.junit.Assert.assertTrue(condition)
}
