package tribixbite.cleverkeys

import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.objenesis.ObjenesisStd

/** Real pointer routing: hold is non-destructive and owns moves outside the original key. */
class PointersBackspaceHoldTest {
    private lateinit var pointers: Pointers
    private lateinit var handler: Pointers.IPointerEventHandler
    private lateinit var timers: Handler
    private lateinit var ptr: Pointers.Pointer
    private val ptrs = ArrayList<Pointers.Pointer>()

    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        handler = mockk(relaxed = true); timers = mockk(relaxed = true)
        every { handler.beginBackspaceHold() } returns true
        every { handler.beginBackspaceDrag() } returns true
        every { handler.backspaceKeyboardWidth() } returns 1000f
        val key = KeyValue.keyeventKey(0xE003, KeyEvent.KEYCODE_DEL, 0)
        ptr = Pointers.Pointer(1, KeyboardData.Key.EMPTY.withKeyValue(0,key), key, 900f, 100f,
            Pointers.Modifiers.EMPTY, Pointers.FLAG_P_DEFERRED_DOWN,
            testConfigSnapshot(swipe_typing_enabled = false, keyrepeat_enabled = false))
        pointers = ObjenesisStd().newInstance(Pointers::class.java)
        field("_handler", handler); field("_ptrs", ptrs); field("_longpress_handler", timers)
        ptrs.add(ptr)
    }
    @After fun teardown() { unmockkStatic(Log::class) }
    private fun field(name: String, value: Any) {
        Pointers::class.java.getDeclaredField(name).apply { isAccessible = true }.set(pointers,value)
    }
    private fun hold() {
        Pointers::class.java.getDeclaredMethod("handleLongPress", Pointers.Pointer::class.java)
            .apply { isAccessible = true }.invoke(pointers,ptr)
    }
    @Test fun diagnosticsDistinguishHoldMoveReversalAndRelease() {
        val trace = mutableListOf<String>()
        every { handler.traceBackspace(any()) } answers { trace.add(firstArg()); Unit }
        hold(); pointers.onTouchMove(100f,200f,1); pointers.onTouchMove(103f,200f,1)
        pointers.onTouchMove(130f,200f,1)
        pointers.onTouchUp(1)
        assertTrue(trace.any { it.contains("hold started=true") })
        assertEquals(1,trace.count { it.contains("move observed") })
        assertTrue(trace.any { it.contains("move direction=-1") })
        assertTrue(trace.any { it.contains("move direction=0") })
        assertTrue(trace.any { it.contains("move direction=1") })
        assertTrue(trace.last().contains("up pointer=1 owned=true"))
        verify(exactly = 1) { handler.finishBackspaceHold(true) }
    }
    @Test fun diagnosticsExposeCancellationInsteadOfCommit() {
        val trace = mutableListOf<String>()
        every { handler.traceBackspace(any()) } answers { trace.add(firstArg()); Unit }
        hold(); pointers.clear()
        assertTrue(trace.last().contains("clear pointer=1 owned=true"))
        verify(exactly = 1) { handler.finishBackspaceHold(false) }
        verify(exactly = 0) { handler.finishBackspaceHold(true) }
    }
    @Test fun diagnosticFailureDoesNotLoseHoldMoveOrRelease() {
        every { handler.traceBackspace(any()) } throws IllegalStateException("sink failure")
        hold(); pointers.onTouchMove(100f,200f,1); pointers.onTouchUp(1)
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 1) { handler.finishBackspaceHold(true) }
    }
    @Test fun holdWithoutMovingPreviewsWordEvenWithRepeatDisabled() {
        hold(); assertTrue(ptr.backspaceWordHold)
        verify(exactly = 1) { handler.beginBackspaceHold() }
        verify(exactly = 0) { handler.onPointerHold(any(),any()) }
        verify(exactly = 0) { handler.finishBackspaceHold(any()) }
    }
    @Test fun movingAcrossOtherKeysNeverActivatesTheirKeys() {
        hold(); pointers.onTouchMove(100f,200f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 0) { handler.onPointerDown(any(),any()) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
        assertTrue(pointers.isSliding())
    }
    @Test fun movingRightInLeftHalfShrinksSelection() {
        hold(); pointers.onTouchMove(100f,200f,1); pointers.onTouchMove(103f,300f,1)
        pointers.onTouchMove(130f,300f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 1) { handler.stepBackspaceHold(1) }
    }
    @Test fun releaseCommitsSelectionExactlyOnceWithoutSendingDeleteKey() {
        hold(); pointers.onTouchUp(1); pointers.onTouchUp(1)
        verify(exactly = 1) { handler.finishBackspaceHold(true) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
        assertTrue(ptrs.isEmpty())
    }
    @Test fun cancelRestoresSelectionAndStopsTimerWithoutDeleting() {
        hold(); pointers.clear()
        verify(exactly = 1) { handler.finishBackspaceHold(false) }
        verify { timers.removeMessages(ptr.selectionDeleteWhat) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
        assertTrue(ptrs.isEmpty())
    }
    @Test fun furtherTouchIsIgnoredWhileHoldOwnsKeyboard() {
        hold()
        pointers.onTouchDown(20f,20f,2,ptr.key)
        assertEquals(1,ptrs.size)
        verify(exactly = 0) { handler.onPointerDown(any(),any()) }
    }

    private fun configured(options: EditBehaviorOptions) {
        val key = ptr.value!!
        ptrs.clear()
        ptr = Pointers.Pointer(1, ptr.key, key, 900f, 100f,
            Pointers.Modifiers.EMPTY, Pointers.FLAG_P_DEFERRED_DOWN,
            testConfigSnapshot(swipe_typing_enabled = false, edit_behavior = options))
        ptrs.add(ptr)
    }
    @Test fun releaseOptionKeepsTheSelectionWithoutDeleteDispatch() {
        configured(EditBehaviorOptions(releaseDelete = false))
        hold(); pointers.onTouchUp(1)
        verify(exactly = 1) { handler.keepBackspaceHoldSelection() }
        verify(exactly = 0) { handler.finishBackspaceHold(any()) }
        verify(exactly = 0) { handler.onPointerUp(any(), any()) }
    }
    @Test fun configuredDistancesAreScaledByDensityAndKeepPauseFreeOfTimers() {
        configured(EditBehaviorOptions(pauseDp = 6, resumeDp = 24))
        every { handler.backspaceDensity() } returns 2f
        hold(); pointers.onTouchMove(100f, 200f, 1)
        pointers.onTouchMove(111f, 200f, 1); assertEquals(-1, ptr.backspaceDrag!!.direction)
        pointers.onTouchMove(112f, 200f, 1); assertEquals(0, ptr.backspaceDrag!!.direction)
        pointers.onTouchMove(159f, 200f, 1); assertEquals(0, ptr.backspaceDrag!!.direction)
        pointers.onTouchMove(160f, 200f, 1); assertEquals(1, ptr.backspaceDrag!!.direction)
    }
    @Test fun holdSelectionCanBeDisabled() {
        configured(EditBehaviorOptions(holdSelect = false))
        hold()
        assertFalse(ptr.backspaceWordHold)
        verify(exactly = 0) { handler.beginBackspaceHold() }
    }
    private fun tick() {
        Pointers::class.java.getDeclaredMethod("handleSelectionDeleteRepeat", Pointers.Pointer::class.java)
            .apply { isAccessible = true }.invoke(pointers,ptr)
    }
    @Test fun leftDragClaimsPointerImmediatelyWithoutWordPreviewOrLongPress() {
        ptr.timeoutWhat = 17
        pointers.onTouchMove(870f,100f,1)
        assertTrue(ptr.backspaceWordHold)
        assertEquals(BackspaceGesture.Mode.DRAG, ptr.backspaceMode)
        verify(exactly = 1) { handler.beginBackspaceDrag() }
        verify(exactly = 0) { handler.beginBackspaceHold() }
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify { timers.removeMessages(17) }
        hold() // A cancelled hold message must not replace the active drag.
        verify(exactly = 0) { handler.beginBackspaceHold() }
        pointers.onTouchUp(1)
        verify(exactly = 1) { handler.finishBackspaceHold(true) }
        verify(exactly = 0) { handler.onPointerDown(any(),any()) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
    }
    @Test fun immediateDragUsesDensityAndCapturedDistanceBeforeTheHoldTimeout() {
        configured(EditBehaviorOptions(resumeDp = 24))
        every { handler.backspaceDensity() } returns 2f
        // 48 px is exactly the captured 24 dp threshold at density 2.
        pointers.onTouchMove(852f,100f,1)
        verify(exactly = 1) { handler.beginBackspaceDrag() }
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
    }
    @Test fun stationaryHoldAlternatesPreviewAndDeletionWithoutCharacterRepeat() {
        every { handler.deleteBackspaceHoldWord() } returns true
        every { handler.previewPreviousBackspaceWord() } returns true
        hold()
        verify(exactly = 0) { handler.deleteBackspaceHoldWord() }
        verify { timers.sendEmptyMessageDelayed(any(), BackspaceGesture.WORD_PREVIEW_MS) }
        tick()
        assertEquals(BackspaceGesture.Mode.WORD_GAP, ptr.backspaceMode)
        verify(exactly = 1) { handler.deleteBackspaceHoldWord() }
        verify(exactly = 0) { handler.previewPreviousBackspaceWord() }
        verify { timers.sendEmptyMessageDelayed(any(), BackspaceGesture.WORD_GAP_MS) }
        tick()
        assertEquals(BackspaceGesture.Mode.WORD_PREVIEW, ptr.backspaceMode)
        verify(exactly = 1) { handler.previewPreviousBackspaceWord() }
        tick()
        verify(exactly = 2) { handler.deleteBackspaceHoldWord() }
        verify(exactly = 0) { handler.onPointerHold(any(),any()) }
    }
    @Test fun releaseImmediatelyAfterTimedDeletionStopsBeforeSelectingAnotherWord() {
        every { handler.deleteBackspaceHoldWord() } returns true
        hold(); tick()
        val message = ptr.selectionDeleteWhat
        pointers.onTouchUp(1)
        val stale = org.objenesis.ObjenesisStd().newInstance(android.os.Message::class.java)
        stale.what = message
        assertFalse(pointers.handleMessage(stale))
        verify(exactly = 1) { handler.deleteBackspaceHoldWord() }
        verify(exactly = 0) { handler.previewPreviousBackspaceWord() }
        verify(exactly = 1) { handler.finishBackspaceHold(true) }
    }
    @Test fun draggingDuringWordPreviewCancelsAutomaticWordCycleEvenAfterBraking() {
        hold()
        val oldMessage = ptr.selectionDeleteWhat
        pointers.onTouchMove(100f,100f,1)
        assertEquals(BackspaceGesture.Mode.DRAG, ptr.backspaceMode)
        val stale = org.objenesis.ObjenesisStd().newInstance(android.os.Message::class.java)
        stale.what = oldMessage
        assertFalse(pointers.handleMessage(stale))
        pointers.onTouchMove(103f,100f,1)
        repeat(3) { tick() }
        verify(exactly = 0) { handler.deleteBackspaceHoldWord() }
        verify(exactly = 0) { handler.previewPreviousBackspaceWord() }
        pointers.onTouchUp(1)
        verify(exactly = 1) { handler.finishBackspaceHold(true) }
    }
    @Test fun exhaustedWordCycleStopsInsteadOfFallingBackToDeleteKey() {
        every { handler.deleteBackspaceHoldWord() } returns false
        hold(); tick()
        assertEquals(-1, ptr.selectionDeleteWhat)
        verify(exactly = 0) { handler.onPointerDown(any(),any()) }
        verify(exactly = 0) { handler.onPointerHold(any(),any()) }
        verify(exactly = 0) { handler.previewPreviousBackspaceWord() }
    }
    @Test fun brakeStopsPendingRepeatUntilFurtherRightMotion() {
        hold(); pointers.onTouchMove(100f,200f,1)
        val activeTimer = ptr.selectionDeleteWhat
        pointers.onTouchMove(103f,200f,1)
        assertEquals(0,ptr.backspaceDrag!!.direction)
        verify { timers.removeMessages(activeTimer) }
        repeat(5) { tick() }
        pointers.onTouchMove(110f,200f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 0) { handler.stepBackspaceHold(1) }
        // Initial hold timer and first active step only; the pause adds no timer.
        verify(exactly = 2) { timers.sendEmptyMessageDelayed(any(),any()) }
        pointers.onTouchMove(118f,200f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(1) }
        tick()
        verify(exactly = 2) { handler.stepBackspaceHold(1) }
    }
    @Test fun brakeCanResumeLeftAndReleaseStillCommitsSelection() {
        hold(); pointers.onTouchMove(100f,200f,1)
        pointers.onTouchMove(103f,200f,1); tick()
        pointers.onTouchMove(88f,200f,1)
        verify(exactly = 2) { handler.stepBackspaceHold(-1) }
        pointers.onTouchMove(91f,200f,1); tick()
        pointers.onTouchUp(1)
        verify(exactly = 2) { handler.stepBackspaceHold(-1) }
        verify(exactly = 0) { handler.stepBackspaceHold(1) }
        verify(exactly = 1) { handler.finishBackspaceHold(true) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
    }
}
