package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.repository.RevistasRepository
import javax.inject.Inject

class RefreshCatalogUseCase @Inject constructor(
    private val repository: RevistasRepository
) {
    suspend operator fun invoke(force: Boolean = false): Boolean = repository.refresh(force)
}
