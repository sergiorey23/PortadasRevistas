package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRevistasRepository
) {
    suspend operator fun invoke(revista: RevistaRef) = repository.toggle(revista)
}
