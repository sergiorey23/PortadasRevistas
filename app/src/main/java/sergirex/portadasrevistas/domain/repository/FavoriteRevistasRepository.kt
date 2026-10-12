package sergirex.portadasrevistas.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasrevistas.domain.model.RevistaRef

/**
 * The user's favorite magazines, regardless of category, in the order they were added. Kept apart
 * from [RevistasRepository] (the fixed catalog) since favorites are user-authored state. Exposed as
 * a [Flow] so every screen reacts to a change on its own.
 */
interface FavoriteRevistasRepository {
    val favorites: Flow<List<RevistaRef>>

    /** Adds [revista] to the favorites, or removes it if it is already there. */
    suspend fun toggle(revista: RevistaRef)
}
