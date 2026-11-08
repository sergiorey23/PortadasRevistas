package sergirex.portadasrevistas.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.data.local.ImageCacheControl
import sergirex.portadasrevistas.domain.model.CatalogRefreshResult
import sergirex.portadasrevistas.domain.model.CatalogStatus
import sergirex.portadasrevistas.domain.model.HomeTab
import sergirex.portadasrevistas.domain.model.ReminderConfig
import sergirex.portadasrevistas.domain.model.ReminderFrequency
import sergirex.portadasrevistas.domain.repository.RevistasRepository
import sergirex.portadasrevistas.domain.repository.SettingsRepository
import sergirex.portadasrevistas.domain.usecase.ClearFavoritesUseCase
import sergirex.portadasrevistas.domain.usecase.ObserveFavoritesUseCase
import sergirex.portadasrevistas.notifications.NotificationScheduler
import sergirex.portadasrevistas.presentation.LoadTracker
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: NotificationScheduler,
    private val catalog: RevistasRepository,
    private val imageCache: ImageCacheControl,
    observeFavorites: ObserveFavoritesUseCase,
    private val clearAllFavorites: ClearFavoritesUseCase
) : ViewModel() {

    // One tracker for everything the Settings screen shows, so it can wait until the values are the stored ones.
    private val tracker = LoadTracker(expected = 6)

    val initialTab: StateFlow<HomeTab> = tracker.track(viewModelScope, settings.initialTab, HomeTab.default)

    val dailyNotificationEnabled: StateFlow<Boolean> =
        tracker.track(viewModelScope, settings.dailyNotificationEnabled, true)

    val reminderConfig: StateFlow<ReminderConfig> = tracker.track(viewModelScope, settings.reminderConfig, ReminderConfig())

    val gridColumns: StateFlow<Int> = tracker.track(viewModelScope, settings.gridColumns, 0)

    val pureBlack: StateFlow<Boolean> = tracker.track(viewModelScope, settings.pureBlack, false)

    /** How many favorites the user has (the ones shown, i.e. whose magazine is still in the catalog). */
    val favoritesCount: StateFlow<Int> =
        tracker.track(viewModelScope, observeFavorites().map { it.size }, 0)

    /** Catalog and cache details, read by [prepare] so the screen can show them from its first frame. */
    var preparedCatalogStatus: CatalogStatus? = null
        private set
    var preparedImageCacheSize: Long? = null
        private set

    /** Waits until every value the Settings screen shows has been read, including the slower catalog and cache details. */
    suspend fun prepare() {
        tracker.await()
        preparedCatalogStatus = catalog.status()
        preparedImageCacheSize = imageCache.sizeBytes()
    }

    /** Deletes every favorite, then calls [onDone] on the main thread. */
    fun clearFavorites(onDone: () -> Unit) {
        viewModelScope.launch {
            clearAllFavorites()
            onDone()
        }
    }

    fun setInitialTab(tab: HomeTab) {
        viewModelScope.launch { settings.setInitialTab(tab) }
    }

    fun setDailyNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDailyNotificationEnabled(enabled)
            scheduler.sync(replace = true)
        }
    }

    fun setReminderMinutes(minutes: Int) = updateSchedule { settings.setReminderMinutes(minutes) }

    fun setReminderFrequency(frequency: ReminderFrequency) = updateSchedule { settings.setReminderFrequency(frequency) }

    fun setReminderDays(days: Set<Int>) = updateSchedule { settings.setReminderDays(days) }

    fun setReminderWeekDay(day: Int) = updateSchedule { settings.setReminderWeekDay(day) }

    fun setReminderMonthDay(day: Int) = updateSchedule { settings.setReminderMonthDay(day) }

    /** Saves a schedule change and moves the pending check to the new next moment. */
    private fun updateSchedule(change: suspend () -> Unit) {
        viewModelScope.launch {
            change()
            scheduler.sync(replace = true)
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { settings.setGridColumns(columns) }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch { settings.setPureBlack(enabled) }
    }

    /** The system permission prompt is shown once automatically, and only if the alerts are on. */
    suspend fun shouldAskNotificationPermission(): Boolean =
        settings.dailyNotificationEnabled.first() && !settings.notificationPermissionRequested.first()

    fun onNotificationPermissionRequested() {
        viewModelScope.launch { settings.setNotificationPermissionRequested() }
    }

    /** Call after the permission prompt resolves: checks are only scheduled while notifications can be shown. */
    fun onNotificationPermissionResult() {
        viewModelScope.launch { scheduler.sync() }
    }

    suspend fun catalogStatus(): CatalogStatus = catalog.status()

    suspend fun refreshCatalog(): CatalogRefreshResult = catalog.refreshNow()

    suspend fun imageCacheSize(): Long = imageCache.sizeBytes()

    suspend fun clearImageCache() = imageCache.clear()
}
