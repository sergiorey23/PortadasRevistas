package sergirex.portadasrevistas.domain.model

/** The one place that knows how cover and page image URLs are laid out on the kiosk services. */
object CoverUrls {
    const val KIOSK_HOST = "www.kioskoymas.com"
    const val PAGES_HOST = "t.prcdn.co"

    /** Width in pixels (at most) of the grid's cover images: [issueThumbnail] is 474 px wide, [latest] 424. */
    const val THUMBNAIL_WIDTH = 474
    private const val LATEST_WIDTH = 424
    private const val LATEST_HEIGHT = 600
    private const val THUMBNAIL_SCALE = 74
    private const val DETAIL_SCALE = 171

    /**
     * The magazine's newest cover; the service decides which issue that is. The URL never changes from one issue to the
     * next, so an image cache keeps showing the old cover: only for checking that a cover exists, or when the issue date
     * is unknown. Use [issueThumbnail] to show it.
     */
    fun latest(id: String) = "https://$KIOSK_HOST/Imageresize/getLatestImage/$id/$LATEST_WIDTH/$LATEST_HEIGHT"

    /** The grid-sized cover of the issue published on [date] (`yyyyMMdd`): a new issue is a new URL. */
    fun issueThumbnail(id: String, date: String) = "https://$PAGES_HOST/img?file=$id${date}00000000001001&scale=$THUMBNAIL_SCALE"

    /** The list of published issue dates of a magazine (`yyyyMMdd|yyyyMMdd|...`, newest first). */
    fun history(id: String) = "https://$KIOSK_HOST/Api_postcalendar/getHistory/$id"

    /** The full-size cover of the issue published on [date] (`yyyyMMdd`). */
    fun issueCover(id: String, date: String) = "https://$PAGES_HOST/img?file=$id${date}00000000001001&scale=$DETAIL_SCALE"

    /** Page [page] (1-based) of the issue published on [date]. */
    fun issuePage(id: String, date: String, page: Int, scale: String) =
        "https://$PAGES_HOST/img?file=$id${date}00000000001001&page=$page&scale=$scale"
}
