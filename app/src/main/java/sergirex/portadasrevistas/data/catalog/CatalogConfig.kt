package sergirex.portadasrevistas.data.catalog

/**
 * Where the live catalog is published (GitHub Pages, served from this repository's docs/ folder).
 * Changing a magazine = editing docs/catalog/revistas.json (and the identical copy in the app's
 * assets, which a test keeps in sync) and bumping "catalogVersion"; no app release needed.
 */
object CatalogConfig {
    const val URL = "https://sergiorey23.github.io/PortadasRevistas/catalog/revistas.json"

    /** Don't ask the server more often than this, however often the app is opened. */
    const val MIN_CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000
}
