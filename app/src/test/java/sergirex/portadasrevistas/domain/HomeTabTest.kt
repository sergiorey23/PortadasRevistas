package sergirex.portadasrevistas.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import sergirex.portadasrevistas.domain.model.HomeTab
import sergirex.portadasrevistas.domain.model.RevistaCategory

class HomeTabTest {

    @Test
    fun `stable keys round-trip`() {
        (listOf(HomeTab.Favorites) + HomeTab.categories).forEach {
            assertEquals(it, HomeTab.fromKey(it.key))
        }
    }

    @Test
    fun `positions stored by older versions are still understood`() {
        // The old "init_category" preference stored the tab position: 0 = favorites, 1.. = categories in tab order.
        assertEquals(HomeTab.Favorites, HomeTab.fromKey("0"))
        assertEquals(HomeTab.Category(RevistaCategory.CORAZON), HomeTab.fromKey("1"))
        assertEquals(HomeTab.Category(RevistaCategory.DEPORTES), HomeTab.fromKey("3"))
        assertEquals(HomeTab.Category(RevistaCategory.HISTORIA), HomeTab.fromKey("10"))
    }

    @Test
    fun `unknown or missing values fall back to the default tab`() {
        assertEquals(HomeTab.default, HomeTab.fromKey(null))
        assertEquals(HomeTab.default, HomeTab.fromKey("???"))
        assertEquals(HomeTab.default, HomeTab.fromKey("11"))
    }
}
