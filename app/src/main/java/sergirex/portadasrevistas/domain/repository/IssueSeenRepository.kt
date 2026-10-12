package sergirex.portadasrevistas.domain.repository

/** Remembers, per magazine, the newest issue the user has already been told about. */
interface IssueSeenRepository {
    /** The publication date (`yyyyMMdd`) of the newest issue known for [magazineId], or null if none was ever recorded. */
    suspend fun lastSeen(magazineId: String): String?

    suspend fun markSeen(magazineId: String, date: String)
}
