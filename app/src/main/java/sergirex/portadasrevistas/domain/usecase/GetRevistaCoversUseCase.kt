package sergirex.portadasrevistas.domain.usecase

import kotlinx.coroutines.flow.Flow
import sergirex.portadasrevistas.domain.model.RevistaCover
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.RevistaCoverRepository
import javax.inject.Inject

class GetRevistaCoversUseCase @Inject constructor(
    private val repository: RevistaCoverRepository
) {
    operator fun invoke(revistas: List<RevistaRef>, forceRefresh: Boolean): Flow<RevistaCover> =
        repository.getCovers(revistas, forceRefresh)
}
