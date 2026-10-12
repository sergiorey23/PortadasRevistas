package sergirex.portadasrevistas.presentation.portadas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.domain.model.HomeTab
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.model.toRef
import sergirex.portadasrevistas.domain.repository.SettingsRepository
import sergirex.portadasrevistas.domain.usecase.GetFavoriteRevistasUseCase
import sergirex.portadasrevistas.domain.usecase.GetRevistaCoversUseCase
import sergirex.portadasrevistas.domain.usecase.GetRevistasByCategoryUseCase
import sergirex.portadasrevistas.domain.usecase.ObserveFavoritesUseCase
import javax.inject.Inject

/**
 * Holds the state of every home tab, scoped to the home Activity rather than to each tab's
 * Fragment. Fragments can then be destroyed and recreated freely by the pager (tabs load lazily and
 * only neighbours stay alive) without losing the covers already confirmed. A tab loads the first
 * time it is shown, and again when its magazines changed (favorites) or on pull-to-refresh.
 */
@HiltViewModel
class PortadasViewModel @Inject constructor(
    private val getRevistasByCategory: GetRevistasByCategoryUseCase,
    private val getFavoriteRevistas: GetFavoriteRevistasUseCase,
    private val getRevistaCovers: GetRevistaCoversUseCase,
    private val settings: SettingsRepository,
    observeFavorites: ObserveFavoritesUseCase
) : ViewModel() {

    private class TabLoader {
        val state = MutableStateFlow(PortadasUiState())
        var job: Job? = null
    }

    private val loaders = HashMap<String, TabLoader>()

    /** The tabs to show; the favorites tab exists only while there is at least one favorite. Empty until known. */
    val tabs: StateFlow<List<HomeTab>> = observeFavorites()
        .map { it.isNotEmpty() }
        .distinctUntilChanged()
        .map { hasFavorites -> if (hasFavorites) listOf(HomeTab.Favorites) + HomeTab.categories else HomeTab.categories }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** True once the initial-tab setting has been applied, so later tab changes don't re-select it. */
    var initialTabApplied = false

    suspend fun initialTab(): HomeTab = settings.initialTab.first()

    fun uiState(tab: HomeTab): StateFlow<PortadasUiState> = loader(tab).state.asStateFlow()

    /** No-op while a load is running or when what's on screen is already current. */
    fun loadIfNeeded(tab: HomeTab) {
        val loader = loader(tab)
        if (loader.job?.isActive == true) return
        loader.job = viewModelScope.launch {
            val revistas = revistasFor(tab)
            val state = loader.state.value
            if (revistas == state.allRevistas && state.covers.isNotEmpty()) return@launch
            fetch(loader, revistas, forceRefresh = false)
        }
    }

    fun refresh(tab: HomeTab) {
        val loader = loader(tab)
        loader.job?.cancel()
        loader.job = viewModelScope.launch { fetch(loader, revistasFor(tab), forceRefresh = true) }
    }

    private fun loader(tab: HomeTab) = loaders.getOrPut(tab.key) { TabLoader() }

    private suspend fun revistasFor(tab: HomeTab): List<RevistaRef> = when (tab) {
        HomeTab.Favorites -> getFavoriteRevistas()
        is HomeTab.Category -> getRevistasByCategory(tab.category).getOrDefault(emptyList()).map { it.toRef() }
    }

    private suspend fun fetch(loader: TabLoader, revistas: List<RevistaRef>, forceRefresh: Boolean) {
        loader.state.update {
            it.copy(
                allRevistas = revistas,
                covers = if (revistas.isEmpty()) emptyList() else it.covers,
                isRefreshing = true
            )
        }
        try {
            // Keep showing the previous covers until the first new one arrives, so a failed
            // reload doesn't blank the screen.
            var received = false
            getRevistaCovers(revistas, forceRefresh)
                .catch { error -> Log.w(TAG, "Loading covers failed", error) } // keep whatever resolved so far on screen
                .collect { cover ->
                    loader.state.update { it.copy(covers = if (received) it.covers + cover else listOf(cover)) }
                    received = true
                }
        } finally {
            loader.state.update { it.copy(isRefreshing = false) }
        }
    }

    private companion object {
        const val TAG = "PortadasViewModel"
    }
}
