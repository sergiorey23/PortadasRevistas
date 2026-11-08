package sergirex.portadasrevistas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasrevistas.domain.model.ReminderConfig
import sergirex.portadasrevistas.domain.model.ReminderFrequency
import sergirex.portadasrevistas.domain.model.ReminderSchedule
import java.util.Calendar
import java.util.TimeZone

class ReminderScheduleTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun calendar() = Calendar.getInstance(utc)

    /** Epoch millis of the given UTC date and time (month is 1-based). */
    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        calendar().apply { clear(); set(year, month - 1, day, hour, minute) }.timeInMillis

    private fun daily(minutes: Int = 9 * 60, days: Set<Int> = ReminderSchedule.ALL_DAYS) =
        ReminderConfig(minutesOfDay = minutes, frequency = ReminderFrequency.DAILY, days = days)

    private fun trigger(now: Long, config: ReminderConfig, afterFiring: Boolean = false) =
        ReminderSchedule.nextTrigger(now, config, afterFiring, calendar())

    // 2026-10-19 is a Monday.
    private val mondayMorning = at(2026, 10, 19, 7)

    @Test
    fun `fires later today when the time hasn't passed`() {
        assertEquals(at(2026, 10, 19, 9), trigger(mondayMorning, daily()))
    }

    @Test
    fun `fires tomorrow when today's time has passed`() {
        assertEquals(at(2026, 10, 20, 9), trigger(at(2026, 10, 19, 9, 30), daily()))
    }

    @Test
    fun `a custom time is respected`() {
        assertEquals(at(2026, 10, 19, 18, 45), trigger(mondayMorning, daily(minutes = 18 * 60 + 45)))
    }

    @Test
    fun `after a run it moves to the next day even if the time hasn't passed`() {
        // Background work can run a little early: the next one must still be a later day.
        assertEquals(at(2026, 10, 20, 9), trigger(at(2026, 10, 19, 8, 59), daily(), afterFiring = true))
    }

    @Test
    fun `weekdays only skips the weekend`() {
        val fridayAfterTime = at(2026, 10, 23, 10)
        assertEquals(at(2026, 10, 26, 9), trigger(fridayAfterTime, daily(days = ReminderSchedule.WEEKDAYS)))
    }

    @Test
    fun `no days means no check`() {
        assertNull(trigger(mondayMorning, daily(days = emptySet())))
    }

    @Test
    fun `weekly waits for its day of the week`() {
        val config = ReminderConfig(frequency = ReminderFrequency.WEEKLY, weekDay = Calendar.SATURDAY)
        assertEquals(at(2026, 10, 24, 9), trigger(mondayMorning, config))
        // Right after Saturday's run, the next one is a week later.
        assertEquals(at(2026, 10, 31, 9), trigger(at(2026, 10, 24, 9, 1), config, afterFiring = true))
    }

    @Test
    fun `weekly ignores the daily day list`() {
        val config = ReminderConfig(frequency = ReminderFrequency.WEEKLY, days = emptySet(), weekDay = Calendar.MONDAY)
        assertEquals(at(2026, 10, 19, 9), trigger(mondayMorning, config))
    }

    @Test
    fun `monthly waits for its day of the month`() {
        val config = ReminderConfig(frequency = ReminderFrequency.MONTHLY, monthDay = 5)
        assertEquals(at(2026, 11, 5, 9), trigger(mondayMorning, config))
    }

    @Test
    fun `a day the month doesn't have falls on its last day`() {
        val config = ReminderConfig(frequency = ReminderFrequency.MONTHLY, monthDay = 31)
        // November has 30 days.
        assertEquals(at(2026, 11, 30, 9), trigger(at(2026, 11, 2, 9), config))
        // February 2027 has 28.
        assertEquals(at(2027, 2, 28, 9), trigger(at(2027, 2, 1, 9), config))
    }

    @Test
    fun `isAllowedOn follows the frequency`() {
        assertTrue(ReminderSchedule.isAllowedOn(mondayMorning, daily(days = ReminderSchedule.WEEKDAYS), calendar()))
        assertFalse(ReminderSchedule.isAllowedOn(at(2026, 10, 24, 9), daily(days = ReminderSchedule.WEEKDAYS), calendar()))
        val weekly = ReminderConfig(frequency = ReminderFrequency.WEEKLY, weekDay = Calendar.MONDAY)
        assertTrue(ReminderSchedule.isAllowedOn(mondayMorning, weekly, calendar()))
        val monthly = ReminderConfig(frequency = ReminderFrequency.MONTHLY, monthDay = 19)
        assertTrue(ReminderSchedule.isAllowedOn(mondayMorning, monthly, calendar()))
        assertFalse(ReminderSchedule.isAllowedOn(at(2026, 10, 20, 9), monthly, calendar()))
    }
}
