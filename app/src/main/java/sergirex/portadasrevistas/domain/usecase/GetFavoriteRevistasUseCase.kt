package sergirex.portadasrevistas.domain.usecase

import kotlinx.coroutines.flow.first
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import javax.inject.Inject

class GetFavoriteRevistasUseCase @Inject constructor(
    private val repository: FavoriteRevistasRepository
) {
    suspend operator fun invoke(): List<RevistaRef> = repository.favorites.first()
}
