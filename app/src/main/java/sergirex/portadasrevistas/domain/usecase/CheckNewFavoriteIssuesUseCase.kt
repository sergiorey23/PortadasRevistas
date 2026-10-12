package sergirex.portadasrevistas.domain.usecase

import kotlinx.coroutines.flow.first
import sergirex.portadasrevistas.domain.model.NewIssue
import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import sergirex.portadasrevistas.domain.repository.IssueCalendarRepository
import sergirex.portadasrevistas.domain.repository.IssueSeenRepository
import java.io.IOException
import javax.inject.Inject

/**
 * Looks for a new issue among the user's favorite magazines.
 *
 * Goes through the favorites in order (at most [MAX_FAVORITES_CHECKED] of them, to bound the
 * number of requests) and asks the service for each one's newest issue. That is compared with the
 * one recorded last time: a newer date is "new". The search stops at the first new issue found, since
 * a single magazine is all the notification names; the others are looked at on the next day's
 * check, which is when they will be reported if they are still unseen.
 *
 * The first time a magazine is seen (just made a favorite) its current issue is only recorded, not
 * reported: it isn't news, the user picked the magazine while looking at it.
 *
 * Fails, so that the caller can retry later, only when there are favorites and not one calendar
 * could be read (i.e. the network is down); a few failures among many are skipped.
 */
class CheckNewFavoriteIssuesUseCase @Inject constructor(
    private val favorites: FavoriteRevistasRepository,
    private val calendar: IssueCalendarRepository,
    private val seen: IssueSeenRepository
) {
    /** The new issue to tell the user about, or null if there is none. */
    suspend operator fun invoke(): Result<NewIssue?> {
        val favoriteList = favorites.favorites.first().take(MAX_FAVORITES_CHECKED)
        if (favoriteList.isEmpty()) return Result.success(null)

        var anyCalendarRead = false
        for (favorite in favoriteList) {
            val latest = calendar.getIssueDates(favorite.id, forceRefresh = true).getOrNull()?.firstOrNull() ?: continue
            anyCalendarRead = true
            val lastSeen = seen.lastSeen(favorite.id)
            if (lastSeen != null && latest <= lastSeen) continue
            seen.markSeen(favorite.id, latest)
            if (lastSeen != null) return Result.success(NewIssue(favorite, latest))
        }
        return if (anyCalendarRead) Result.success(null) else Result.failure(IOException("No issue calendar could be read"))
    }

    companion object {
        const val MAX_FAVORITES_CHECKED = 20
    }
}
