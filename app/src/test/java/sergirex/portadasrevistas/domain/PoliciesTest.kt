package sergirex.portadasrevistas.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasrevistas.domain.model.Engagement
import sergirex.portadasrevistas.domain.model.Entitlements
import sergirex.portadasrevistas.domain.model.InterstitialPolicy
import sergirex.portadasrevistas.domain.model.ReviewPromptPolicy

class PoliciesTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_000 * day

    private fun engaged(opens: Int = 5, installedDaysAgo: Long = 3, promptedDaysAgo: Long? = null) =
        Engagement(opens, now - installedDaysAgo * day, promptedDaysAgo?.let { now - it * day })

    @Test
    fun `review is requested once the user is engaged enough`() {
        assertTrue(ReviewPromptPolicy.shouldPrompt(engaged(), now))
    }

    @Test
    fun `never on first use`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(engaged(opens = 1, installedDaysAgo = 0), now))
    }

    @Test
    fun `not before enough covers were opened or enough days passed`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(engaged(opens = 4), now))
        assertFalse(ReviewPromptPolicy.shouldPrompt(engaged(installedDaysAgo = 2), now))
    }

    @Test
    fun `not again soon after a previous request, but again after the cooldown`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(engaged(promptedDaysAgo = 89), now))
        assertTrue(ReviewPromptPolicy.shouldPrompt(engaged(promptedDaysAgo = 90), now))
    }

    @Test
    fun `the full-screen ad shows on every third cover`() {
        assertEquals3(listOf(1, 2, 3, 4, 5, 6).filter(InterstitialPolicy::shouldShow))
        assertFalse(InterstitialPolicy.shouldShow(0))
    }

    private fun assertEquals3(shown: List<Int>) = org.junit.Assert.assertEquals(listOf(3, 6), shown)

    @Test
    fun `ads are shown until the remove-ads purchase`() {
        assertTrue(Entitlements().showAds)
        assertFalse(Entitlements(adsRemoved = true).showAds)
    }
}
