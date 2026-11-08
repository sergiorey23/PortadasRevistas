package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.model.ReviewPromptPolicy
import sergirex.portadasrevistas.domain.repository.EngagementRepository
import javax.inject.Inject

class RecordCoverOpenedUseCase @Inject constructor(
    private val repository: EngagementRepository
) {
    /** Counts a cover opened; returns whether this is a good moment to ask for a Play review. */
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): Boolean =
        ReviewPromptPolicy.shouldPrompt(repository.recordCoverOpened(), now)
}
