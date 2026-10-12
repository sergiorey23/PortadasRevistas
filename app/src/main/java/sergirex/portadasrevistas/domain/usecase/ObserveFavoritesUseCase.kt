package sergirex.portadasrevistas.domain.usecase

import kotlinx.coroutines.flow.Flow
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: FavoriteRevistasRepository
) {
    operator fun invoke(): Flow<List<RevistaRef>> = repository.favorites
}
