package sergirex.portadasrevistas.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import sergirex.portadasrevistas.domain.model.RevistaCategory

class CatalogParserTest {

    @Test
    fun `a valid catalog is read with its version`() {
        val parsed = CatalogParser.parse(catalogJson(version = 7, count = 12))!!
        assertEquals(7, parsed.version)
        assertEquals(12, parsed.revistas.size)
        assertEquals(0, parsed.skipped)
        assertEquals("Magazine 3", parsed.revistas[3].name)
    }

    @Test
    fun `bad entries are skipped without affecting the rest`() {
        val bad = listOf(
            """{"id":"","name":"No id","category":"CORAZON"}""",
            """{"id":"noname","name":"  ","category":"CORAZON"}""",
            """{"id":"weird","name":"Weird","category":"SPACE"}""",
            """{"id":"p1","name":"Duplicate of p1","category":"CORAZON"}""",
            """{"id":"nopages","name":"No pages","category":"CORAZON","pages":0}""",
            """{"id":"badscale","name":"Bad scale","category":"CORAZON","scale":"big"}""",
            """{"id":"incomplete"}""",
            """"just a string""""
        )
        val parsed = CatalogParser.parse(catalogJson(count = 12, extra = bad))!!
        assertEquals(12, parsed.revistas.size)
        assertEquals(8, parsed.skipped)
        assertEquals("Magazine 1", parsed.revistas.first { it.id == "p1" }.name)
    }

    @Test
    fun `a file written for a newer app is rejected`() {
        assertNull(CatalogParser.parse(catalogJson(schema = CatalogParser.SUPPORTED_SCHEMA + 1)))
    }

    @Test
    fun `a catalog with too few usable entries is rejected so a bad deploy cannot empty the app`() {
        assertNull(CatalogParser.parse(catalogJson(count = CatalogParser.MIN_ENTRIES - 1)))
        assertNull(CatalogParser.parse(catalogJson(count = 0)))
    }

    @Test
    fun `things that are not a catalog are rejected`() {
        listOf("", "not json", "42", "null", "{}", """{"revistas":"nope"}""", "<html>404</html>").forEach {
            assertNull("accepted: $it", CatalogParser.parse(it))
        }
    }

    @Test
    fun `the original bare array format is still understood`() {
        val bare = (0 until 12).joinToString(",", "[", "]") {
            """{"id":"b$it","name":"B $it","category":"MOTOR"}"""
        }
        val parsed = CatalogParser.parse(bare)!!
        assertEquals(0, parsed.version)
        assertEquals(RevistaCategory.MOTOR, parsed.revistas.first().category)
        assertNotNull(parsed)
    }

    @Test
    fun `unknown extra fields are ignored so the format can grow`() {
        val text = catalogJson().replace(""""scale":"74"""", """"scale":"74","newField":123""")
        assertEquals(12, CatalogParser.parse(text)!!.revistas.size)
    }
}
