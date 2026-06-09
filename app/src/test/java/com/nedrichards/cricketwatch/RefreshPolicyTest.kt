package com.nedrichards.cricketwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {
    @Test
    fun foregroundRefreshCadenceAvoidsMinuteByMinutePolling() {
        assertEquals(5 * 60 * 1000L, LIVE_REFRESH_INTERVAL_MS)
        assertTrue(LIVE_REFRESH_INTERVAL_MS > 60_000L)
    }
}
