package sergirex.portadasrevistas.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The publication date of one magazine issue, as the kiosk service writes it: a `yyyyMMdd`
 * string. Magazines come out weekly or monthly, so unlike a newspaper there is no "today's
 * edition": the newest date a magazine's calendar lists is its current issue.
 */
object IssueDate {
    private const val PATTERN = "yyyyMMdd"

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /** The device's current day, used to tell apart saved copies of a magazine's "latest" cover from different days. */
    fun today(now: Long = System.currentTimeMillis()): String = SimpleDateFormat(PATTERN, Locale.FRANCE).format(Date(now))

    /**
     * Whether the issue published on [date] is more than [olderThanDays] days old. A date that
     * can't be read is never old (see [OldIssuePolicy] for the limit that applies to a magazine).
     * Both days are compared as UTC midnights, so daylight-saving changes can't shift the count.
     */
    fun isOld(date: String, olderThanDays: Int, now: Long = System.currentTimeMillis()): Boolean {
        val issueDay = toUtcMillis(date) ?: return false
        val today = toUtcMillis(today(now)) ?: return false
        return (today - issueDay) / DAY_MILLIS > olderThanDays
    }

    /**
     * Material date pickers report the chosen day as UTC midnight; formatting that in the device
     * time zone would show the previous day anywhere west of UTC.
     */
    fun fromUtcMillis(millis: Long): String = utcFormat().format(Date(millis))

    fun toUtcMillis(date: String): Long? = runCatching { utcFormat().parse(date)?.time }.getOrNull()

    /**
     * The short label for a ribbon: day, abbreviated month and year in [locale] (`20261007` ->
     * `07 Oct 2026` in English, `07 oct 2026` in Spanish). Anything that isn't a well-formed date is returned unchanged.
     */
    fun toRibbonLabel(date: String, locale: Locale = Locale.getDefault()): String {
        val millis = toUtcMillis(date) ?: return date
        return SimpleDateFormat("dd MMM yyyy", locale).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))
    }

    /** `20261007` -> `07/10/2026`. Anything that isn't a well-formed date is returned unchanged. */
    fun toDisplay(date: String): String =
        if (date.length == 8 && date.all(Char::isDigit)) "${date.substring(6)}/${date.substring(4, 6)}/${date.substring(0, 4)}" else date

    private fun utcFormat() = SimpleDateFormat(PATTERN, Locale.FRANCE).apply { timeZone = TimeZone.getTimeZone("UTC") }
}
