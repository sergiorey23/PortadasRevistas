package sergirex.portadasrevistas.domain.model

/**
 * A magazine the app can show covers for.
 *
 * Replaces the legacy `"id:title:web:pages:scale"` colon-encoded String that used to flow through
 * the whole app (Revistas.java's arrays, GetPortadas' `String.split(":")`, the Intent extras):
 * five unrelated pieces of data packed into one String, parsed again at every consumer.
 *
 * @param id the magazine's id on the kiosk service (also the key of everything stored about it)
 * @param name the title shown to the user
 * @param web the magazine's web address without scheme (e.g. "semana.es"), or empty if unknown
 * @param pages number of pages of an issue, for the reader
 * @param scale the image scale the reader requests pages at
 */
data class Revista(
    val id: String,
    val name: String,
    val web: String,
    val pages: Int,
    val scale: String,
    val category: RevistaCategory
) {
    companion object {
        const val DEFAULT_PAGES = 100
        const val DEFAULT_SCALE = "74"
    }
}
