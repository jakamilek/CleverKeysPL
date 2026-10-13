package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test

class PunctuationSpacingTest {
    @Test fun numericSentencePeriodWithExplicitSeparator() {
        for (before in listOf("3. ", "Mam 3. ", "3,5. ", "(3. ", "-3. ")) {
            assertTrue(before, SmartAutoSpace.numericPeriodStartsSentence(before, ""))
        }
    }
    @Test fun numericSentencePeriodBeforeWordSuggestion() {
        assertTrue(SmartAutoSpace.numericPeriodStartsSentence("Mam 3.", "", allowAdjacent = true))
        assertFalse(SmartAutoSpace.numericPeriodStartsSentence("Mam 3.", ""))
    }
    @Test fun decimalAndTechnicalContextsAreNotNumericSentenceBoundaries() {
        for (before in listOf("3.4", "3,4", "3:", "v3. ", "www.3. ", "1.2.3. ")) {
            assertFalse(before, SmartAutoSpace.numericPeriodStartsSentence(before, "", allowAdjacent = true))
        }
        assertFalse(SmartAutoSpace.numericPeriodStartsSentence("3.", "4", allowAdjacent = true))
        assertFalse(SmartAutoSpace.numericPeriodStartsSentence(null, ""))
        assertFalse(SmartAutoSpace.numericPeriodStartsSentence("3. ", null))
    }
    private fun insert(before: String, char: Char, after: String = ""): String {
        val edit = SmartAutoSpace.punctuationEdit(char, before, after)
        return if (edit == null) before + char + after
        else before.dropLast(edit.deleteBefore) + edit.text + after
    }
    @Test fun commaAttachesAndSeparatesTheNextWord() {
        assertEquals("Łódź, ", insert("Łódź ", ','))
        assertEquals("Łódź, ", insert("Łódź", ','))
        assertEquals("Łódź, dalej", insert("Łódź ", ',', "dalej"))
    }
    @Test fun manualSpacesAreNormalizedAtTheCaret() {
        assertEquals("malina; ", insert("malina   ", ';'))
        assertEquals("malina: ", insert("malina ", ':'))
    }
    @Test fun existingWhitespaceIsNotDuplicated() {
        assertEquals("Łódź, dalej", insert("Łódź ", ',', " dalej"))
        assertEquals("Łódź.\n", insert("Łódź ", '.', "\n"))
    }
    @Test fun punctuationClustersStayTogether() {
        assertEquals("Co?! ", insert(insert("Co ", '?'), '!'))
        assertEquals("Tak... ", insert(insert(insert("Tak ", '.'), '.'), '.'))
    }
    @Test fun closingGroupsAndQuotesAttach() {
        assertEquals("(Łódź) ", insert("(Łódź ", ')'))
        assertEquals("„Łódź” ", insert("„Łódź ", '”'))
        assertEquals("\"Łódź\" ", insert("\"Łódź ", '"'))
        assertEquals("(Łódź).", insert("(Łódź ", ')', "."))
    }
    @Test fun openingsAndLexicalJoinersAreLiteral() {
        assertEquals("tekst (", insert("tekst ", '('))
        assertEquals("tekst \"", insert("tekst ", '"'))
        assertEquals("O'", insert("O", '\''))
        assertEquals("biało-", insert("biało", '-'))
        assertFalse(SmartAutoSpace.needsLeadingSpace('„', ' '))
    }
    @Test fun numericPunctuationStaysLiteral() {
        assertEquals("3,14", insert("3", ',', "14"))
        assertEquals("12:", insert("12", ':'))
        assertEquals("1.2.", insert("1.2", '.'))
    }
    @Test fun technicalTokensAreNotFormatted() {
        assertEquals("https:", insert("https", ':'))
        assertEquals("www.", insert("www", '.'))
        assertEquals("a@b.pl.", insert("a@b.pl", '.'))
        assertEquals("foo_bar,", insert("foo_bar", ','))
    }
    @Test fun indentationAndUnknownContextArePreserved() {
        assertEquals("\n  ,", insert("\n  ", ','))
        assertEquals("word\t,", insert("word\t", ','))
        assertNull(SmartAutoSpace.punctuationEdit(',', null, ""))
        assertNull(SmartAutoSpace.punctuationEdit(',', "word ", null))
    }
    @Test fun replacementCountsOnlyTheSpaceThatExists() {
        assertEquals(5, SmartAutoSpace.committedWordDeleteCount("A łódź ", "łódź"))
        assertEquals(4, SmartAutoSpace.committedWordDeleteCount("Ałódź", "łódź"))
        assertNull(SmartAutoSpace.committedWordDeleteCount("A inny ", "łódź"))
        assertNull(SmartAutoSpace.committedWordDeleteCount(null, "łódź"))
    }
    @Test fun existingClosersSuppressSuggestionSpace() {
        for (char in listOf(' ', '\n', ',', '.', ')', '”')) {
            assertTrue(SmartAutoSpace.hasSeparatorAfter(char))
        }
        assertFalse(SmartAutoSpace.hasSeparatorAfter(null))
        assertFalse(SmartAutoSpace.hasSeparatorAfter('a'))
    }
}
