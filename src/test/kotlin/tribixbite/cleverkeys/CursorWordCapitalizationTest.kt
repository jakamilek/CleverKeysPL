package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test

class CursorWordCapitalizationTest {
    @Test fun polishWordAtEnd() {
        assertEquals(CursorWordCapitalization.Edit(8, "ł", "Ł", 12),
            CursorWordCapitalization.plan("To jest łódź", "", 12))
    }
    @Test fun polishWordInMiddle() {
        assertEquals(CursorWordCapitalization.Edit(8, "ł", "Ł", 10),
            CursorWordCapitalization.plan("To jest łó", "dź.", 10))
    }
    @Test fun capitalizedWordReturnsToLowercase() {
        assertEquals("ł", CursorWordCapitalization.plan("Łódź", "", 4)?.replacement)
    }
    @Test fun onlyFirstLetterChanges() {
        assertEquals(CursorWordCapitalization.Edit(0, "i", "I", 6),
            CursorWordCapitalization.plan("iPhone", "", 6))
    }
    @Test fun wordStartAlsoWorks() {
        assertEquals(CursorWordCapitalization.Edit(0, "m", "M", 0),
            CursorWordCapitalization.plan("", "malina", 0))
    }
    @Test fun spaceAfterWordDoesNotReachBack() {
        assertNull(CursorWordCapitalization.plan("łódź ", "", 5))
    }
    @Test fun punctuationAndEmojiOffsetsArePreserved() {
        assertEquals(CursorWordCapitalization.Edit(4, "w", "W", 6),
            CursorWordCapitalization.plan("🙂, wa", "rszawska!", 6))
    }
    @Test fun combiningMarksRemainOutsideReplacement() {
        assertEquals(CursorWordCapitalization.Edit(0, "z", "Z", 3),
            CursorWordCapitalization.plan("z\u0307ó", "łw", 3))
    }
    @Test fun supplementaryLetterUsesTwoCodeUnits() {
        val lower = String(Character.toChars(0x10428))
        val upper = String(Character.toChars(0x10400))
        assertEquals(CursorWordCapitalization.Edit(0, lower, upper, 3),
            CursorWordCapitalization.plan(lower + "a", "", 3))
    }
    @Test fun technicalTokensAreSkipped() {
        for ((before, after) in listOf("lodz" to ".pl", "name@lodz" to "",
            "/lodz" to "", "abc_łódź" to "", "łódź" to "123", "123łódź" to "")) {
            assertNull("$before|$after", CursorWordCapitalization.plan(before, after, before.length))
        }
    }
    @Test fun boundedFetchDoesNotEditTruncatedWord() {
        assertNull(CursorWordCapitalization.plan("a".repeat(128), "", 128))
        assertNull(CursorWordCapitalization.plan("", "a".repeat(128), 0))
    }
    @Test fun uncasedLettersAndEmptyInputAreSkipped() {
        assertNull(CursorWordCapitalization.plan("東京", "", 2))
        assertNull(CursorWordCapitalization.plan("", "", 0))
    }
    @Test fun hyphenatedAndApostropheWordsChangeOnlyInitial() {
        assertEquals("D", CursorWordCapitalization.plan("don’t", "", 5)?.replacement)
        assertEquals("Ł", CursorWordCapitalization.plan("łódź-warszawa", "", 13)?.replacement)
    }
    @Test fun initialShiftCannotEditText() {
        assertNull(CursorWordCapitalization().eligiblePosition)
    }
    @Test fun realCursorMoveArmsEdit() {
        val state = CursorWordCapitalization()
        state.selection(20, 20, 10, 10)
        assertEquals(10, state.eligiblePosition)
    }
    @Test fun mutationCallbackDoesNotArmShift() {
        val state = CursorWordCapitalization()
        state.mutation(12)
        state.selection(8, 8, 12, 12)
        assertNull(state.eligiblePosition)
        state.selection(12, 12, 10, 10)
        assertEquals(10, state.eligiblePosition)
    }
    @Test fun delayedEditorReadOfPreCommitCursorDoesNotArmShift() {
        val state = CursorWordCapitalization()
        state.mutation(8)
        state.selection(8, 8, 12, 12)
        assertNull(state.eligiblePosition)
        state.selection(12, 12, 10, 10)
        assertEquals(10, state.eligiblePosition)
    }
    @Test fun rangeSelectionAndTypingDisarm() {
        val state = CursorWordCapitalization()
        state.selection(20, 20, 10, 10)
        state.selection(10, 10, 8, 12)
        assertNull(state.eligiblePosition)
        state.selection(8, 12, 12, 12)
        assertEquals(12, state.eligiblePosition)
        state.disarm()
        assertNull(state.eligiblePosition)
    }
    @Test fun repeatedEditsRemainAvailableAfterEditorCallbacks() {
        val state = CursorWordCapitalization()
        state.mutation(12, retainEdit = true)
        state.selection(12, 12, 8, 9)
        state.selection(8, 9, 9, 9)
        state.selection(9, 9, 12, 12)
        assertEquals(12, state.eligiblePosition)
    }
    @Test fun unknownMutationSkipsOneCallbackAndResetEndsSession() {
        val state = CursorWordCapitalization()
        state.mutation(-1)
        state.selection(4, 4, 8, 8)
        assertNull(state.eligiblePosition)
        state.selection(8, 8, 6, 6)
        assertEquals(6, state.eligiblePosition)
        state.reset()
        assertNull(state.eligiblePosition)
    }
}
