package tribixbite.cleverkeys.ai

import org.junit.Assert.*
import org.junit.Test

class HerbertCasePolicyTest {
    private val pair = HerbertCasePair("łódź", listOf("łódź", "Łódź"))
    private val identity = HerbertRequestIdentity(1, 2, 20, 20, 3, "pl", "pack-sha", 4, "Jadę do")
    private val cityScores = mapOf("łódź" to -6f, "Łódź" to -3f)
    private fun order(current: HerbertRequestIdentity = identity, scores: Map<String, Float> = cityScores,
                      explicit: Boolean = false, ordinary: Boolean = true, now: Long = 5) =
        pair.ordered(scores, identity, current, now, 10, explicit, ordinary)

    @Test fun best_variant_retains_both_forms() {
        assertEquals(listOf("Łódź", "łódź"), order())
        assertEquals("łódź", pair.key)
    }

    @Test fun ties_preserve_source_order() {
        assertEquals(pair.surfaces, order(scores = mapOf("łódź" to -3f, "Łódź" to -3f)))
    }

    @Test fun all_stale_editor_and_request_components_are_rejected() {
        val stale = listOf(identity.copy(editorSession = 2), identity.copy(textRevision = 3),
            identity.copy(selectionStart = 19), identity.copy(selectionEnd = 21),
            identity.copy(requestId = 4), identity.copy(language = "en"),
            identity.copy(packIdentity = "replacement"), identity.copy(settingsRevision = 5),
            identity.copy(context = "Płynę przez"))
        stale.forEach { assertNull(order(current = it)) }
    }

    @Test fun late_private_search_and_explicit_case_fall_back() {
        assertNull(order(now = 10))
        assertNull(order(ordinary = false))
        assertNull(order(explicit = true))
    }

    @Test fun missing_extra_and_nonfinite_predictions_fall_back() {
        assertNull(order(scores = mapOf("łódź" to -3f)))
        assertNull(order(scores = cityScores + ("lód" to 0f)))
        assertNull(order(scores = cityScores + ("łódź" to Float.NaN)))
        assertNull(order(scores = cityScores + ("łódź" to Float.POSITIVE_INFINITY)))
    }

    @Test fun empty_context_selection_and_non_polish_rejected_even_if_identity_matches() {
        for (id in listOf(identity.copy(context = " "), identity.copy(selectionStart = 19), identity.copy(language = "en"))) {
            assertNull(pair.ordered(cityScores, id, id, 5, 10, false, true))
        }
    }

    @Test fun invented_word_variant_rejected() {
        for (forms in listOf(listOf("łódź", "lód"), listOf("łódź", "ŁÓDŹ"), listOf("łódź", "łódź"))) {
            try { HerbertCasePair("łódź", forms); fail("accepted invalid variants") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun pair_defensively_copies_variants() {
        val forms = mutableListOf("łódź", "Łódź")
        val copied = HerbertCasePair("łódź", forms)
        forms.clear()
        assertEquals(listOf("łódź", "Łódź"), copied.surfaces)
    }

    @Test fun long_context_keeps_case_punctuation_and_recent_64_words() {
        val words = (1..80).map { "Wyraz$it" }
        val result = HerbertContextWindow.retain(words.joinToString(" ") + ". Łódź! ", 64)
        assertEquals("Wyraz18", result.substringBefore(' '))
        assertTrue(result.endsWith(". Łódź! "))
        assertEquals(64, result.trim().split(Regex("\\s+")).size)
    }

    @Test fun context_budget_does_not_invent_truncated_words_or_surrogates() {
        assertEquals("", HerbertContextWindow.retain("a".repeat(5000)))
        val result = HerbertContextWindow.retain("🚤".repeat(3000) + " Łódź. ")
        assertTrue(result.length <= 4096)
        assertTrue(result.contains("Łódź."))
        assertFalse(result.firstOrNull()?.let { Character.isLowSurrogate(it) } ?: false)
    }

    @Test fun unicode_whitespace_is_counted_without_losing_original_separators() {
        val result = HerbertContextWindow.retain((1..70).joinToString("\u00a0") { "X$it" }, 64)
        assertTrue(result.startsWith("X7\u00a0"))
        assertTrue(result.endsWith("X70"))
    }

    @Test fun default_context_retains_recent_32_words_and_short_context_is_unchanged() {
        val text = (1..70).joinToString(" ") { "X$it" }
        val result = HerbertContextWindow.retain(text)
        assertTrue(result.startsWith("X39 "))
        assertEquals(32, result.split(' ').size)
        assertEquals("Jadę do Łodzi. ", HerbertContextWindow.retain("Jadę do Łodzi. "))
        assertThrows(IllegalArgumentException::class.java) { HerbertContextWindow.retain(text, 65) }
    }
}
