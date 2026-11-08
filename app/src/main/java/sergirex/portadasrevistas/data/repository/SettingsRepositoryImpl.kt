package sergirex.portadasrevistas.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sergirex.portadasrevistas.data.local.editSafely
import sergirex.portadasrevistas.data.local.safeData
import sergirex.portadasrevistas.domain.model.HomeTab
import sergirex.portadasrevistas.domain.model.ReminderConfig
import sergirex.portadasrevistas.domain.model.ReminderFrequency
import sergirex.portadasrevistas.domain.model.ReminderSchedule
import sergirex.portadasrevistas.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val INITIAL_TAB_KEY_NAME = "init_category"

// "init_category" in the old default SharedPreferences file is imported under the same key; the
// stored value may be a legacy tab position, which HomeTab.fromKey understands.
private val Context.settingsDataStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "${context.packageName}_preferences",
                keysToMigrate = setOf(INITIAL_TAB_KEY_NAME)
            )
        )
    }
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val dataStore: DataStore<Preferences> = context.settingsDataStore

    private val data: Flow<Preferences> = dataStore.safeData

    override val initialTab: Flow<HomeTab> = data.map { HomeTab.fromKey(it[INITIAL_TAB]) }
    override val dailyNotificationEnabled: Flow<Boolean> = data.map { it[DAILY_NOTIFICATION] ?: true }
    override val notificationPermissionRequested: Flow<Boolean> = data.map { it[PERMISSION_REQUESTED] ?: false }

    override val reminderConfig: Flow<ReminderConfig> = data.map { prefs ->
        ReminderConfig(
            minutesOfDay = (prefs[NOTIFICATION_MINUTES] ?: ReminderSchedule.DEFAULT_MINUTES).coerceIn(0, MINUTES_IN_DAY - 1),
            frequency = prefs[NOTIFICATION_FREQUENCY]?.let { name -> ReminderFrequency.entries.firstOrNull { it.name == name } }
                ?: ReminderFrequency.DAILY,
            days = prefs[NOTIFICATION_DAYS]?.mapNotNull { it.toIntOrNull() }?.filter { it in ReminderSchedule.ALL_DAYS }?.toSet()
                ?: ReminderSchedule.ALL_DAYS,
            weekDay = prefs[NOTIFICATION_WEEK_DAY]?.takeIf { it in ReminderSchedule.ALL_DAYS } ?: ReminderSchedule.DEFAULT_WEEK_DAY,
            monthDay = (prefs[NOTIFICATION_MONTH_DAY] ?: ReminderSchedule.DEFAULT_MONTH_DAY).coerceIn(1, ReminderSchedule.MAX_MONTH_DAY)
        )
    }
    override val gridColumns: Flow<Int> = data.map { (it[GRID_COLUMNS] ?: 0).coerceIn(0, 3) }
    override val pureBlack: Flow<Boolean> = data.map { it[PURE_BLACK] ?: false }

    override suspend fun setReminderMinutes(minutes: Int) {
        dataStore.editSafely { it[NOTIFICATION_MINUTES] = minutes.coerceIn(0, MINUTES_IN_DAY - 1) }
    }

    override suspend fun setReminderFrequency(frequency: ReminderFrequency) {
        dataStore.editSafely { it[NOTIFICATION_FREQUENCY] = frequency.name }
    }

    override suspend fun setReminderDays(days: Set<Int>) {
        dataStore.editSafely { it[NOTIFICATION_DAYS] = days.map(Int::toString).toSet() }
    }

    override suspend fun setReminderWeekDay(day: Int) {
        dataStore.editSafely { it[NOTIFICATION_WEEK_DAY] = day }
    }

    override suspend fun setReminderMonthDay(day: Int) {
        dataStore.editSafely { it[NOTIFICATION_MONTH_DAY] = day.coerceIn(1, ReminderSchedule.MAX_MONTH_DAY) }
    }

    override suspend fun setGridColumns(columns: Int) {
        dataStore.editSafely { it[GRID_COLUMNS] = columns.coerceIn(0, 3) }
    }

    override suspend fun setPureBlack(enabled: Boolean) {
        dataStore.editSafely { it[PURE_BLACK] = enabled }
    }

    override suspend fun setInitialTab(tab: HomeTab) {
        dataStore.editSafely { it[INITIAL_TAB] = tab.key }
    }

    override suspend fun setDailyNotificationEnabled(enabled: Boolean) {
        dataStore.editSafely { it[DAILY_NOTIFICATION] = enabled }
    }

    override suspend fun setNotificationPermissionRequested() {
        dataStore.editSafely { it[PERMISSION_REQUESTED] = true }
    }

    private companion object {
        val INITIAL_TAB = stringPreferencesKey(INITIAL_TAB_KEY_NAME)
        val DAILY_NOTIFICATION = booleanPreferencesKey("daily_notification")
        val PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
        val NOTIFICATION_MINUTES = intPreferencesKey("daily_notification_minutes")
        val NOTIFICATION_FREQUENCY = stringPreferencesKey("notification_frequency")
        val NOTIFICATION_DAYS = stringSetPreferencesKey("daily_notification_days")
        val NOTIFICATION_WEEK_DAY = intPreferencesKey("notification_week_day")
        val NOTIFICATION_MONTH_DAY = intPreferencesKey("notification_month_day")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        const val MINUTES_IN_DAY = 24 * 60
    }
}
