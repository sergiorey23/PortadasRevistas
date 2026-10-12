package sergirex.portadasrevistas.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import sergirex.portadasrevistas.domain.model.RevistaRef

class RevistaRefTest {

    @Test
    fun `parses the full legacy encoding`() {
        assertEquals(RevistaRef("3845", "Semana", "semana.es", 108, "76"), RevistaRef.fromLegacyEncoded("3845:Semana:semana.es:108:76"))
    }

    @Test
    fun `missing parts fall back to the old defaults`() {
        assertEquals(RevistaRef("2490", "Love", "", 100, "74"), RevistaRef.fromLegacyEncoded("2490:Love"))
        assertEquals(RevistaRef("ebkm", "Hola México", "", 116, "91"), RevistaRef.fromLegacyEncoded("ebkm:Hola México::116:91"))
        assertEquals(RevistaRef("x", "x", "", 100, "74"), RevistaRef.fromLegacyEncoded("x"))
    }

    @Test
    fun `a malformed page count does not break parsing`() {
        assertEquals(100, RevistaRef.fromLegacyEncoded("a:A:a.es:many:70").pages)
    }
}
