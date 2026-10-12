package sergirex.portadasrevistas.data.repository

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import sergirex.portadasrevistas.data.local.editSafely
import sergirex.portadasrevistas.data.local.safeData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sergirex.portadasrevistas.domain.model.ThemeMode
import sergirex.portadasrevistas.domain.repository.ThemeRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/**
 * Persists the theme preference with Jetpack DataStore instead of raw
 * SharedPreferences.
 *
 * The old code (each Activity's onCreate) read a "theme" SharedPreferences key with
 * three magic string values ("default"/"light"/"dark") and reacted to changes
 * and re-applied it by hand with setTheme() per screen — a manual,
 * easy-to-get-wrong step that each new screen had to remember.
 * DataStore's `.data` is a Flow by construction: every observer (the
 * Application process applying AppCompatDelegate's night mode, the Settings
 * screen) just collects it and gets updates for free, no listener bookkeeping.
 */
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeRepository {

    override val themeMode: Flow<ThemeMode> = context.themeDataStore.safeData
        .map { prefs -> prefs[THEME_KEY]?.toThemeModeOrNull() ?: migrateFromLegacyPreferenceOrDefault() }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.editSafely { it[THEME_KEY] = mode.name }
    }

    /**
     * One-time migration for people upgrading from the old SharedPreferences-based
     * theme setting, so their existing choice survives this refactor instead of
     * silently resetting to System. Persisting the migrated value here means this
     * only runs once per install: the next read finds THEME_KEY already set.
     */
    private suspend fun migrateFromLegacyPreferenceOrDefault(): ThemeMode {
        val legacyValue = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(LEGACY_PREFERENCE_KEY, null)
        val migrated = when (legacyValue) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        setThemeMode(migrated)
        return migrated
    }

    private fun String.toThemeModeOrNull(): ThemeMode? = runCatching { ThemeMode.valueOf(this) }.getOrNull()

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        const val LEGACY_PREFERENCE_KEY = "theme"
    }
}
