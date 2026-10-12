package sergirex.portadasrevistas.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.domain.model.CoverOpenOutcome
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.usecase.GetIssueDatesUseCase
import sergirex.portadasrevistas.domain.usecase.MarkReviewPromptedUseCase
import sergirex.portadasrevistas.domain.usecase.ObserveFavoritesUseCase
import sergirex.portadasrevistas.domain.usecase.RecordCoverOpenedUseCase
import sergirex.portadasrevistas.domain.usecase.ToggleFavoriteUseCase
import javax.inject.Inject

/**
 * State of the detail screen, shared by its Activity and the Fragment of each magazine in the
 * pager: which issue each magazine is showing, the favorites, and the expanded/collapsed action
 * buttons. Scoped to the Activity so it survives rotation and a Fragment being recreated by the pager.
 */
@HiltViewModel
class PortadaDetalleViewModel @Inject constructor(
    private val getIssueDates: GetIssueDatesUseCase,
    observeFavorites: ObserveFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val recordCoverOpened: RecordCoverOpenedUseCase,
    private val markReviewPrompted: MarkReviewPromptedUseCase
) : ViewModel() {

    private val issues = MutableStateFlow<Map<String, IssueState>>(emptyMap())

    private val _fabsExpanded = MutableStateFlow(false)
    val fabsExpanded: StateFlow<Boolean> = _fabsExpanded

    val favoriteIds: StateFlow<Set<String>> = observeFavorites()
        .map { favorites -> favorites.mapTo(HashSet()) { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun issue(magazineId: String): Flow<IssueState> =
        issues.map { it[magazineId] ?: IssueState() }.distinctUntilChanged()

    fun currentIssue(magazineId: String): IssueState = issues.value[magazineId] ?: IssueState()

    /** Fetches the issue calendar of [magazineId] once; a failed attempt is retried by the next call. */
    fun loadIssues(magazineId: String) {
        val known = issues.value[magazineId]
        if (known != null && !known.failed) return
        setIssue(magazineId, IssueState(isLoading = true))
        viewModelScope.launch {
            getIssueDates(magazineId).fold(
                onSuccess = { dates -> setIssue(magazineId, IssueState(dates, dates.first(), isLoading = false)) },
                onFailure = { setIssue(magazineId, IssueState(isLoading = false, failed = true)) }
            )
        }
    }

    fun selectIssue(magazineId: String, date: String) {
        issues.update { map ->
            val state = map[magazineId] ?: return@update map
            if (date !in state.dates) map else map + (magazineId to state.copy(selected = date))
        }
    }

    fun toggleFabs() {
        _fabsExpanded.update { !it }
    }

    fun toggleFavorite(revista: RevistaRef) {
        viewModelScope.launch { toggleFavoriteUseCase(revista) }
    }

    /** Counts the visit; says whether it's a good moment to ask for a review or show the full-screen ad. */
    suspend fun onCoverOpened(): CoverOpenOutcome = recordCoverOpened()

    fun onReviewPromptShown() {
        viewModelScope.launch { markReviewPrompted() }
    }

    private fun setIssue(magazineId: String, state: IssueState) {
        issues.update { it + (magazineId to state) }
    }
}
