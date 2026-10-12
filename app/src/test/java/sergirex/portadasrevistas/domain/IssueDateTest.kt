package sergirex.portadasrevistas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import sergirex.portadasrevistas.domain.model.IssueDate

class IssueDateTest {

    @Test
    fun `the date picker's UTC midnight is the same calendar day`() {
        val millis = IssueDate.toUtcMillis("20261007")!!
        assertEquals("20261007", IssueDate.fromUtcMillis(millis))
    }

    @Test
    fun `display format is day first`() {
        assertEquals("07/10/2026", IssueDate.toDisplay("20261007"))
        assertEquals("garbage", IssueDate.toDisplay("garbage"))
    }

    @Test
    fun `an unparseable date has no millis`() {
        assertNull(IssueDate.toUtcMillis("nope"))
    }

    private fun at(date: String) = IssueDate.toUtcMillis(date)!! + 12 * 60 * 60 * 1000L

    @Test
    fun `an issue is old only after more than the given number of days`() {
        val now = at("20261012")
        assertFalse(IssueDate.isOld("20261012", 7, now))
        assertFalse(IssueDate.isOld("20261005", 7, now)) // exactly 7 days
        assertTrue(IssueDate.isOld("20261004", 7, now)) // 8 days
        assertTrue(IssueDate.isOld("20250101", 7, now))
    }

    @Test
    fun `a date that cannot be read is never flagged`() {
        assertFalse(IssueDate.isOld("nope", 7, at("20261012")))
    }

    @Test
    fun `the ribbon label is day, short month and year in the given language`() {
        assertEquals("07 Oct 2026", IssueDate.toRibbonLabel("20261007", Locale.ENGLISH))
        assertEquals("07 oct 2026", IssueDate.toRibbonLabel("20261007", Locale("es", "ES")))
        assertEquals("nope", IssueDate.toRibbonLabel("nope", Locale.ENGLISH))
    }
}
