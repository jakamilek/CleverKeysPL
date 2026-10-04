package tribixbite.cleverkeys.ai

import org.junit.Assert.*
import org.junit.Test

class HerbertPreparedBatchTest {
    private val special = HerbertPreparedBatch.SpecialTokens(0, 2, 4, 1, 3)
    private fun candidate(surface: String = "łódź", target: LongArray = longArrayOf(17), left: LongArray = longArrayOf(5)) =
        HerbertPreparedBatch.CandidateTokens(surface, left, target)

    @Test fun candidate_padding_keeps_attention_and_targets_aligned() {
        val batch = HerbertPreparedBatch.create(listOf(candidate(), candidate("Łódź", longArrayOf(18,19))), special)
        assertEquals(2, batch.size)
        assertEquals(5, batch.sequence)
        assertEquals(2, batch.targets)
        assertArrayEquals(longArrayOf(0,5,4,2,1,0,5,4,4,2), batch.inputIds())
        assertArrayEquals(longArrayOf(1,1,1,1,0,1,1,1,1,1), batch.attentionMask())
        assertArrayEquals(longArrayOf(2,0,2,3), batch.targetPositions())
        assertArrayEquals(longArrayOf(17,0,18,19), batch.targetTokenIds())
        assertArrayEquals(floatArrayOf(1f,0f,1f,1f), batch.targetMask(), 0f)
    }

    @Test fun feed_is_immune_to_caller_mutation() {
        val target = longArrayOf(17)
        val left = longArrayOf(5)
        val batch = HerbertPreparedBatch.create(listOf(candidate(target = target, left = left)), special)
        target[0] = 999; left[0] = 999
        batch.inputIds()[1] = 999
        batch.targetTokenIds()[0] = 999
        assertArrayEquals(longArrayOf(0,5,4,2), batch.inputIds())
        assertArrayEquals(longArrayOf(17), batch.targetTokenIds())
    }

    @Test fun unsupported_target_and_invalid_token_are_refused() {
        for (target in listOf(longArrayOf(), longArrayOf(3), longArrayOf(-1), longArrayOf(50_000))) {
            try { HerbertPreparedBatch.create(listOf(candidate(target = target)), special); fail("accepted target") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun duplicate_empty_and_excessive_candidates_refused() {
        for (candidates in listOf(emptyList(), listOf(candidate(), candidate()),
            (1..13).map { candidate(surface = "x$it") })) {
            try { HerbertPreparedBatch.create(candidates, special); fail("accepted slate") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun oversized_sequence_and_target_refused() {
        for (c in listOf(candidate(left = LongArray(510) { 5 }), candidate(target = LongArray(33) { 17 }))) {
            try { HerbertPreparedBatch.create(listOf(c), special); fail("accepted budget") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun boundary_sequence_is_supported_without_silent_truncation() {
        val batch = HerbertPreparedBatch.create(listOf(candidate(left = LongArray(509) { 5 })), special)
        assertEquals(512, batch.sequence)
        assertEquals(510L, batch.targetPositions()[0])
        assertEquals(2L, batch.inputIds().last())
    }
}
