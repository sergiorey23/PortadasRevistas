package sergirex.portadasrevistas.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import sergirex.portadasrevistas.data.network.await
import sergirex.portadasrevistas.domain.model.CoverUrls
import sergirex.portadasrevistas.domain.model.RevistaCover
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.IssueCalendarRepository
import sergirex.portadasrevistas.domain.repository.RevistaCoverRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Probes (HEAD, no download) which magazines have a latest cover; the image bytes themselves are
 * Coil's job. The kiosk service answers an unknown magazine with a redirect to an SVG placeholder
 * instead of a 404, so "exists" means "a successful response that is a raster image".
 *
 * Each confirmed cover is dated with its magazine's newest issue (from the issue calendar, which
 * is cached) so the UI can flag the ones that are out of date.
 *
 * Answers are memoized for [RECHECK_MS]: a magazine's cover availability changes rarely, and a
 * pull-to-refresh (`forceRefresh`) always asks again.
 */
@Singleton
class RevistaCoverRepositoryImpl @Inject constructor(
    private val client: OkHttpClient,
    private val calendar: IssueCalendarRepository
) : RevistaCoverRepository {

    private data class Probe(val exists: Boolean, val checkedAt: Long)

    private val probes = ConcurrentHashMap<String, Probe>()

    override fun getCovers(revistas: List<RevistaRef>, forceRefresh: Boolean): Flow<RevistaCover> = flow {
        coroutineScope {
            // Probe several magazines at once, but emit in list order as each one is confirmed.
            val gate = Semaphore(MAX_CONCURRENT_PROBES)
            val pending = revistas.map { r -> r to async { gate.withPermit { findCover(r, forceRefresh) } } }
            for ((_, cover) in pending) {
                cover.await()?.let { emit(it) }
            }
        }
    }.flowOn(Dispatchers.IO)

    /** The cover of [revista] with the date of its issue, or null when it has no cover. */
    private suspend fun findCover(revista: RevistaRef, forceRefresh: Boolean): RevistaCover? {
        if (!coverExists(revista.id, forceRefresh)) return null
        // The calendar only dates the cover: if it can't be read, the cover is still shown (just never flagged as old).
        val issueDate = calendar.getIssueDates(revista.id).getOrNull()?.firstOrNull()
        return RevistaCover(revista, CoverUrls.latest(revista.id), issueDate)
    }

    private suspend fun coverExists(id: String, forceRefresh: Boolean): Boolean {
        val now = System.currentTimeMillis()
        if (!forceRefresh) probes[id]?.takeIf { now - it.checkedAt < RECHECK_MS }?.let { return it.exists }

        val exists = try {
            client.newCall(Request.Builder().url(CoverUrls.latest(id)).head().build()).await().use { response ->
                val type = response.header("Content-Type").orEmpty()
                response.isSuccessful && type.startsWith("image/") && !type.contains("svg")
            }
        } catch (error: IOException) {
            // A network failure says nothing about the cover: don't remember it.
            return false
        }
        probes[id] = Probe(exists, now)
        return exists
    }

    private companion object {
        const val MAX_CONCURRENT_PROBES = 8
        const val RECHECK_MS = 30 * 60 * 1000L
    }
}
