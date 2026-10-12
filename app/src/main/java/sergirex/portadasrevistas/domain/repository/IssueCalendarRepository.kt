package sergirex.portadasrevistas.domain.repository

/** The issues a magazine has published. */
interface IssueCalendarRepository {

    /**
     * The publication dates (`yyyyMMdd`) of every issue of [magazineId], newest first. Answers may
     * come from a cache; [forceRefresh] asks the service again (for checks that look for news).
     */
    suspend fun getIssueDates(magazineId: String, forceRefresh: Boolean = false): Result<List<String>>
}
