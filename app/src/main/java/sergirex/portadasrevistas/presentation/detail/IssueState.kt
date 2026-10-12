package sergirex.portadasrevistas.presentation.detail

/**
 * What the detail screen knows about one magazine's issues: the dates it has published (newest
 * first) and which of them is on screen. [selected] is null until the calendar arrives.
 */
data class IssueState(
    val dates: List<String> = emptyList(),
    val selected: String? = null,
    val isLoading: Boolean = true,
    val failed: Boolean = false
)
