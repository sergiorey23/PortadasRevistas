package sergirex.portadasrevistas.domain.model

/** An issue of a favorite magazine that came out since the user last heard about that magazine. */
data class NewIssue(
    val revista: RevistaRef,
    /** Publication date of the issue (`yyyyMMdd`). */
    val date: String
)
