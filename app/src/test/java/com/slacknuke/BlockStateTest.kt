package com.slacknuke

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class BlockStateTest {

    private val zone = ZoneId.of("Europe/London")

    @Test
    fun nextBlockBeforeWakeTimeExpiresTodayAtSix() {
        val now = ZonedDateTime.of(2026, 4, 29, 5, 59, 0, 0, zone)
        val expected = ZonedDateTime.of(2026, 4, 29, 6, 0, 0, 0, zone)

        assertEquals(expected.toInstant().toEpochMilli(), BlockState.nextBlockUntilMillis(now))
    }

    @Test
    fun nextBlockAtWakeTimeExpiresTomorrowAtSix() {
        val now = ZonedDateTime.of(2026, 4, 29, 6, 0, 0, 0, zone)
        val expected = ZonedDateTime.of(2026, 4, 30, 6, 0, 0, 0, zone)

        assertEquals(expected.toInstant().toEpochMilli(), BlockState.nextBlockUntilMillis(now))
    }

    @Test
    fun nextBlockAfterWakeTimeExpiresTomorrowAtSix() {
        val now = ZonedDateTime.of(2026, 4, 29, 23, 0, 0, 0, zone)
        val expected = ZonedDateTime.of(2026, 4, 30, 6, 0, 0, 0, zone)

        assertEquals(expected.toInstant().toEpochMilli(), BlockState.nextBlockUntilMillis(now))
    }
}
