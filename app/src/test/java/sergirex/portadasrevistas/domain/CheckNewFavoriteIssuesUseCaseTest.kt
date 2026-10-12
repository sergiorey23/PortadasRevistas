package sergirex.portadasrevistas.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import sergirex.portadasrevistas.domain.repository.IssueCalendarRepository
import sergirex.portadasrevistas.domain.repository.IssueSeenRepository
import sergirex.portadasrevistas.domain.usecase.CheckNewFavoriteIssuesUseCase
import java.io.IOException

class CheckNewFavoriteIssuesUseCaseTest {

    private val semana = RevistaRef("3845", "Semana", "semana.es", 108, "76")
    private val lecturas = RevistaRef("3741", "Lecturas", "lecturas.com", 100, "74")

    private val favoritesFlow = MutableStateFlow<List<RevistaRef>>(listOf(semana, lecturas))
    private val favorites = object : FavoriteRevistasRepository {
        override val favorites: Flow<List<RevistaRef>> = favoritesFlow
        override suspend fun toggle(revista: RevistaRef) = Unit
    }

    /** The newest issue of each magazine, or absent when its calendar can't be read. */
    private val newest = mutableMapOf("3845" to "20261007", "3741" to "20261007")
    private var calendarRequestsForced = 0
    private val calendar = object : IssueCalendarRepository {
        override suspend fun getIssueDates(magazineId: String, forceRefresh: Boolean): Result<List<String>> {
            if (forceRefresh) calendarRequestsForced++
            val latest = newest[magazineId] ?: return Result.failure(IOException("offline"))
            return Result.success(listOf(latest, "20260101"))
        }
    }

    private val seenDates = mutableMapOf<String, String>()
    private val seen = object : IssueSeenRepository {
        override suspend fun lastSeen(magazineId: String) = seenDates[magazineId]
        override suspend fun markSeen(magazineId: String, date: String) { seenDates[magazineId] = date }
    }

    private val check = CheckNewFavoriteIssuesUseCase(favorites, calendar, seen)

    @Test
    fun `the first look at a magazine only records its current issue`() = runTest {
        assertNull(check().getOrThrow())
        assertEquals(mapOf("3845" to "20261007", "3741" to "20261007"), seenDates)
    }

    @Test
    fun `a newer issue is reported once, naming its magazine`() = runTest {
        check()
        newest["3845"] = "20261014"

        val issue = check().getOrThrow()
        assertEquals("Semana", issue?.revista?.name)
        assertEquals("20261014", issue?.date)

        assertNull(check().getOrThrow()) // already told
    }

    @Test
    fun `the search stops at the first new issue and leaves the others for the next check`() = runTest {
        check()
        newest["3845"] = "20261014"
        newest["3741"] = "20261014"
        calendarRequestsForced = 0

        assertEquals("Semana", check().getOrThrow()?.revista?.name)
        assertEquals(1, calendarRequestsForced) // Lecturas wasn't even asked about

        assertEquals("Lecturas", check().getOrThrow()?.revista?.name) // reported the next day
    }

    @Test
    fun `only a limited number of favorites is looked at`() = runTest {
        val many = (1..30).map { RevistaRef("m$it", "M$it", "", 100, "74") }
        many.forEach { newest[it.id] = "20261007" }
        favoritesFlow.value = many

        check()

        assertEquals(CheckNewFavoriteIssuesUseCase.MAX_FAVORITES_CHECKED, calendarRequestsForced)
    }

    @Test
    fun `the calendar is asked fresh, not from a cache`() = runTest {
        check()

        assertEquals(2, calendarRequestsForced)
    }

    @Test
    fun `no favorites means nothing to check and no network use`() = runTest {
        favoritesFlow.value = emptyList()

        assertNull(check().getOrThrow())
        assertEquals(0, calendarRequestsForced)
    }

    @Test
    fun `a few unreadable calendars are skipped, the rest still work`() = runTest {
        check()
        newest.remove("3741")
        newest["3845"] = "20261014"

        assertEquals("Semana", check().getOrThrow()?.revista?.name)
    }

    @Test
    fun `when no calendar can be read the check fails so it can be retried, and records nothing`() = runTest {
        newest.clear()

        assertTrue(check().isFailure)
        assertTrue(seenDates.isEmpty())
    }
}
