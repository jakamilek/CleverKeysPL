package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test

class StartupWordPolicyTest {
    @Test fun usageCountsWinOverLexicalFrequencyAndInputOrder() {
        assertEquals(listOf("cześć", "dzięki", "tak"), StartupWordPolicy.select(
            listOf("tak" to 2, "cześć" to 20, "dzięki" to 8), mapOf("jest" to 10000)))
    }
    @Test fun fallbackFillsOnlyMissingSlots() {
        assertEquals(listOf("cześć", "tak", "jest"), StartupWordPolicy.select(
            listOf("cześć" to 5), mapOf("tak" to 90, "jest" to 80, "cześć" to 1, "nie" to 70)))
    }
    @Test fun punctuationAndZeroCountsCannotBecomeIdleIdentifiers() {
        assertEquals(listOf("tak"), StartupWordPolicy.select(
            listOf("a@b.pl" to 100, "czarno-biały" to 99, "x" to 0), mapOf("tak" to 1, "a.b" to 10)))
    }
    @Test fun disabledWordsAreExcludedFromBothTiers() {
        assertEquals(listOf("nie"), StartupWordPolicy.select(listOf("tak" to 8), mapOf("tak" to 9, "nie" to 1)) { it != "tak" })
    }
    @Test fun syntheticUserInsertionWeightIsNotFallbackUsage() {
        assertEquals(listOf("jest"), StartupWordPolicy.select(emptyList(), mapOf("xyz" to 1000, "jest" to 10), excludeFallback = setOf("xyz")))
    }
    @Test fun tiesAreDeterministicAndRepeatedPersonalWordsOccupyOneSlot() {
        assertEquals(listOf("a", "b", "c"), StartupWordPolicy.select(listOf("b" to 5, "a" to 5, "b" to 5), mapOf("d" to 4, "c" to 4)))
    }
    @Test fun noSlotsMeansNoResultsAndLargeRequestedLimitIsBounded() {
        assertTrue(StartupWordPolicy.select(emptyList(), mapOf("a" to 1), 0).isEmpty())
        val words = ('a'..'z').associate { it.toString() to 1 }
        assertEquals(10, StartupWordPolicy.select(emptyList(), words, 100).size)
    }
}
