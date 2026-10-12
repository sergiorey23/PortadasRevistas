package sergirex.portadasrevistas.domain.model

/**
 * When a cover counts as "old" and gets the date ribbon. Magazines of different kinds come out at
 * different paces: celebrity weeklies every week, nearly everything else monthly or so, so a month
 * and a half without a new issue is what is worth flagging there.
 */
object OldIssuePolicy {
    const val CELEBRITY_DAYS = 7
    const val DEFAULT_DAYS = 45

    /** Days after which an issue of a magazine of [category] is considered old (unknown categories get the default). */
    fun thresholdDays(category: RevistaCategory?): Int =
        if (category == RevistaCategory.CORAZON) CELEBRITY_DAYS else DEFAULT_DAYS

    fun isOld(date: String, category: RevistaCategory?, now: Long = System.currentTimeMillis()): Boolean =
        IssueDate.isOld(date, thresholdDays(category), now)
}
