package sergirex.portadasrevistas.domain.model

/**
 * A magazine whose latest cover is known to exist. Holds a URL, not a Bitmap: decoding and
 * caching the image is a presentation concern (Coil), not a domain one.
 *
 * @param issueDate publication date (`yyyyMMdd`) of the issue that cover belongs to, or null when
 * the magazine's calendar couldn't be read (then nothing is known about how old the cover is)
 */
data class RevistaCover(
    val revista: RevistaRef,
    val imageUrl: String,
    val issueDate: String? = null
) {
    /** The cover is from an issue that is no longer current, by the limit of its magazine's category. */
    val isOld: Boolean get() = issueDate?.let { OldIssuePolicy.isOld(it, revista.category) } == true
}
