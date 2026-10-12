package sergirex.portadasrevistas.presentation.portadas

import sergirex.portadasrevistas.domain.model.RevistaCover
import sergirex.portadasrevistas.domain.model.RevistaRef

/**
 * Single immutable snapshot a tab renders — the point of unidirectional data flow: the UI never
 * mutates an adapter imperatively from callbacks (the old AsyncTask's publishProgress chain), it
 * just re-renders whenever this changes.
 *
 * [allRevistas] is every magazine the tab asked for; [covers] is the subset whose cover has been
 * confirmed so far, which is what the grid shows.
 */
data class PortadasUiState(
    val allRevistas: List<RevistaRef> = emptyList(),
    val covers: List<RevistaCover> = emptyList(),
    val isRefreshing: Boolean = false
) {
    /** A load finished, there were magazines to show, and none of their covers could be fetched. */
    val showLoadFailed: Boolean get() = !isRefreshing && allRevistas.isNotEmpty() && covers.isEmpty()
}
