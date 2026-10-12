package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.model.CoverOpenOutcome
import sergirex.portadasrevistas.domain.model.InterstitialPolicy
import sergirex.portadasrevistas.domain.model.ReviewPromptPolicy
import sergirex.portadasrevistas.domain.repository.EngagementRepository
import javax.inject.Inject

class RecordCoverOpenedUseCase @Inject constructor(
    private val repository: EngagementRepository
) {
    /** Counts a cover opened; returns whether this is a good moment to ask for a review or show the full-screen ad. */
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): CoverOpenOutcome {
        val engagement = repository.recordCoverOpened()
        return CoverOpenOutcome(
            promptReview = ReviewPromptPolicy.shouldPrompt(engagement, now),
            showInterstitial = InterstitialPolicy.shouldShow(engagement.coverOpens)
        )
    }
}
