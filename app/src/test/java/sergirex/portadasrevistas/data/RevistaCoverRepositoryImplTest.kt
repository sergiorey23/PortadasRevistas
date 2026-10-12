package sergirex.portadasrevistas.data

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sergirex.portadasrevistas.data.repository.RevistaCoverRepositoryImpl
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.IssueCalendarRepository

class RevistaCoverRepositoryImplTest {

    private val semana = RevistaRef("3845", "Semana", "semana.es", 108, "76")
    private val sin = RevistaRef("zzzz", "Desconocida", "", 100, "74")
    private val mensual = RevistaRef("m001", "Mensual", "", 100, "74")

    /** Serves a JPEG for every id except [sin], which — like the real service — gets an SVG placeholder. */
    private val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        val request = chain.request()
        val type = if ("/zzzz/" in request.url.toString()) "image/svg+xml" else "image/jpeg"
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK").header("Content-Type", type)
            .body("".toResponseBody(type.toMediaType())).build()
    }).build()

    private val calendar = object : IssueCalendarRepository {
        override suspend fun getIssueDates(magazineId: String) = when (magazineId) {
            "3845" -> Result.success(listOf("20261007", "20260930"))
            else -> Result.failure(java.io.IOException("offline"))
        }
    }

    private val repository = RevistaCoverRepositoryImpl(client, calendar)

    @Test
    fun `only magazines with a real cover are emitted, in list order`() = runTest {
        val covers = repository.getCovers(listOf(sin, semana, mensual), forceRefresh = true).toList()

        assertEquals(listOf("3845", "m001"), covers.map { it.revista.id })
    }

    @Test
    fun `a cover carries the date of the magazine's newest issue`() = runTest {
        val cover = repository.getCovers(listOf(semana), forceRefresh = true).toList().single()

        assertEquals("20261007", cover.issueDate)
        assertEquals("https://www.kioskoymas.com/Imageresize/getLatestImage/3845/424/600", cover.imageUrl)
    }

    @Test
    fun `an unreadable calendar still shows the cover, just undated and never flagged as old`() = runTest {
        val cover = repository.getCovers(listOf(mensual), forceRefresh = true).toList().single()

        assertNull(cover.issueDate)
        assertEquals(false, cover.isOld)
    }
}
