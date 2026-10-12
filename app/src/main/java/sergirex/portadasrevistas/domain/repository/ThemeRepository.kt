package sergirex.portadasrevistas.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasrevistas.domain.model.ThemeMode

/**
 * Source of truth for the user's theme preference, exposed as a [Flow] so every observer (the
 * Application applying the night mode, the Settings screen) reacts when it changes.
 */
interface ThemeRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
