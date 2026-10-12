package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.model.Revista
import sergirex.portadasrevistas.domain.model.RevistaCategory
import sergirex.portadasrevistas.domain.repository.RevistasRepository
import javax.inject.Inject

/** "Give me the magazines of this category": the seam where future rules (regional filtering, ordering) land. */
class GetRevistasByCategoryUseCase @Inject constructor(
    private val repository: RevistasRepository
) {
    suspend operator fun invoke(category: RevistaCategory): Result<List<Revista>> = repository.getByCategory(category)
}
