package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test

class CursorWordCapitalizationTest {
    @Test fun polishWordAtEnd() {
        assertNull(CursorWordCapitalization.plan("To jest łódź", "", 12))
    }
    @Test fun polishWordInMiddle() {
        assertEquals(CursorWordCapitalization.Edit(8, "ł", "Ł", 10),
            CursorWordCapitalization.plan("To jest łó", "dź.", 10))
    }
    @Test fun capitalizedWordReturnsToLowercase() {
        assertEquals("ł", CursorWordCapitalization.plan("Łó", "dź", 2)?.replacement)
    }
    @Test fun onlyFirstLetterChanges() {
        assertEquals(CursorWordCapitalization.Edit(0, "i", "I", 3),
            CursorWordCapitalization.plan("iPh", "one", 3))
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
            CursorWordCapitalization.plan(lower + "a", "b", 3))
    }
    @Test fun technicalTokensAreSkipped() {
        for ((before, after) in listOf("lod" to "z.pl", "name@lo" to "dz",
            "/lo" to "dz", "abc_łó" to "dź", "łó" to "dź123", "123łó" to "dź")) {
            assertNull("$before|$after", CursorWordCapitalization.plan(before, after, before.length))
        }
    }
    @Test fun boundedFetchDoesNotEditTruncatedWord() {
        assertNull(CursorWordCapitalization.plan("a".repeat(128), "", 128))
        assertNull(CursorWordCapitalization.plan("", "a".repeat(128), 0))
    }
    @Test fun uncasedLettersAndEmptyInputAreSkipped() {
        assertNull(CursorWordCapitalization.plan("東", "京", 1))
        assertNull(CursorWordCapitalization.plan("", "", 0))
    }
    @Test fun hyphenatedAndApostropheWordsChangeOnlyInitial() {
        assertEquals("D", CursorWordCapitalization.plan("don", "’t", 3)?.replacement)
        assertEquals("Ł", CursorWordCapitalization.plan("łódź-war", "szawa", 8)?.replacement)
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
    @Test fun caseEditWithoutFinalCallbackDoesNotSwallowTheNextUserMove() {
        val state = CursorWordCapitalization()
        state.mutation(96, retainEdit = true)
        // Batch edit ended at the same caret: Android need not send a final callback.
        state.selection(96, 96, 87, 87)
        assertEquals(87, state.eligiblePosition)
    }
    @Test fun everyInteriorPositionWorksAndTheWordEndDoesNot() {
        for (word in listOf("łódź", "malina", "warszawska", "znowu")) {
            for (offset in 0 until word.length) {
                val before = "A " + word.take(offset)
                val after = word.drop(offset) + "."
                assertNotNull("$word at $offset", CursorWordCapitalization.plan(before, after, before.length))
            }
            assertNull(CursorWordCapitalization.plan("A $word", ".", word.length + 2))
        }
    }
    @Test fun cursorKeyDisarmingTheRepeatDoesNotTurnACaseEditIntoATypingMutation() {
        val state = CursorWordCapitalization()
        state.mutation(96, retainEdit = true)
        state.disarm()
        state.selection(96, 96, 87, 87)
        assertEquals(87, state.eligiblePosition)
    }
}
