package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.repository.EngagementRepository
import javax.inject.Inject

class MarkReviewPromptedUseCase @Inject constructor(
    private val repository: EngagementRepository
) {
    suspend operator fun invoke() = repository.markReviewPrompted()
}
