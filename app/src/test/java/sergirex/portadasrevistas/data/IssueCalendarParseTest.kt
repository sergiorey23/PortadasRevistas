package sergirex.portadasrevistas.data

import org.junit.Assert.assertEquals
import org.junit.Test
import sergirex.portadasrevistas.data.repository.IssueCalendarRepositoryImpl

class IssueCalendarParseTest {

    @Test
    fun `reads the pipe separated dates of the first line`() {
        assertEquals(listOf("20261007", "20260930"), IssueCalendarRepositoryImpl.parse("20261007|20260930\nignored"))
    }

    @Test
    fun `drops anything that is not a date`() {
        assertEquals(listOf("20261007"), IssueCalendarRepositoryImpl.parse("20261007||abc|2026-09-30|"))
        assertEquals(emptyList<String>(), IssueCalendarRepositoryImpl.parse(""))
        assertEquals(emptyList<String>(), IssueCalendarRepositoryImpl.parse("<html>error</html>"))
    }
}
