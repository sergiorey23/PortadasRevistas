package sergirex.portadasrevistas.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import sergirex.portadasrevistas.data.local.ScaleStore
import sergirex.portadasrevistas.data.network.await
import sergirex.portadasrevistas.domain.model.CoverUrls
import sergirex.portadasrevistas.domain.repository.PageScaleRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looks for the largest `scale` the service serves for a magazine's pages by asking for page 2 (the
 * first page that is subject to the limit; page 1, the cover, accepts far larger values) with
 * HEAD requests, so no image is downloaded.
 *
 * The limit is the same for every page and every issue of a magazine, so the answer is remembered
 * in a [ScaleStore] and later only confirmed with one request. If the confirmation fails (the
 * service tightened the limit) the search runs again. When the network can't answer, the preferred
 * scale is used and nothing is remembered.
 */
@Singleton
class PageScaleRepositoryImpl @Inject constructor(
    private val client: OkHttpClient,
    private val store: ScaleStore
) : PageScaleRepository {

    /** The server's answer for one scale: accepted, refused, or not known (network failure). */
    private enum class Answer { ACCEPTED, REFUSED, UNKNOWN }

    override suspend fun resolve(magazineId: String, date: String, preferredScale: Int): Int =
        withContext(Dispatchers.IO) {
            val start = store.get(magazineId)?.coerceIn(MIN_SCALE, preferredScale) ?: preferredScale
            when (ask(magazineId, date, start)) {
                Answer.ACCEPTED -> start.also { store.put(magazineId, it) }
                Answer.UNKNOWN -> start
                Answer.REFUSED -> search(magazineId, date, below = start) ?: start
            }
        }

    /** Binary search for the largest accepted scale below [below]; null if none can be determined. */
    private suspend fun search(magazineId: String, date: String, below: Int): Int? {
        var low = MIN_SCALE
        var high = below - 1
        // The lowest scale must be accepted, otherwise the problem isn't the scale (e.g. the issue has no page 2).
        when (ask(magazineId, date, low)) {
            Answer.ACCEPTED -> Unit
            else -> return null
        }
        while (low < high) {
            val middle = (low + high + 1) / 2
            when (ask(magazineId, date, middle)) {
                Answer.ACCEPTED -> low = middle
                Answer.REFUSED -> high = middle - 1
                Answer.UNKNOWN -> return null
            }
        }
        store.put(magazineId, low)
        return low
    }

    private suspend fun ask(magazineId: String, date: String, scale: Int): Answer = try {
        val url = CoverUrls.issuePage(magazineId, date, page = PROBE_PAGE, scale = scale.toString())
        client.newCall(Request.Builder().url(url).head().build()).await().use { response ->
            if (response.isSuccessful) Answer.ACCEPTED else Answer.REFUSED
        }
    } catch (error: IOException) {
        Answer.UNKNOWN
    }

    private companion object {
        const val PROBE_PAGE = 2
        const val MIN_SCALE = 10
    }
}
