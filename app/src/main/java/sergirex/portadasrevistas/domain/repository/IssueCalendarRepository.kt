package sergirex.portadasrevistas.domain.repository

/** The issues a magazine has published. */
interface IssueCalendarRepository {

    /** The publication dates (`yyyyMMdd`) of every issue of [magazineId], newest first. */
    suspend fun getIssueDates(magazineId: String): Result<List<String>>
}
