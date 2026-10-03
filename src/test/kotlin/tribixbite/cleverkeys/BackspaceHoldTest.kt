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
            testConfigSnapshot(swipe_typing_enabled = false))
        field("_handler",host); field("_ptrs",arrayListOf(ptr))
        field("_longpress_handler",mockk<Handler>(relaxed = true))
        Pointers::class.java.getDeclaredMethod("handleLongPress",Pointers.Pointer::class.java)
            .apply { isAccessible = true }.invoke(pointers,ptr)
        reportedSelection = 11 to 11
        acknowledgeSelection()
        pointers.onTouchMove(100f,200f,1)
        assertEquals(" mleko ",conn.getSelectedText(0))
        acknowledgeSelection()
        pointers.onTouchMove(103f,200f,1)
        assertEquals(" mleko ",conn.getSelectedText(0))
        pointers.onTouchMove(130f,200f,1)
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
}
