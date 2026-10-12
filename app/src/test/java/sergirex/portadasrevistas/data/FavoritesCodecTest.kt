package sergirex.portadasrevistas.data

import org.junit.Assert.assertEquals
import org.junit.Test
import sergirex.portadasrevistas.data.local.FavoritesCodec
import sergirex.portadasrevistas.domain.model.RevistaRef

class FavoritesCodecTest {

    private val semana = RevistaRef("3845", "Semana", "semana.es", 108, "76")
    private val love = RevistaRef("2490", "Love", "", 100, "74")

    @Test
    fun `favorites survive an encode-decode round trip, in order`() {
        assertEquals(listOf(semana, love), FavoritesCodec.decode(FavoritesCodec.encode(listOf(semana, love))))
    }

    @Test
    fun `missing or damaged data decodes to no favorites instead of crashing`() {
        assertEquals(emptyList<RevistaRef>(), FavoritesCodec.decode(null))
        assertEquals(emptyList<RevistaRef>(), FavoritesCodec.decode(""))
        assertEquals(emptyList<RevistaRef>(), FavoritesCodec.decode("{not json"))
    }
}
