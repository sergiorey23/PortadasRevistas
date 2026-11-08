package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import javax.inject.Inject

class ClearFavoritesUseCase @Inject constructor(
    private val repository: FavoriteRevistasRepository
) {
    suspend operator fun invoke() = repository.clear()
}
