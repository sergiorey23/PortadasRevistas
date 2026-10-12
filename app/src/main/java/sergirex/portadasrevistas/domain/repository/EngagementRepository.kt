package sergirex.portadasrevistas.domain.repository

import sergirex.portadasrevistas.domain.model.Engagement

interface EngagementRepository {
    /** Counts one more cover opened and returns the updated totals. */
    suspend fun recordCoverOpened(): Engagement

    suspend fun markReviewPrompted()
}
