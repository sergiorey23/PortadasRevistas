package sergirex.portadasrevistas.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasrevistas.domain.model.HomeTab
import sergirex.portadasrevistas.domain.model.ReminderConfig
import sergirex.portadasrevistas.domain.model.ReminderFrequency

/** User-facing settings other than the theme (which has its own [ThemeRepository]). */
interface SettingsRepository {
    val initialTab: Flow<HomeTab>

    /** Whether the new-issue check (and its notification) is on. */
    val dailyNotificationEnabled: Flow<Boolean>

    /** Whether the system notification-permission prompt was already shown, so it isn't repeated every launch. */
    val notificationPermissionRequested: Flow<Boolean>

    /** When the new-issue check runs: time of day and frequency (default every day at 09:00). */
    val reminderConfig: Flow<ReminderConfig>

    /** Columns of the cover grid: 0 follows the screen size, otherwise 1 to 3. */
    val gridColumns: Flow<Int>

    /** True for a pure-black dark theme (AMOLED screens). */
    val pureBlack: Flow<Boolean>

    suspend fun setInitialTab(tab: HomeTab)
    suspend fun setReminderMinutes(minutes: Int)
    suspend fun setReminderFrequency(frequency: ReminderFrequency)
    suspend fun setReminderDays(days: Set<Int>)
    suspend fun setReminderWeekDay(day: Int)
    suspend fun setReminderMonthDay(day: Int)
    suspend fun setGridColumns(columns: Int)
    suspend fun setPureBlack(enabled: Boolean)
    suspend fun setDailyNotificationEnabled(enabled: Boolean)
    suspend fun setNotificationPermissionRequested()
}
