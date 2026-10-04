package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test

class BackspaceGestureTest {
    @Test fun wordDeletionPreservesThePrecedingSpace() {
        val s = "olej mleko "
        val span = BackspaceGesture.previousWord(s)!!
        assertEquals("mleko ", s.substring(span.start, span.end))
        assertEquals("olej ", s.substring(0, span.start))
    }
    @Test fun wordWithoutTrailingSpaceAndFirstWordWork() {
        assertEquals(BackspaceGesture.Span(5, 10), BackspaceGesture.previousWord("olej mleko"))
        assertEquals(BackspaceGesture.Span(0, 5), BackspaceGesture.previousWord("łódź "))
    }
    @Test fun lineBreakAndIndentationArePreserved() {
        assertEquals(BackspaceGesture.Span(7, 12), BackspaceGesture.previousWord("olej\n  mleko"))
    }
    @Test fun emptyAndWhitespaceOnlyHaveNoWord() {
        for (s in listOf("", " ", "\t", "\n ")) assertNull(BackspaceGesture.previousWord(s))
    }
    @Test fun truncatedTokenIsNotDeletedAsAWholeWord() {
        assertNull(BackspaceGesture.previousWord("mleko ", hasEarlierText = true))
        assertEquals(BackspaceGesture.Span(2, 7), BackspaceGesture.previousWord("a mleko", true))
    }
    @Test fun selectionStepsDoNotSplitSurrogatePairs() {
        assertEquals(1, BackspaceGesture.step("a🙂b", 3, -1))
        assertEquals(3, BackspaceGesture.step("a🙂b", 1, 1))
    }
    @Test fun selectionCannotExtendPastAnchorOrBufferStart() {
        assertEquals(0, BackspaceGesture.step("abc", 0, -1))
        assertEquals(3, BackspaceGesture.step("abc", 3, 1))
    }
    private fun motion(step: Float = 10f, maximum: Float = 1f) =
        SliderMotion(100f, 0f, step, 0.6f, maximum)

    @Test fun stationaryFingerNeverMovesEvenAfterALongWait() {
        val m = motion()
        assertEquals(-2, m.move(80f, 0f, 100))
        repeat(20) { assertEquals(0, m.move(80f, 0f, 10000L + it)) }
    }
    @Test fun slowMovementAccumulatesFractionsIntoCharacters() {
        val m = motion()
        assertEquals(0, m.move(91f, 0f, 100))
        assertEquals(-1, m.move(90f, 0f, 200))
        assertEquals(0, m.move(86f, 0f, 300))
        assertEquals(-1, m.move(80f, 0f, 400))
    }
    @Test fun reverseMovementShrinksWithoutABrakeOrResumeDistance() {
        val m = motion()
        assertEquals(-3, m.move(70f, 0f, 100))
        assertEquals(1, m.move(80f, 0f, 200))
        assertEquals(-1, m.move(70f, 0f, 300))
    }
    @Test fun fasterFingerAcceleratesMoreThanSlowerFinger() {
        val slow = motion(maximum = 6f); val fast = motion(maximum = 6f)
        slow.move(90f, 0f, 100); fast.move(90f, 0f, 100)
        slow.move(80f, 0f, 1100); fast.move(80f, 0f, 101)
        assertTrue(kotlin.math.abs(fast.move(60f, 0f, 102)) >
            kotlin.math.abs(slow.move(60f, 0f, 2100)))
    }
    @Test fun sharedSensitivityChangesDistancePerCharacter() {
        assertEquals(-4, motion(step = 5f).move(80f, 0f, 100))
        assertEquals(-1, motion(step = 20f).move(80f, 0f, 100))
    }
    @Test fun speedDependsOnMotionNotAbsoluteScreenPosition() {
        val a = motion(maximum = 6f)
        val b = SliderMotion(900f, 0f, 10f, 0.6f, 6f)
        for ((i, x) in listOf(90f, 70f, 75f, 95f).withIndex()) {
            assertEquals(a.move(x, 0f, 100L + i * 30), b.move(x + 800f, 0f, 100L + i * 30))
        }
    }
    @Test fun sameTimestampAndInvalidInputCannotPoisonMotion() {
        val m = motion(maximum = 6f)
        assertEquals(0, m.move(Float.NaN, 0f, 100))
        assertEquals(0, m.move(90f, Float.POSITIVE_INFINITY, 100))
        assertEquals(-1, m.move(90f, 0f, 100))
        assertTrue(m.move(80f, 0f, 100) < 0)
        assertTrue(m.move(70f, 0f, 99) < 0)
    }
    @Test fun corruptSliderOptionsHaveFiniteUsableFallbacks() {
        val m = SliderMotion(100f, 0f, 0f, Float.NaN, Float.NaN)
        assertEquals(-1, m.move(70f, 0f, 100))
        assertEquals(0, m.move(70f, 0f, 100))
    }
    @Test fun hugeEventIsBoundedAndCannotContinueAfterFingerStops() {
        val m = motion()
        assertEquals(-256, m.move(-10000f, 0f, 100))
        assertEquals(0, m.move(-10000f, 0f, 10000))
    }
    @Test fun verticalSpaceSliderRetainsItsAxisAndMultiplier() {
        val m = SliderMotion(0f, 100f, 10f, 0.6f, 1f, 0, -1, 0.5f)
        assertEquals(1, m.move(0f, 80f, 100))
        assertEquals(-1, m.move(0f, 100f, 200))
    }
    @Test fun multipleCharacterStepsStayWithinAnchorAndPreserveUnicode() {
        assertEquals(1, BackspaceGesture.step("a🙂b🙂c", 6, -3))
        assertEquals(6, BackspaceGesture.step("a🙂b🙂c", 1, 3))
        assertEquals(0, BackspaceGesture.step("a🙂b", 4, Int.MIN_VALUE))
        assertEquals(4, BackspaceGesture.step("a🙂b", 0, Int.MAX_VALUE))
    }
}
