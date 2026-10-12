package sergirex.portadasrevistas.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasrevistas.domain.model.RevistaCover
import sergirex.portadasrevistas.domain.model.RevistaRef

/** Finds which magazines currently have a published cover. */
interface RevistaCoverRepository {

    /**
     * Emits a [RevistaCover] per magazine whose latest cover exists, in list order, as each is
     * confirmed (a [Flow], not a List, so covers pop into the grid as found). Magazines the
     * service has no cover for are skipped.
     */
    fun getCovers(revistas: List<RevistaRef>, forceRefresh: Boolean): Flow<RevistaCover>
}
