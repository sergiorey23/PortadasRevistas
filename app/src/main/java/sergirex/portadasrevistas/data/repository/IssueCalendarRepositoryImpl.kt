package sergirex.portadasrevistas.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import sergirex.portadasrevistas.data.network.await
import sergirex.portadasrevistas.domain.model.CoverUrls
import sergirex.portadasrevistas.domain.repository.IssueCalendarRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads a magazine's issue calendar from the kiosk service: a single line of `yyyyMMdd` dates
 * separated by `|`, newest first. Successful answers are kept for the process lifetime (a
 * magazine publishes at most weekly, so a stale list is at worst a few hours behind).
 */
@Singleton
class IssueCalendarRepositoryImpl @Inject constructor(
    private val client: OkHttpClient
) : IssueCalendarRepository {

    private val cache = ConcurrentHashMap<String, List<String>>()

    override suspend fun getIssueDates(magazineId: String, forceRefresh: Boolean): Result<List<String>> {
        if (!forceRefresh) cache[magazineId]?.let { return Result.success(it) }
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(Request.Builder().url(CoverUrls.history(magazineId)).build()).await().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val dates = parse(response.body?.string().orEmpty())
                    if (dates.isEmpty()) throw IOException("No issues listed")
                    cache[magazineId] = dates
                    Result.success(dates)
                }
            } catch (error: IOException) {
                Result.failure(error)
            }
        }
    }

    internal companion object {
        /** The dates in [body]'s first line; anything that isn't a `yyyyMMdd` date is dropped. */
        fun parse(body: String): List<String> =
            body.lineSequence().firstOrNull().orEmpty().split("|").filter { it.length == 8 && it.all(Char::isDigit) }
    }
}
