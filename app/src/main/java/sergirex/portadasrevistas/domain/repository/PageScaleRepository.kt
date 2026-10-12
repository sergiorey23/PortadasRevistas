package sergirex.portadasrevistas.domain.repository

/**
 * Finds the sharpest page resolution the image service will serve for a magazine. The service
 * refuses (403) pages from the second one on when the requested `scale` is above a limit that
 * differs per magazine, so a fixed value from the catalog can't be trusted to keep working.
 */
interface PageScaleRepository {

    /**
     * The largest scale, up to [preferredScale], the service accepts for the pages of
     * [magazineId]. Falls back to [preferredScale] when that can't be determined (e.g. offline).
     */
    suspend fun resolve(magazineId: String, date: String, preferredScale: Int): Int
}
