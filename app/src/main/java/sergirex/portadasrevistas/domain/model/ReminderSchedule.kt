package sergirex.portadasrevistas.domain.model

import java.util.Calendar

/** How often the new-issue check runs. */
enum class ReminderFrequency { DAILY, WEEKLY, MONTHLY }

/**
 * When the new-issue check runs: a time of day plus a frequency. Daily checks run on the chosen days of the week,
 * weekly ones on one day of the week and monthly ones on one day of the month (the last day in shorter months).
 * Days of the week use [Calendar]'s constants ([Calendar.SUNDAY] = 1 … [Calendar.SATURDAY] = 7) so they follow the
 * device calendar.
 */
data class ReminderConfig(
    val minutesOfDay: Int = ReminderSchedule.DEFAULT_MINUTES,
    val frequency: ReminderFrequency = ReminderFrequency.DAILY,
    val days: Set<Int> = ReminderSchedule.ALL_DAYS,
    val weekDay: Int = ReminderSchedule.DEFAULT_WEEK_DAY,
    val monthDay: Int = ReminderSchedule.DEFAULT_MONTH_DAY
)

object ReminderSchedule {
    const val DEFAULT_MINUTES = 9 * 60
    const val DEFAULT_WEEK_DAY = Calendar.MONDAY
    const val DEFAULT_MONTH_DAY = 1
    const val MAX_MONTH_DAY = 31

    /** Monday to Sunday, the order the days are listed in. */
    val DAYS_IN_ORDER = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
    val ALL_DAYS: Set<Int> = DAYS_IN_ORDER.toSet()
    val WEEKDAYS: Set<Int> = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
    )

    /**
     * The first moment strictly after [now] at the configured time on a day the [config] allows, or null if no day
     * is selected. With [afterFiring] today is skipped (the check just ran, possibly a little early).
     */
    fun nextTrigger(
        now: Long,
        config: ReminderConfig,
        afterFiring: Boolean,
        calendar: Calendar = Calendar.getInstance()
    ): Long? {
        if (config.frequency == ReminderFrequency.DAILY && config.days.isEmpty()) return null
        // A monthly day can be up to a month (and a bit) away; daily and weekly ones are within a week.
        val horizon = if (config.frequency == ReminderFrequency.MONTHLY) 62 else 7
        for (offset in 0..horizon) {
            val candidate = (calendar.clone() as Calendar).apply {
                timeInMillis = now
                add(Calendar.DATE, offset)
                set(Calendar.HOUR_OF_DAY, config.minutesOfDay / 60)
                set(Calendar.MINUTE, config.minutesOfDay % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (!isAllowedOn(candidate, config)) continue
            if (candidate.timeInMillis <= now) continue
            if (afterFiring && offset == 0) continue
            return candidate.timeInMillis
        }
        return null
    }

    /** Whether the check is allowed on the day [now] falls on. */
    fun isAllowedOn(now: Long, config: ReminderConfig, calendar: Calendar = Calendar.getInstance()): Boolean =
        isAllowedOn((calendar.clone() as Calendar).apply { timeInMillis = now }, config)

    private fun isAllowedOn(day: Calendar, config: ReminderConfig): Boolean = when (config.frequency) {
        ReminderFrequency.DAILY -> day.get(Calendar.DAY_OF_WEEK) in config.days
        ReminderFrequency.WEEKLY -> day.get(Calendar.DAY_OF_WEEK) == config.weekDay
        ReminderFrequency.MONTHLY ->
            day.get(Calendar.DAY_OF_MONTH) == config.monthDay.coerceAtMost(day.getActualMaximum(Calendar.DAY_OF_MONTH))
    }
}
