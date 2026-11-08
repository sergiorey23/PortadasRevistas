package sergirex.portadasrevistas.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import sergirex.portadasrevistas.data.catalog.BundledCatalog
import sergirex.portadasrevistas.data.catalog.CatalogCache
import sergirex.portadasrevistas.data.catalog.CatalogConfig
import sergirex.portadasrevistas.data.catalog.CatalogParser
import sergirex.portadasrevistas.data.catalog.ParsedCatalog
import sergirex.portadasrevistas.data.catalog.RemoteCatalog
import sergirex.portadasrevistas.data.catalog.RemoteCatalogSource
import sergirex.portadasrevistas.domain.model.CatalogRefreshResult
import sergirex.portadasrevistas.domain.model.CatalogStatus
import sergirex.portadasrevistas.domain.model.Revista
import sergirex.portadasrevistas.domain.model.RevistaCategory
import sergirex.portadasrevistas.domain.repository.RevistasRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The magazine catalog, from three places in order of freshness: the copy downloaded from the published
 * catalog (cached on disk), and the one bundled in the app. Whichever has the higher `catalogVersion` is
 * used, so an app update that ships a newer bundled catalog beats a stale download, and a download beats
 * an old app. A downloaded file is only adopted after it validates (see [CatalogParser]); if the network,
 * the file or the disk fails, the catalog in use is simply kept.
 *
 * It is read once into memory (~140 small entries); [refresh] swaps the whole list atomically.
 */
@Singleton
class RevistasRepositoryImpl @Inject constructor(
    private val bundled: BundledCatalog,
    private val cache: CatalogCache,
    private val remote: RemoteCatalogSource
) : RevistasRepository {

    // Replaceable in tests.
    internal var clock: () -> Long = System::currentTimeMillis
    internal var onProblem: (String) -> Unit = { Log.w(TAG, it) }

    private val refreshLock = Mutex()
    private val changes = MutableStateFlow(0)
    override val catalogVersion: StateFlow<Int> = changes.asStateFlow()

    @Volatile
    private var current: ParsedCatalog? = null

    override suspend fun getAll(): List<Revista> = withContext(Dispatchers.IO) { catalog().revistas }

    override suspend fun getByCategory(category: RevistaCategory): Result<List<Revista>> =
        withContext(Dispatchers.IO) { runCatching { catalog().revistas.filter { it.category == category } } }

    override suspend fun getById(id: String): Revista? =
        withContext(Dispatchers.IO) { catalog().revistas.firstOrNull { it.id == id } }

    override suspend fun refresh(force: Boolean): Boolean = update(force) == CatalogRefreshResult.UPDATED

    override suspend fun refreshNow(): CatalogRefreshResult = update(force = true)

    override suspend fun status(): CatalogStatus = withContext(Dispatchers.IO) {
        val now = catalog()
        CatalogStatus(version = now.version, entries = now.revistas.size, lastCheckedAt = cache.lastCheckedAt())
    }

    private suspend fun update(force: Boolean): CatalogRefreshResult = refreshLock.withLock {
        withContext(Dispatchers.IO) {
            val now = clock()
            if (!force && now - cache.lastCheckedAt() < CatalogConfig.MIN_CHECK_INTERVAL_MS) {
                return@withContext CatalogRefreshResult.UP_TO_DATE
            }

            when (val result = remote.fetch(cache.readEtag())) {
                RemoteCatalog.NotModified -> {
                    markChecked(now)
                    CatalogRefreshResult.UP_TO_DATE
                }
                is RemoteCatalog.Failed -> {
                    // Not marked as checked: offline now, so try again at the next opportunity.
                    onProblem("Catalog download failed: ${result.reason}")
                    CatalogRefreshResult.FAILED
                }
                is RemoteCatalog.Updated -> {
                    markChecked(now)
                    if (adopt(result.text, result.etag)) CatalogRefreshResult.UPDATED else CatalogRefreshResult.UP_TO_DATE
                }
            }
        }
    }

    private fun adopt(text: String, etag: String?): Boolean {
        val parsed = CatalogParser.parse(text)
        if (parsed == null) {
            onProblem("Downloaded catalog rejected (invalid, too small or from a newer schema)")
            return false
        }
        val now = catalog()
        // Never go back to an older catalog, and don't churn when nothing really changed.
        if (parsed.version < now.version || parsed.revistas == now.revistas) return false

        runCatching { cache.saveCatalog(text, etag) }.onFailure { onProblem("Couldn't cache the catalog: $it") }
        current = parsed
        changes.value += 1
        return true
    }

    private fun markChecked(now: Long) {
        runCatching { cache.markChecked(now) }.onFailure { onProblem("Couldn't record the catalog check: $it") }
    }

    private fun catalog(): ParsedCatalog = current ?: synchronized(this) { current ?: load().also { current = it } }

    private fun load(): ParsedCatalog {
        val fromCache = runCatching { cache.readCatalog() }.getOrNull()?.let(CatalogParser::parse)
        val fromBundle = runCatching { bundled.read() }.getOrNull()?.let(CatalogParser::parse)
        return listOfNotNull(fromCache, fromBundle).maxByOrNull { it.version }
            ?: ParsedCatalog(version = 0, revistas = emptyList(), skipped = 0).also {
                onProblem("No readable catalog (cache and bundled copy both failed)")
            }
    }

    private companion object {
        const val TAG = "RevistasRepository"
    }
}
