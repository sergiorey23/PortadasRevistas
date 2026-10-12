package sergirex.portadasrevistas.domain.model

/** The one place that knows how cover and page image URLs are laid out on the kiosk services. */
object CoverUrls {
    const val KIOSK_HOST = "www.kioskoymas.com"
    const val PAGES_HOST = "t.prcdn.co"

    private const val COVER_WIDTH = 424
    private const val COVER_HEIGHT = 600
    private const val DETAIL_SCALE = 171

    /** The magazine's newest cover; the service decides which issue that is. */
    fun latest(id: String) = "https://$KIOSK_HOST/Imageresize/getLatestImage/$id/$COVER_WIDTH/$COVER_HEIGHT"

    /** The list of published issue dates of a magazine (`yyyyMMdd|yyyyMMdd|...`, newest first). */
    fun history(id: String) = "https://$KIOSK_HOST/Api_postcalendar/getHistory/$id"

    /** The full-size cover of the issue published on [date] (`yyyyMMdd`). */
    fun issueCover(id: String, date: String) = "https://$PAGES_HOST/img?file=$id${date}00000000001001&scale=$DETAIL_SCALE"

    /** Page [page] (1-based) of the issue published on [date]. */
    fun issuePage(id: String, date: String, page: Int, scale: String) =
        "https://$PAGES_HOST/img?file=$id${date}00000000001001&page=$page&scale=$scale"
}
