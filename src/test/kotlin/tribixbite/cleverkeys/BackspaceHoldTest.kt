package tribixbite.cleverkeys

import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.*
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Drives the real editor path; checks actual retained text, stale edits and cancellation. */
class BackspaceHoldTest {
    private lateinit var recv: KeyEventHandler.IReceiver
    private lateinit var conn: InputConnection
    private lateinit var handler: KeyEventHandler
    private lateinit var info: EditorInfo
    private var text = "olej mleko "
    private var a = text.length
    private var b = a
    private var reportedSelection: Pair<Int,Int>? = null

    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        recv = mockk(relaxed = true); conn = mockk(relaxed = true)
        info = mockk<EditorInfo>(relaxed = true).also {
            it.inputType = InputType.TYPE_CLASS_TEXT
            it.packageName = "example.editor"
        }
        every { recv.getHandler() } returns mockk<Handler>(relaxed = true)
        every { recv.getCurrentEditorInfo() } returns info
        every { recv.getCurrentInputConnection() } returns conn
        requestField().set(null, mockk<ExtractedTextRequest>(relaxed = true))
        val et = mockk<ExtractedText>(relaxed = true)
        every { conn.finishComposingText() } returns true
        every { conn.getExtractedText(any(), any()) } answers {
            et.startOffset = 0
            et.selectionStart = reportedSelection?.first ?: a
            et.selectionEnd = reportedSelection?.second ?: b
            et
        }
        every { conn.getTextBeforeCursor(any(), any()) } answers {
            text.take(minOf(a, b)).takeLast(firstArg<Int>())
        }
        every { conn.getSelectedText(0) } answers { text.substring(minOf(a,b), maxOf(a,b)) }
        every { conn.setSelection(any(), any()) } answers { a = firstArg(); b = secondArg(); true }
        every { conn.commitText(any(), any()) } answers {
            val start = minOf(a,b); val end = maxOf(a,b)
            val replacement = firstArg<CharSequence>().toString()
            text = text.take(start) + replacement + text.drop(end)
            a = start + replacement.length; b = a; true
        }
        handler = spyk(KeyEventHandler(recv))
    }
    @After fun teardown() { requestField().set(null, null); unmockkStatic(Log::class) }
    private fun requestField() = KeyEventHandler::class.java.getDeclaredField("moveCursorReq")
        .apply { isAccessible = true }
    private fun caret(at: Int) { a = at; b = at }

    private fun acknowledgeSelection() {
        handler.selection_updated(11, a, 11, b)
    }

    @Test fun diagnosticsExplainReleaseBlockedByChangedConnectionWithoutEditorText() {
        val trace = mutableListOf<String>()
        handler.backspaceTrace = { trace.add(it) }
        assertTrue(handler.beginBackspaceHold())
        every { recv.getCurrentInputConnection() } returns mockk(relaxed = true)
        handler.finishBackspaceHold(true)
        assertTrue(trace.any { it.contains("begin accepted") })
        assertTrue(trace.any { it.contains("validation blocked: connection changed") })
        assertTrue(trace.any { it.contains("release blocked: validation") })
        assertFalse(trace.any { it.contains("olej") || it.contains("mleko") })
        verify(exactly = 0) { conn.commitText(any(),any()) }
    }

    @Test fun diagnosticsSeparateSelectionMismatchFromSelectedTextMismatch() {
        val trace = mutableListOf<String>()
        handler.backspaceTrace = { trace.add(it) }
        assertTrue(handler.beginBackspaceHold())
        reportedSelection = 2 to 2
        assertFalse(handler.stepBackspaceHold(-1))
        assertTrue(trace.any { it.contains("validation blocked: extracted=") })
        reportedSelection = null
        text = "olej sokxx "
        handler.finishBackspaceHold(true)
        assertTrue(trace.any { it.contains("validation blocked: selected length=") })
        assertFalse(trace.any { it.contains("sokxx") || it.contains("mleko") })
        verify(exactly = 0) { conn.commitText(any(),any()) }
    }

    @Test fun diagnosticSinkFailureDoesNotPreventReleaseDeletion() {
        handler.backspaceTrace = { throw IllegalStateException("sink failure") }
        assertTrue(handler.beginBackspaceHold())
        handler.finishBackspaceHold(true)
        assertEquals("olej ",text)
        verify(exactly = 1) { conn.commitText("",1) }
    }

    @Test fun diagnosticBudgetBoundsRepeatedStepsAndRetainsReleaseResult() {
        val trace = mutableListOf<String>()
        handler.backspaceTrace = { trace.add(it) }
        assertTrue(handler.beginBackspaceHold())
        repeat(100) { handler.stepBackspaceHold(if (it % 2 == 0) -1 else 1) }
        handler.finishBackspaceHold(true)
        assertTrue(trace.size <= 28)
        assertTrue(trace.last().contains("release commitText accepted=true"))
        assertEquals("olej ",text)
    }

    @Test fun laggingExtractionDoesNotDiscardGestureBeforeEditorCatchesUp() {
        assertTrue(handler.beginBackspaceHold())
        reportedSelection = 11 to 11
        assertFalse(handler.stepBackspaceHold(-1))
        reportedSelection = null
        assertTrue(handler.stepBackspaceHold(-1))
        handler.finishBackspaceHold(true)
        assertEquals("olej", text)
    }

    @Test fun acknowledgedPreviewCanBeReleasedWhileExtractionStillShowsOldCaret() {
        assertTrue(handler.beginBackspaceHold())
        reportedSelection = 11 to 11
        acknowledgeSelection()
        handler.finishBackspaceHold(true)
        assertEquals("olej ", text)
        verify(exactly = 1) { conn.commitText("",1) }
    }

    @Test fun callbacksPermitExtensionAndReversalWithLaggingExtraction() {
        assertTrue(handler.beginBackspaceHold())
        reportedSelection = 11 to 11
        acknowledgeSelection()
        assertTrue(handler.stepBackspaceHold(-1))
        acknowledgeSelection()
        assertTrue(handler.stepBackspaceHold(1))
        acknowledgeSelection()
        handler.finishBackspaceHold(true)
        assertEquals("olej ", text)
    }

    @Test fun callbackCannotAuthorizeDeletionAfterAnUnrelatedLiveCaretMove() {
        assertTrue(handler.beginBackspaceHold())
        acknowledgeSelection()
        reportedSelection = 2 to 2
        handler.finishBackspaceHold(true)
        assertEquals("olej mleko ", text)
        verify(exactly = 0) { conn.commitText(any(),any()) }
    }

    @Test fun callbackCannotAuthorizeDeletionWhenTheSelectedTextChanged() {
        assertTrue(handler.beginBackspaceHold())
        acknowledgeSelection()
        reportedSelection = 11 to 11
        text = "olej sokxx "
        handler.finishBackspaceHold(true)
        assertEquals("olej sokxx ", text)
        verify(exactly = 0) { conn.commitText(any(),any()) }
    }

    @Test fun synchronousSelectionAcknowledgementIsNotLostAtActivation() {
        reportedSelection = 11 to 11
        every { conn.setSelection(any(),any()) } answers {
            a = firstArg(); b = secondArg(); acknowledgeSelection(); true
        }
        assertTrue(handler.beginBackspaceHold())
        handler.finishBackspaceHold(true)
        assertEquals("olej ", text)
    }

    @Test fun pointerReleaseDeletesAfterDragAndReversalWithLaggingEditorReads() {
        val host = mockk<Pointers.IPointerEventHandler>(relaxed = true)
        every { host.beginBackspaceHold() } answers { handler.beginBackspaceHold() }
        every { host.stepBackspaceHold(any()) } answers { handler.stepBackspaceHold(firstArg()) }
        every { host.finishBackspaceHold(any()) } answers { handler.finishBackspaceHold(firstArg()) }
        every { host.backspaceKeyboardWidth() } returns 1000f
        val pointers = org.objenesis.ObjenesisStd().newInstance(Pointers::class.java)
        fun field(name: String, value: Any) {
            Pointers::class.java.getDeclaredField(name).apply { isAccessible = true }.set(pointers,value)
        }
        val key = KeyValue.keyeventKey(0xE003,KeyEvent.KEYCODE_DEL,0)
        val ptr = Pointers.Pointer(1, KeyboardData.Key.EMPTY.withKeyValue(0,key), key,900f,100f,
            Pointers.Modifiers.EMPTY,Pointers.FLAG_P_DEFERRED_DOWN,
            testConfigSnapshot(swipe_typing_enabled = false, slider_speed_max = 1f))
        field("_handler",host); field("_ptrs",arrayListOf(ptr))
        field("_longpress_handler",mockk<Handler>(relaxed = true))
        Pointers::class.java.getDeclaredMethod("handleLongPress",Pointers.Pointer::class.java)
            .apply { isAccessible = true }.invoke(pointers,ptr)
        reportedSelection = 11 to 11
        acknowledgeSelection()
        pointers.onTouchMove(870f,200f,1)
        assertEquals(" mleko ",conn.getSelectedText(0))
        acknowledgeSelection()
        pointers.onTouchMove(873f,200f,1)
        assertEquals(" mleko ",conn.getSelectedText(0))
        pointers.onTouchMove(900f,200f,1)
        assertEquals("mleko ",conn.getSelectedText(0))
        acknowledgeSelection()
        pointers.onTouchUp(1)
        assertEquals("olej ",text)
        verify(exactly = 0) { host.onPointerUp(any(),any()) }
    }

    @Test fun holdPreviewsWordAndDeletesOnlyOnReleasePreservingLeadingSpace() {
        assertTrue(handler.beginBackspaceHold())
        assertEquals("olej mleko ", text)
        assertEquals("mleko ", conn.getSelectedText(0))
        verify(exactly = 0) { conn.commitText(any(), any()) }
        handler.finishBackspaceHold(true)
        assertEquals("olej ", text); assertEquals(5, a); assertEquals(a,b)
        verify(exactly = 1) { recv.handle_backspace() }
    }
    @Test fun immediateDragStartsAtCaretAndSelectsOnlyTheNextCodePoint() {
        text = "olej łódź😀"; caret(text.length)
        assertTrue(handler.beginBackspaceDrag())
        assertEquals("", conn.getSelectedText(0))
        assertTrue(handler.stepBackspaceHold(-1))
        assertEquals("😀", conn.getSelectedText(0))
        handler.finishBackspaceHold(true)
        assertEquals("olej łódź", text)
    }
    @Test fun timedWordsPreserveLeadingSpacesAndReleaseDuringGapDoesNotDeleteAgain() {
        text = "olej mleko karton "; caret(text.length)
        assertTrue(handler.beginBackspaceHold())
        assertEquals("karton ", conn.getSelectedText(0))
        assertTrue(handler.deleteBackspaceHoldWord())
        assertEquals("olej mleko ", text)
        assertEquals(a,b)
        assertTrue(handler.previewPreviousBackspaceWord())
        assertEquals("mleko ", conn.getSelectedText(0))
        assertTrue(handler.deleteBackspaceHoldWord())
        assertEquals("olej ", text)
        handler.finishBackspaceHold(true)
        assertEquals("olej ", text)
        assertFalse(handler.previewPreviousBackspaceWord())
        verify(exactly = 2) { conn.commitText("",1) }
    }
    @Test fun repeatedWordsStopAtStartAndCannotDeleteNewlyInsertedText() {
        text = "łódź"; caret(text.length)
        assertTrue(handler.beginBackspaceHold())
        assertTrue(handler.deleteBackspaceHoldWord())
        assertEquals("",text)
        assertFalse(handler.previewPreviousBackspaceWord())
        assertFalse(handler.deleteBackspaceHoldWord())
        handler.finishBackspaceHold(true)
        verify(exactly = 1) { conn.commitText("",1) }
    }
    @Test fun repeatingWordCycleCannotRearmAfterConnectionOrTextChanges() {
        assertTrue(handler.beginBackspaceHold())
        assertTrue(handler.deleteBackspaceHoldWord())
        text = "sokx " // Same caret/length, but preceding text changed externally.
        assertFalse(handler.previewPreviousBackspaceWord())
        every { recv.getCurrentInputConnection() } returns mockk(relaxed = true)
        assertFalse(handler.previewPreviousBackspaceWord())
        handler.finishBackspaceHold(true)
        assertEquals("sokx ",text)
        verify(exactly = 1) { conn.commitText("",1) }
    }
    @Test fun rejectedTimedDeletionNeverArmsTheNextWord() {
        assertTrue(handler.beginBackspaceHold())
        every { conn.commitText(any(),any()) } returns false
        assertFalse(handler.deleteBackspaceHoldWord())
        assertFalse(handler.previewPreviousBackspaceWord())
        handler.finishBackspaceHold(true)
        assertEquals("olej mleko ",text)
        verify(exactly = 1) { conn.commitText("",1) }
    }
    @Test fun wordCycleCapturesSynchronousCallbacksDespiteLaggingExtraction() {
        every { conn.commitText(any(), any()) } answers {
            val start = minOf(a,b); val end = maxOf(a,b)
            text = text.take(start) + text.drop(end); caret(start)
            acknowledgeSelection(); true
        }
        every { conn.setSelection(any(),any()) } answers {
            a = firstArg(); b = secondArg(); acknowledgeSelection(); true
        }
        assertTrue(handler.beginBackspaceHold())
        assertTrue(handler.deleteBackspaceHoldWord())
        reportedSelection = 11 to 11 // Unrelated old caret must not authorize re-arming.
        assertFalse(handler.previewPreviousBackspaceWord())
        reportedSelection = null
        assertTrue(handler.previewPreviousBackspaceWord())
        assertTrue(handler.deleteBackspaceHoldWord())
        assertEquals("",text)
    }
    @Test fun realPointerTimerDeletesOneWordAtATimeAndReleaseDuringGapKeepsThePreviousWord() {
        text = "olej mleko karton "; caret(text.length)
        val host = mockk<Pointers.IPointerEventHandler>(relaxed = true)
        every { host.beginBackspaceHold() } answers { handler.beginBackspaceHold() }
        every { host.beginBackspaceDrag() } answers { handler.beginBackspaceDrag() }
        every { host.deleteBackspaceHoldWord() } answers { handler.deleteBackspaceHoldWord() }
        every { host.previewPreviousBackspaceWord() } answers { handler.previewPreviousBackspaceWord() }
        every { host.finishBackspaceHold(any()) } answers { handler.finishBackspaceHold(firstArg()) }
        val pointers = org.objenesis.ObjenesisStd().newInstance(Pointers::class.java)
        val key = KeyValue.keyeventKey(0xE003,KeyEvent.KEYCODE_DEL,0)
        val ptr = Pointers.Pointer(1, KeyboardData.Key.EMPTY.withKeyValue(0,key), key,900f,100f,
            Pointers.Modifiers.EMPTY,Pointers.FLAG_P_DEFERRED_DOWN,
            testConfigSnapshot(swipe_typing_enabled = false))
        fun field(name: String, value: Any) {
            Pointers::class.java.getDeclaredField(name).apply { isAccessible = true }.set(pointers,value)
        }
        field("_handler",host); field("_ptrs",arrayListOf(ptr))
        field("_longpress_handler",mockk<Handler>(relaxed = true))
        fun invoke(name: String) {
            Pointers::class.java.getDeclaredMethod(name,Pointers.Pointer::class.java)
                .apply { isAccessible = true }.invoke(pointers,ptr)
        }
        invoke("handleLongPress")
        assertEquals("karton ",conn.getSelectedText(0))
        invoke("handleSelectionDeleteRepeat")
        assertEquals("olej mleko ",text)
        invoke("handleSelectionDeleteRepeat")
        assertEquals("mleko ",conn.getSelectedText(0))
        invoke("handleSelectionDeleteRepeat")
        assertEquals("olej ",text)
        pointers.onTouchUp(1)
        assertEquals("olej ",text)
        verify(exactly = 2) { conn.commitText("",1) }
        verify(exactly = 0) { host.onPointerUp(any(),any()) }
    }
    @Test fun cancelRestoresCaretWithoutDeleting() {
        assertTrue(handler.beginBackspaceHold()); assertTrue(handler.stepBackspaceHold(-1))
        handler.finishBackspaceHold(false)
        assertEquals("olej mleko ", text); assertEquals(text.length,a); assertEquals(a,b)
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun leftExtendsAndRightShrinksWithoutCrossingOriginalAnchor() {
        assertTrue(handler.beginBackspaceHold()); assertTrue(handler.stepBackspaceHold(-1))
        assertEquals(" mleko ", conn.getSelectedText(0))
        repeat(20) { handler.stepBackspaceHold(1) }
        assertEquals(a,b); assertEquals(text.length,a)
        handler.finishBackspaceHold(true)
        assertEquals("olej mleko ", text)
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun externalCaretMovementInvalidatesPendingDeletion() {
        assertTrue(handler.beginBackspaceHold()); caret(2)
        handler.finishBackspaceHold(true)
        assertEquals("olej mleko ", text)
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun externalTextReplacementWithSameSelectionInvalidatesDeletion() {
        assertTrue(handler.beginBackspaceHold()); text = "olej sokxx "
        handler.finishBackspaceHold(true)
        assertEquals("olej sokxx ", text)
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun changingConnectionInvalidatesPendingDeletion() {
        assertTrue(handler.beginBackspaceHold())
        every { recv.getCurrentInputConnection() } returns mockk(relaxed = true)
        handler.finishBackspaceHold(true)
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun rejectedSelectionDoesNotArmWordDeletion() {
        every { conn.setSelection(any(), any()) } returns false
        assertFalse(handler.beginBackspaceHold()); handler.finishBackspaceHold(true)
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun rejectedCompositionFinishCannotArmReplacementOfTheWrongRegion() {
        every { conn.finishComposingText() } returns false
        assertFalse(handler.beginBackspaceHold()); handler.finishBackspaceHold(true)
        verify(exactly = 0) { conn.setSelection(any(),any()) }
        verify(exactly = 0) { conn.commitText(any(),any()) }
    }
    @Test fun emptyBufferHoldDoesNotDeleteAnything() {
        text = ""; caret(0)
        assertTrue(handler.beginBackspaceHold()); handler.finishBackspaceHold(true)
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun firstWordCanBeDeletedAndRepeatReleaseIsHarmless() {
        text = "łódź"; caret(text.length)
        assertTrue(handler.beginBackspaceHold()); handler.finishBackspaceHold(true)
        handler.finishBackspaceHold(true)
        assertEquals("",text); verify(exactly = 1) { conn.commitText("",1) }
    }
    @Test fun privateAndInlineModesDoNotReadApplicationTextForHold() {
        info.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertFalse(handler.beginBackspaceHold())
        info.inputType = InputType.TYPE_CLASS_TEXT
        every { recv.isClipboardEditMode() } returns true
        assertFalse(handler.beginBackspaceHold())
        verify(exactly = 0) { conn.getTextBeforeCursor(any(), any()) }
    }
    private fun withTapMode(mode: Int, block: () -> Unit) {
        mockkObject(Config.Companion)
        try {
            val config = mockk<Config>(relaxed = true)
            config.edit_behavior = EditBehaviorOptions(tapMode = mode)
            every { Config.globalConfigOrNull() } returns config
            every { Config.globalConfig() } returns config
            block()
        } finally { unmockkObject(Config.Companion) }
    }
    private fun tapBackspace() = handler.key_up(
        KeyValue.keyeventKey(0xE003, KeyEvent.KEYCODE_DEL, 0), Pointers.Modifiers.EMPTY, false)
    @Test fun explicitlyEnabledSwipeUndoDeletesOnlyTheVerifiedLastToken() = withTapMode(2) {
        every { recv.getLastAutoInsertedWord() } returns "mleko"
        every { recv.getLastAutocorrectOriginalWord() } returns null
        every { conn.deleteSurroundingText(any(), any()) } answers {
            text = text.dropLast(firstArg<Int>()); caret(text.length); true
        }
        tapBackspace()
        assertEquals("olej ", text)
        verify(exactly = 0) { handler.send_key_down_up(KeyEvent.KEYCODE_DEL) }
    }
    @Test fun explicitlyEnabledAutocorrectUndoUsesOneCommitAndPreservesSpacing() = withTapMode(1) {
        every { recv.getLastAutoInsertedWord() } returns "mleko"
        every { recv.getLastAutocorrectOriginalWord() } returns "mlekoo"
        tapBackspace()
        assertEquals("olej mlekoo ", text)
        verify(exactly = 1) { conn.commitText("mlekoo ", 1) }
        verify(exactly = 0) { conn.deleteSurroundingText(any(), any()) }
        verify(exactly = 0) { handler.send_key_down_up(KeyEvent.KEYCODE_DEL) }
    }
    @Test fun swipeUndoCannotEraseAMatchingSuffixInsideAnotherWord() = withTapMode(2) {
        text = "olej niemleko "; caret(text.length)
        every { recv.getLastAutoInsertedWord() } returns "mleko"
        every { recv.getLastAutocorrectOriginalWord() } returns null
        every { handler.send_key_down_up(KeyEvent.KEYCODE_DEL) } answers {
            text = text.dropLast(1); caret(text.length)
        }
        tapBackspace(); assertEquals("olej niemleko", text)
        verify(exactly = 0) { conn.deleteSurroundingText(any(), any()) }
    }
    @Test fun keepingThePreviewDoesNotDeleteOrCollapseTheSelection() {
        assertTrue(handler.beginBackspaceHold())
        val selected = a to b
        handler.keepBackspaceHoldSelection()
        assertEquals("olej mleko ", text)
        assertEquals(selected, a to b)
        handler.finishBackspaceHold(true)
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun tapDeletesOneSpaceThenOneCharacterAfterSwipeOrAutocorrection() {
        every { recv.getLastAutoInsertedWord() } returns "mleko"
        every { recv.getLastAutocorrectOriginalWord() } returns "mlekoo"
        every { handler.send_key_down_up(KeyEvent.KEYCODE_DEL) } answers {
            text = text.dropLast(1); caret(text.length)
        }
        val key = KeyValue.keyeventKey(0xE003, KeyEvent.KEYCODE_DEL, 0)
        handler.key_up(key, Pointers.Modifiers.EMPTY, false)
        assertEquals("olej mleko",text)
        handler.key_up(key, Pointers.Modifiers.EMPTY, false)
        assertEquals("olej mlek",text)
        verify(exactly = 0) { conn.deleteSurroundingText(any(),any()) }
        verify(exactly = 2) { recv.clearSwipeUndoState() }
        verify(exactly = 2) { recv.clearAutocorrectUndoState() }
    }
    @Test fun batchedDragUpdatesSelectionOnceAndPreservesUnicodeOnRelease() {
        text = "a🙂b🙂c"; caret(text.length)
        assertTrue(handler.beginBackspaceDrag())
        assertTrue(handler.stepBackspaceHold(-4))
        assertEquals("🙂b🙂c",conn.getSelectedText(0))
        assertTrue(handler.stepBackspaceHold(2))
        assertEquals("🙂c",conn.getSelectedText(0))
        verify(exactly = 3) { conn.setSelection(any(),any()) } // Activation + two touch events.
        handler.finishBackspaceHold(true)
        assertEquals("a🙂b",text)
    }
    @Test fun changedTextBlocksABatchedDragWithoutPartialSelectionOrDeletion() {
        assertTrue(handler.beginBackspaceDrag())
        assertTrue(handler.stepBackspaceHold(-4))
        val selection = a to b
        text = "olej soki  "
        assertFalse(handler.stepBackspaceHold(-100))
        assertEquals(selection,a to b)
        handler.finishBackspaceHold(true)
        assertEquals("olej soki  ",text)
        verify(exactly = 0) { conn.commitText(any(),any()) }
    }
    /**
     * Model the SimpleX-style state bridge: it stores ascending selection endpoints
     * and recreates its input connection when the view receives a reversed range.
     * This is a compatibility regression model, not a real SimpleX device test.
     */
    private fun normalizingEditor(): MutableList<Pair<Int, Int>> {
        val requests = mutableListOf<Pair<Int, Int>>()
        every { conn.setSelection(any(), any()) } answers {
            a = firstArg(); b = secondArg()
            requests.add(a to b)
            acknowledgeSelection()
            if (a > b) {
                // Recomposition can rebuild the field after reversed endpoints diverge
                // from its normalized state. Future validation must reject that connection.
                every { recv.getCurrentInputConnection() } returns mockk(relaxed = true)
            }
            true
        }
        return requests
    }

    @Test fun orderedWordPreviewAndDragDoNotRestartANormalizingEditor() {
        val requests = normalizingEditor()
        assertTrue(handler.beginBackspaceHold())
        assertTrue(handler.stepBackspaceHold(-1))
        assertEquals(" mleko ", conn.getSelectedText(0))
        assertTrue(handler.stepBackspaceHold(-2))
        assertTrue(handler.stepBackspaceHold(3))
        assertEquals("mleko ", conn.getSelectedText(0))
        handler.finishBackspaceHold(true)
        assertEquals("olej ", text)
        assertTrue(requests.all { it.first <= it.second })
        verify(exactly = 1) { conn.commitText("", 1) }
    }

    @Test fun orderedDirectDragPassesSpacesBeforeAndAfterWordsAndReverses() {
        val requests = normalizingEditor()
        assertTrue(handler.beginBackspaceDrag())
        assertTrue(handler.stepBackspaceHold(-1))
        assertEquals(" ", conn.getSelectedText(0))
        assertTrue(handler.stepBackspaceHold(-5))
        assertEquals("mleko ", conn.getSelectedText(0))
        assertTrue(handler.stepBackspaceHold(-1))
        assertEquals(" mleko ", conn.getSelectedText(0))
        assertTrue(handler.stepBackspaceHold(1))
        assertEquals("mleko ", conn.getSelectedText(0))
        handler.finishBackspaceHold(true)
        assertEquals("olej ", text)
        assertTrue(requests.all { it.first <= it.second })
        verify(exactly = 1) { conn.commitText("", 1) }
    }

    @Test fun orderedWordCycleKeepsTheConnectionThroughSuccessivePreviews() {
        text = "olej mleko karton "; caret(text.length)
        val requests = normalizingEditor()
        assertTrue(handler.beginBackspaceHold())
        assertTrue(handler.deleteBackspaceHoldWord())
        assertEquals("olej mleko ", text)
        assertTrue(handler.previewPreviousBackspaceWord())
        assertTrue(handler.deleteBackspaceHoldWord())
        assertEquals("olej ", text)
        assertTrue(handler.previewPreviousBackspaceWord())
        assertTrue(handler.deleteBackspaceHoldWord())
        assertEquals("", text)
        handler.finishBackspaceHold(true)
        assertTrue(requests.all { it.first <= it.second })
        verify(exactly = 3) { conn.commitText("", 1) }
    }

}
