package sergirex.portadasrevistas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasrevistas.data.catalog.CatalogParser
import sergirex.portadasrevistas.domain.model.RevistaCategory
import java.io.File

/** Guards the bundled catalog and its published copy: a typo would otherwise only show up at runtime. */
class CatalogAssetTest {

    private val asset = File("src/main/assets/revistas.json")
    private val published = File("../docs/catalog/revistas.json")

    private val catalog = CatalogParser.parse(asset.readText())

    @Test
    fun `the bundled catalog is valid and every entry survived validation`() {
        assertNotNull("the bundled catalog doesn't validate", catalog)
        assertEquals("some entries were skipped as invalid", 0, catalog!!.skipped)
    }

    @Test
    fun `every category has at least one magazine`() {
        val used = catalog!!.revistas.map { it.category }.toSet()
        RevistaCategory.entries.forEach { assertTrue("$it is empty", it in used) }
    }

    @Test
    fun `the published copy is identical to the bundled one`() {
        assertEquals(
            "docs/catalog/revistas.json must be a copy of app/src/main/assets/revistas.json",
            asset.readText(),
            published.readText()
        )
    }
}
