package com.lyq2010.leesmusic.data.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyMixStoreTest {
    @Test
    fun sameDayIsCurrent() {
        assertTrue(dailyMixIsCurrent("2026-09-26", "2026-09-26"))
    }

    @Test
    fun previousDayIsStale() {
        assertFalse(dailyMixIsCurrent("2026-09-25", "2026-09-26"))
    }

    @Test
    fun midnightWaitReachesTheNextLocalDay() {
        val zone = ZoneId.of("Asia/Shanghai")
        val now = ZonedDateTime.of(2026, 9, 26, 23, 30, 0, 0, zone)
        val wait = millisUntilNextMidnight(now)
        val after = now.plusNanos(wait * 1_000_000)
        assertTrue(after.toLocalDate().isAfter(now.toLocalDate()))
    }
}
