package sergirex.portadasrevistas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasrevistas.domain.model.IssueDate
import sergirex.portadasrevistas.domain.model.OldIssuePolicy
import sergirex.portadasrevistas.domain.model.RevistaCategory
import sergirex.portadasrevistas.domain.model.RevistaCover
import sergirex.portadasrevistas.domain.model.RevistaRef

class OldIssuePolicyTest {
    private val now = IssueDate.toUtcMillis("20261012")!! + 12 * 60 * 60 * 1000L

    @Test
    fun `celebrity magazines are old after a week, everything else after 45 days`() {
        assertEquals(7, OldIssuePolicy.thresholdDays(RevistaCategory.CORAZON))
        RevistaCategory.entries.filter { it != RevistaCategory.CORAZON }
            .forEach { assertEquals("$it", 45, OldIssuePolicy.thresholdDays(it)) }
        assertEquals(45, OldIssuePolicy.thresholdDays(null))
    }

    @Test
    fun `the same issue can be old for one category and current for another`() {
        // 3 weeks old
        assertTrue(OldIssuePolicy.isOld("20260921", RevistaCategory.CORAZON, now))
        assertFalse(OldIssuePolicy.isOld("20260921", RevistaCategory.CULTURA, now))
        // 46 days old
        assertTrue(OldIssuePolicy.isOld("20260827", RevistaCategory.MOTOR, now))
        assertFalse(OldIssuePolicy.isOld("20260828", RevistaCategory.MOTOR, now)) // 45 days exactly
    }

    @Test
    fun `a cover is flagged by the limit of its own magazine's category`() {
        fun cover(category: RevistaCategory?) =
            RevistaCover(RevistaRef("x", "X", "", 100, "74", category), "url", issueDate = "20200101")
        assertTrue(cover(RevistaCategory.CORAZON).isOld)
        assertTrue(cover(RevistaCategory.COCINA).isOld)
        assertFalse(RevistaCover(RevistaRef("x", "X", "", 100, "74", RevistaCategory.CORAZON), "url").isOld)
    }
}
