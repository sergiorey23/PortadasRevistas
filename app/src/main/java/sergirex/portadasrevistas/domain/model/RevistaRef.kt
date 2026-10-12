package sergirex.portadasrevistas.domain.model

/**
 * Everything needed to load, show and read a magazine, without its category: a favorite can
 * belong to any category, and past the home tabs nothing cares which one it was.
 */
data class RevistaRef(
    val id: String,
    val name: String,
    val web: String,
    val pages: Int,
    val scale: String,
    /** The catalog category, or null for a favorite that is no longer (or not yet known to be) in the catalog. */
    val category: RevistaCategory? = null
) {
    companion object {
        /**
         * Parses the legacy `"id:title[:web[:pages[:scale]]]"` encoding that favorites used to be
         * stored in, so existing favorites keep working. Missing or malformed numbers fall back to
         * the same defaults the old code used.
         */
        fun fromLegacyEncoded(raw: String): RevistaRef {
            val parts = raw.split(":")
            return RevistaRef(
                id = parts[0],
                name = parts.getOrElse(1) { parts[0] },
                web = parts.getOrElse(2) { "" },
                pages = parts.getOrNull(3)?.toIntOrNull() ?: Revista.DEFAULT_PAGES,
                scale = parts.getOrNull(4)?.takeIf { it.isNotBlank() } ?: Revista.DEFAULT_SCALE
            )
        }
    }
}

fun Revista.toRef(): RevistaRef = RevistaRef(id, name, web, pages, scale, category)
