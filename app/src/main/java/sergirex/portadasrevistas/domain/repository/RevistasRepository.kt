package sergirex.portadasrevistas.domain.repository

import kotlinx.coroutines.flow.StateFlow
import sergirex.portadasrevistas.domain.model.CatalogRefreshResult
import sergirex.portadasrevistas.domain.model.CatalogStatus
import sergirex.portadasrevistas.domain.model.Revista
import sergirex.portadasrevistas.domain.model.RevistaCategory

/**
 * Source of truth for which magazines exist. The domain layer only knows this interface: where the data
 * comes from (a bundled asset and a published copy) is a data-layer detail, which is what lets use cases
 * and ViewModels be unit-tested with a fake.
 */
interface RevistasRepository {
    suspend fun getByCategory(category: RevistaCategory): Result<List<Revista>>

    /** The catalog entry with this id, or null if it isn't (or no longer is) in the catalog. */
    suspend fun getById(id: String): Revista?

    /** Every magazine in the catalog (empty only if the catalog couldn't be read at all). */
    suspend fun getAll(): List<Revista>

    /** Increases whenever the catalog content changes (a newer copy was downloaded). */
    val catalogVersion: StateFlow<Int>

    /**
     * Checks the published catalog and adopts it if it is newer and valid. Skipped when checked recently
     * (unless [force]). Never throws: if anything goes wrong the current catalog stays. Returns true if it changed.
     */
    suspend fun refresh(force: Boolean = false): Boolean

    /** Like [refresh] with `force`, but says what happened (for the "Update now" button). */
    suspend fun refreshNow(): CatalogRefreshResult

    /** Version, size and last check of the catalog currently in use. */
    suspend fun status(): CatalogStatus
}
