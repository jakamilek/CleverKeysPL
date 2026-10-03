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
    @Test fun leftEdgeAcceleratesSelectionAndRightEdgeAcceleratesShrinking() {
        assertEquals(30L, BackspaceGesture.repeatDelay(0f, 1000f, -1))
        assertEquals(30L, BackspaceGesture.repeatDelay(1000f, 1000f, 1))
        assertTrue(BackspaceGesture.repeatDelay(100f, 1000f, -1) <
            BackspaceGesture.repeatDelay(800f, 1000f, -1))
        assertTrue(BackspaceGesture.repeatDelay(900f, 1000f, 1) <
            BackspaceGesture.repeatDelay(200f, 1000f, 1))
    }
    @Test fun invalidGeometryKeepsASafeRepeatDelay() {
        assertEquals(200L, BackspaceGesture.repeatDelay(Float.NaN, 1000f, -1))
        assertEquals(200L, BackspaceGesture.repeatDelay(10f, 0f, 1))
        assertEquals(30L, BackspaceGesture.repeatDelay(-100f, 1000f, -1))
    }
    @Test fun firstRightwardMoveDoesNotActivateSelection() {
        val drag = BackspaceGesture.Drag(900f)
        assertFalse(drag.move(950f)); assertEquals(0, drag.direction)
        assertFalse(drag.move(890f)); assertEquals(0, drag.direction)
        assertTrue(drag.move(880f)); assertEquals(-1, drag.direction)
    }
    @Test fun rightwardReversalShrinksEvenInTheLeftHalf() {
        val drag = BackspaceGesture.Drag(900f)
        assertTrue(drag.move(100f)); assertEquals(-1, drag.direction)
        assertTrue(drag.move(103f)); assertEquals(0, drag.direction)
        assertTrue(drag.move(130f)); assertEquals(1, drag.direction)
        assertFalse(drag.move(135f)); assertEquals(1, drag.direction)
        assertTrue(drag.move(132f)); assertEquals(0, drag.direction)
        assertTrue(drag.move(100f)); assertEquals(-1, drag.direction)
    }

    @Test fun smallReversalBrakesWithoutImmediatelyShrinking() {
        val drag = BackspaceGesture.Drag(900f)
        assertTrue(drag.move(100f))
        assertTrue(drag.move(103f)); assertEquals(0, drag.direction)
        repeat(20) { assertFalse(drag.move(103f)); assertEquals(0, drag.direction) }
        assertFalse(drag.move(110f)); assertEquals(0, drag.direction)
    }
    @Test fun pausedGestureCanResumeLeftFromItsPausePosition() {
        val drag = BackspaceGesture.Drag(900f)
        drag.move(100f); drag.move(103f)
        assertFalse(drag.move(89f)); assertEquals(0, drag.direction)
        assertTrue(drag.move(88f)); assertEquals(-1, drag.direction)
    }
    @Test fun pausedGestureCanResumeRightAndBrakeAgain() {
        val drag = BackspaceGesture.Drag(900f)
        drag.move(100f); drag.move(103f)
        assertFalse(drag.move(117f)); assertEquals(0, drag.direction)
        assertTrue(drag.move(118f)); assertEquals(1, drag.direction)
        assertTrue(drag.move(115f)); assertEquals(0, drag.direction)
        assertTrue(drag.move(130f)); assertEquals(1, drag.direction)
    }
    @Test fun brakeFollowsLatestExtremeAndIgnoresSubThresholdJitter() {
        val drag = BackspaceGesture.Drag(900f)
        drag.move(100f); assertFalse(drag.move(50f))
        assertFalse(drag.move(52f)); assertEquals(-1, drag.direction)
        assertTrue(drag.move(53f)); assertEquals(0, drag.direction)
        assertFalse(drag.move(54f)); assertFalse(drag.move(52f))
        assertFalse(drag.move(67f)); assertEquals(0, drag.direction)
        assertTrue(drag.move(68f)); assertEquals(1, drag.direction)
    }
    @Test fun nonFiniteMotionCannotBrakeOrResume() {
        val drag = BackspaceGesture.Drag(900f)
        drag.move(100f)
        assertFalse(drag.move(Float.NaN)); assertEquals(-1, drag.direction)
        drag.move(103f)
        assertFalse(drag.move(Float.POSITIVE_INFINITY)); assertEquals(0, drag.direction)
        assertTrue(drag.move(118f)); assertEquals(1, drag.direction)
    }
}
