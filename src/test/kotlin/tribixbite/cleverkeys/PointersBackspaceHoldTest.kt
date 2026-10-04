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
        every { handler.stepBackspaceHold(any()) } returns true
        every { handler.backspaceKeyboardWidth() } returns 1000f
        val key = KeyValue.keyeventKey(0xE003, KeyEvent.KEYCODE_DEL, 0)
        ptr = Pointers.Pointer(1, KeyboardData.Key.EMPTY.withKeyValue(0,key), key, 900f, 100f,
            Pointers.Modifiers.EMPTY, Pointers.FLAG_P_DEFERRED_DOWN,
            testConfigSnapshot(swipe_typing_enabled = false, keyrepeat_enabled = false, slider_speed_max = 1f))
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
        hold(); pointers.onTouchMove(870f,200f,1); pointers.onTouchMove(873f,200f,1)
        pointers.onTouchMove(900f,200f,1)
        pointers.onTouchUp(1)
        assertTrue(trace.any { it.contains("hold started=true") })
        assertEquals(1,trace.count { it.contains("move observed") })
        assertTrue(trace.any { it.contains("move direction=-1") })
        assertEquals(2, trace.count { it.contains("move direction=") })
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
        hold(); pointers.onTouchMove(870f,200f,1); pointers.onTouchUp(1)
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
        hold(); pointers.onTouchMove(870f,200f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 0) { handler.onPointerDown(any(),any()) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
        assertTrue(pointers.isSliding())
    }
    @Test fun movingRightInLeftHalfShrinksSelection() {
        hold(); pointers.onTouchMove(870f,200f,1); pointers.onTouchMove(873f,300f,1)
        pointers.onTouchMove(900f,300f,1)
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

    private fun configured(options: EditBehaviorOptions, stepPx: Float = 30f) {
        val key = ptr.value!!
        ptrs.clear()
        ptr = Pointers.Pointer(1, ptr.key, key, 900f, 100f,
            Pointers.Modifiers.EMPTY, Pointers.FLAG_P_DEFERRED_DOWN,
            testConfigSnapshot(swipe_typing_enabled = false, edit_behavior = options, slide_step_px = stepPx, slider_speed_max = 1f))
        ptrs.add(ptr)
    }
    @Test fun releaseOptionKeepsTheSelectionWithoutDeleteDispatch() {
        configured(EditBehaviorOptions(releaseDelete = false))
        hold(); pointers.onTouchUp(1)
        verify(exactly = 1) { handler.keepBackspaceHoldSelection() }
        verify(exactly = 0) { handler.finishBackspaceHold(any()) }
        verify(exactly = 0) { handler.onPointerUp(any(), any()) }
    }
    @Test fun capturedSpaceSensitivitySetsActivationDistance() {
        configured(EditBehaviorOptions(), stepPx = 60f)
        every { handler.backspaceDensity() } returns 2f
        pointers.onTouchMove(841f,100f,1)
        verify(exactly = 0) { handler.beginBackspaceDrag() }
        pointers.onTouchMove(840f,100f,1)
        verify(exactly = 1) { handler.beginBackspaceDrag() }
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        assertEquals(-1,ptr.selectionDeleteWhat)
        verify(exactly = 0) { timers.sendEmptyMessageDelayed(any(),any()) }
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
    @Test fun activationUsesSpacePixelsWithoutScalingThemAgainByDensity() {
        configured(EditBehaviorOptions(), stepPx = 48f)
        every { handler.backspaceDensity() } returns 3f
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
    @Test fun draggingDuringWordPreviewCancelsAutomaticWordCycleEvenWhenStopped() {
        hold()
        val oldMessage = ptr.selectionDeleteWhat
        pointers.onTouchMove(870f,100f,1)
        assertEquals(BackspaceGesture.Mode.DRAG, ptr.backspaceMode)
        val stale = org.objenesis.ObjenesisStd().newInstance(android.os.Message::class.java)
        stale.what = oldMessage
        assertFalse(pointers.handleMessage(stale))
        pointers.onTouchMove(873f,100f,1)
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
    @Test fun stationaryDragHasNoRepeatAndReversesWithFingerMovement() {
        hold(); pointers.onTouchMove(870f,200f,1)
        repeat(5) { tick(); pointers.onTouchMove(870f,200f,1) }
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 0) { handler.stepBackspaceHold(1) }
        assertEquals(-1,ptr.selectionDeleteWhat)
        pointers.onTouchMove(900f,200f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(1) }
        verify(exactly = 1) { timers.sendEmptyMessageDelayed(any(),any()) }
    }
    @Test fun reversibleMotionStillCommitsExactlyOnceOnRelease() {
        hold(); pointers.onTouchMove(870f,200f,1)
        pointers.onTouchMove(900f,200f,1)
        pointers.onTouchMove(840f,200f,1)
        pointers.onTouchUp(1)
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 1) { handler.stepBackspaceHold(-2) }
        verify(exactly = 1) { handler.stepBackspaceHold(1) }
        verify(exactly = 1) { handler.finishBackspaceHold(true) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
    }
    @Test fun oneFingerMoveCanSelectSeveralCharactersWithNoDragTimer() {
        pointers.onTouchMove(810f,100f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(-3) }
        verify(exactly = 0) { timers.sendEmptyMessageDelayed(any(),any()) }
        repeat(5) { tick(); pointers.onTouchMove(810f,100f,1) }
        verify(exactly = 1) { handler.stepBackspaceHold(-3) }
    }
    @Test fun rejectedEditorStepStopsTheRestOfThatMove() {
        every { handler.stepBackspaceHold(any()) } returns false
        pointers.onTouchMove(600f,100f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(-10) }
        verify(exactly = 0) { handler.onPointerUp(any(),any()) }
    }
    @Test fun movingDuringWordGapCancelsNextPreviewPermanently() {
        every { handler.deleteBackspaceHoldWord() } returns true
        hold(); tick()
        val message = ptr.selectionDeleteWhat
        pointers.onTouchMove(870f,100f,1)
        val stale = org.objenesis.ObjenesisStd().newInstance(android.os.Message::class.java)
        stale.what = message
        assertFalse(pointers.handleMessage(stale))
        repeat(5) { tick() }
        verify(exactly = 1) { handler.deleteBackspaceHoldWord() }
        verify(exactly = 0) { handler.previewPreviousBackspaceWord() }
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
    }
    @Test fun verticalMovementDuringDragDoesNotAccelerateHorizontalSelection() {
        pointers.onTouchMove(870f,100f,1)
        pointers.onTouchMove(870f,500f,1)
        verify(exactly = 1) { handler.stepBackspaceHold(-1) }
        verify(exactly = 0) { handler.stepBackspaceHold(1) }
    }
    @Test fun diagonalFlickAndInitialRightwardMotionDoNotClaimBackspace() {
        pointers.onTouchMove(870f,70f,1)
        pointers.onTouchMove(930f,100f,1)
        verify(exactly = 0) { handler.beginBackspaceDrag() }
    }
    @Test fun actualSpaceSliderAndBackspaceConsumeTheSameMovementCounts() {
        val emitted = mutableListOf<Int>()
        every { handler.onPointerHold(any(),any()) } answers {
            emitted.add(firstArg<KeyValue>().getSliderRepeat()); Unit
        }
        val slide = pointers.Sliding(900f,100f,1,0,KeyValue.Slider.Cursor_right,ptr.snap)
        for (x in listOf(810f,840f,840f,900f)) {
            slide.onTouchMove(ptr,x,100f)
            pointers.onTouchMove(x,100f,1)
        }
        assertEquals(listOf(-3,1,2),emitted)
        verifyOrder {
            handler.stepBackspaceHold(-3)
            handler.stepBackspaceHold(1)
            handler.stepBackspaceHold(2)
        }
    }
}
