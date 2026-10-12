package sergirex.portadasrevistas.data

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import sergirex.portadasrevistas.data.local.ScaleStore
import sergirex.portadasrevistas.data.repository.PageScaleRepositoryImpl
import java.io.IOException

class PageScaleRepositoryImplTest {

    private val requestedScales = mutableListOf<Int>()
    private var limit = 71 // the service refuses pages above this scale
    private var offline = false

    private val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        if (offline) throw IOException("offline")
        val request = chain.request()
        val scale = request.url.queryParameter("scale")!!.toInt()
        requestedScales += scale
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
            .code(if (scale <= limit) 200 else 403).message("")
            .body("".toResponseBody(null)).build()
    }).build()

    private val store = object : ScaleStore {
        val values = mutableMapOf<String, Int>()
        override suspend fun get(magazineId: String) = values[magazineId]
        override suspend fun put(magazineId: String, scale: Int) { values[magazineId] = scale }
    }

    private val repository = PageScaleRepositoryImpl(client, store)

    @Test
    fun `an accepted catalog scale is used as is`() = runTest {
        limit = 90

        assertEquals(76, repository.resolve("3845", "20261007", 76))
        assertEquals(1, requestedScales.size)
    }

    @Test
    fun `a refused scale is lowered to the largest one the service accepts`() = runTest {
        assertEquals(71, repository.resolve("3845", "20261007", 76))
        assertEquals(71, store.values["3845"])
    }

    @Test
    fun `the answer is remembered and later only confirmed with one request`() = runTest {
        repository.resolve("3845", "20261007", 76)
        requestedScales.clear()

        assertEquals(71, repository.resolve("3845", "20261014", 76))
        assertEquals(listOf(71), requestedScales)
    }

    @Test
    fun `a remembered scale that stopped working is searched again`() = runTest {
        repository.resolve("3845", "20261007", 76)
        limit = 60

        assertEquals(60, repository.resolve("3845", "20261014", 76))
    }

    @Test
    fun `when the network can't answer, the catalog scale is used and nothing is remembered`() = runTest {
        offline = true

        assertEquals(76, repository.resolve("3845", "20261007", 76))
        assertEquals(emptyMap<String, Int>(), store.values)
    }

    @Test
    fun `when even the smallest scale is refused the problem isn't the scale`() = runTest {
        limit = 5 // e.g. the issue has no second page

        assertEquals(76, repository.resolve("3845", "20261007", 76))
        assertEquals(emptyMap<String, Int>(), store.values)
    }
}
