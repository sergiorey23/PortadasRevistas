package sergirex.portadasrevistas.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasrevistas.data.local.RevistaDto
import sergirex.portadasrevistas.data.local.toDomain
import sergirex.portadasrevistas.domain.model.RevistaCategory
import java.io.File

/** Guards the bundled catalog: a typo here would otherwise only show up as a missing title or a crash at runtime. */
class CatalogAssetTest {

    private val catalog: List<RevistaDto> =
        Json.decodeFromString(File("src/main/assets/revistas.json").readText())

    @Test
    fun `every magazine has a name and valid reader settings`() {
        catalog.forEach {
            assertTrue("${it.id} has no name", it.name.isNotBlank())
            assertTrue("${it.id} has a non-positive page count", it.pages > 0)
            assertTrue("${it.id} has a non-numeric scale '${it.scale}'", it.scale.toIntOrNull() != null)
        }
    }

    @Test
    fun `ids are unique and categories are valid`() {
        assertEquals(catalog.size, catalog.map { it.id }.toSet().size)
        catalog.forEach { it.toDomain() } // throws on an unknown category
    }

    @Test
    fun `every category has at least one magazine`() {
        val used = catalog.map { it.category }.toSet()
        RevistaCategory.entries.forEach { assertTrue("$it is empty", it.name in used) }
    }
}
