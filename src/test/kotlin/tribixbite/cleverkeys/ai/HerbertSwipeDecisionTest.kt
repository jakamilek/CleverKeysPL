package tribixbite.cleverkeys.ai

import org.junit.Assert.*
import org.junit.Test
import tribixbite.cleverkeys.SwipeSurfaceVariants

class HerbertSwipeDecisionTest {
    private val slate = SwipeSurfaceVariants.Slate(listOf("łódź", "Łódź", "kosz"),
        listOf(190, 190, 129), listOf("pl", "pl", "pl"), listOf(true, true, false))
    private val pair = HerbertLiveSlate.pair(slate)!!
    private val identity = HerbertRequestIdentity(1, 2, 30, 30, 3, "pl", "pack-v5", 4, "Jedziemy do ")
    private val scores = mapOf("łódź" to -8f, "Łódź" to -2f)
    private fun request() = HerbertSwipeDecision(slate, pair, identity, 100)

    @Test fun onlyFirstPairMovesAndAllParallelIdentityIsPreserved() {
        val result = request().finish(scores, identity, 99)!!
        assertEquals(listOf("Łódź", "łódź", "kosz"), result.words)
        assertEquals(slate.scores, result.scores)
        assertEquals(slate.languages, result.languages)
        assertEquals(slate.exactCase, result.exactCase)
    }
    @Test fun deadlineIncludingExactBoundaryKeepsTheSourceOrder() {
        for (now in listOf(100L, 101L)) assertEquals(slate, request().finish(scores, identity, now))
    }
    @Test fun nextTouchOrFailedScoreCommitsBaselineOnceThenIgnoresNativeCallback() {
        val request = request()
        assertEquals(slate, request.finish(null, identity, 1))
        assertNull(request.finish(scores, identity, 2))
    }
    @Test fun staleEditorOrCancellationNeverCommitsEvenFallback() {
        for (current in listOf(null, identity.copy(editorSession = 9), identity.copy(textRevision = 9),
            identity.copy(selectionStart = 29), identity.copy(selectionEnd = 29), identity.copy(requestId = 9),
            identity.copy(language = "en"), identity.copy(packIdentity = "new"),
            identity.copy(settingsRevision = 9), identity.copy(context = "changed"))) {
            assertNull(request().finish(scores, current, 1))
        }
        val request = request(); request.cancel()
        assertNull(request.finish(null, identity, 1))
    }
    @Test fun stableTiesInvalidScoresAndUnknownSurfacesKeepBaseline() {
        for (value in listOf(mapOf("łódź" to -2f, "Łódź" to -2f),
            mapOf("łódź" to Float.NaN, "Łódź" to -1f), mapOf("łódź" to -2f, "Lodz" to -1f))) {
            assertEquals(slate, request().finish(value, identity, 1))
        }
    }
    @Test fun malformedOrUnalignedSlatesNeverReachTheModel() {
        for (value in listOf(slate.copy(exactCase = listOf(false, false, false)),
            slate.copy(words = listOf("łódź", "kosz", "luz")),
            slate.copy(scores = listOf(190, 180, 129)), slate.copy(scores = emptyList()),
            slate.copy(languages = listOf("pl", "en", "pl")),
            slate.copy(exactCase = listOf(true, true, true)))) assertNull(HerbertLiveSlate.pair(value))
    }
}
