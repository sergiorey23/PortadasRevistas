package sergirex.portadasrevistas.domain.repository

import sergirex.portadasrevistas.domain.model.Revista
import sergirex.portadasrevistas.domain.model.RevistaCategory

/**
 * Source of truth for which magazines exist. The domain layer only knows this interface: that the
 * data lives in a bundled JSON asset is a data-layer detail, which is what lets use cases and
 * ViewModels be unit-tested with a fake.
 */
interface RevistasRepository {
    suspend fun getByCategory(category: RevistaCategory): Result<List<Revista>>

    /** The catalog entry with this id, or null if it isn't (or no longer is) in the catalog. */
    suspend fun getById(id: String): Revista?
}
