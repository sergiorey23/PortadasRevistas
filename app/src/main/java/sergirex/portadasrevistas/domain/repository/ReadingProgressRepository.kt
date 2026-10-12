package sergirex.portadasrevistas.domain.repository

/** Remembers which page of an issue the reader was on, so reopening it continues there. */
interface ReadingProgressRepository {
    /** The 0-based page last read of the [date] issue of [magazineId], or 0. */
    suspend fun getPage(magazineId: String, date: String): Int

    suspend fun savePage(magazineId: String, date: String, page: Int)
}
