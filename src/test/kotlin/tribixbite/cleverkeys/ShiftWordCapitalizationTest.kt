package tribixbite.cleverkeys

import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Drives the real handler through a small editor model, including exact selection/commit. */
class ShiftWordCapitalizationTest {
    private lateinit var recv: KeyEventHandler.IReceiver
    private lateinit var conn: InputConnection
    private lateinit var handler: KeyEventHandler
    private lateinit var info: EditorInfo
    private lateinit var extracted: ExtractedText
    private var text = "To jest łódź."
    private var start = 12
    private var end = 12
    private var setSelectionAccepted = true
    private var commitAccepted = true
    private var selectedTextOverride: String? = null

    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        recv = mockk(relaxed = true)
        conn = mockk(relaxed = true)
        info = mockk<EditorInfo>(relaxed = true).also {
            it.inputType = InputType.TYPE_CLASS_TEXT
            it.packageName = "example.editor"
        }
        every { recv.getHandler() } returns mockk<Handler>(relaxed = true)
        every { recv.getCurrentEditorInfo() } returns info
        every { recv.getCurrentInputConnection() } returns conn
        extracted = mockk(relaxed = true)
        requestField().set(null, mockk<ExtractedTextRequest>(relaxed = true))
        every { conn.getExtractedText(any(), any()) } answers {
            extracted.selectionStart = start - extracted.startOffset
            extracted.selectionEnd = end - extracted.startOffset
            extracted
        }
        every { conn.getTextBeforeCursor(any(), any()) } answers {
            text.substring((start - firstArg<Int>()).coerceAtLeast(0), start)
        }
        every { conn.getTextAfterCursor(any(), any()) } answers {
            text.substring(end, (end + firstArg<Int>()).coerceAtMost(text.length))
        }
        every { conn.beginBatchEdit() } returns true
        every { conn.endBatchEdit() } returns true
        every { conn.setSelection(any(), any()) } answers {
            if (setSelectionAccepted) { start = firstArg(); end = secondArg() }
            setSelectionAccepted
        }
        every { conn.getSelectedText(any()) } answers {
            selectedTextOverride ?: text.substring(start, end)
        }
        every { conn.commitText(any(), any()) } answers {
            if (commitAccepted) {
                val replacement = firstArg<CharSequence>().toString()
                text = text.substring(0, start) + replacement + text.substring(end)
                start += replacement.length
                end = start
            }
            commitAccepted
        }
        handler = KeyEventHandler(recv)
    }
    @After fun teardown() {
        requestField().set(null, null)
        unmockkStatic(Log::class)
    }
    private fun requestField() = KeyEventHandler::class.java.getDeclaredField("moveCursorReq")
        .apply { isAccessible = true }
    private fun park(cursor: Int) {
        val old = start
        start = cursor; end = cursor
        handler.selection_updated(old + 1, cursor)
    }

    @Test fun wordEndTogglesFirstLetter() {
        park(12)
        assertTrue(handler.tryWordCapitalization())
        assertEquals("To jest Łódź.", text)
        assertEquals(12, start); assertEquals(start, end)
        verify(exactly = 1) { conn.commitText("Ł", 1) }
        verify(exactly = 1) { recv.onWordCapitalizationChanged(any()) }
        verify(exactly = 0) { conn.deleteSurroundingText(any(), any()) }
    }
    @Test fun middleOfWordDoesNotLoseSuffixOrMoveCaret() {
        park(10)
        assertTrue(handler.tryWordCapitalization())
        assertEquals("To jest Łódź.", text)
        assertEquals(10, start)
    }
    @Test fun secondTapReturnsToLowercase() {
        park(10)
        assertTrue(handler.tryWordCapitalization())
        handler.selection_updated(9, 10)
        assertTrue(handler.tryWordCapitalization())
        assertEquals("To jest łódź.", text)
        assertEquals(10, start)
    }
    @Test fun suffixCapitalizationAndWhitespaceRemainExact() {
        text = "A iPhone  B"; park(5)
        assertTrue(handler.tryWordCapitalization())
        assertEquals("A IPhone  B", text)
    }
    @Test fun extractedStartOffsetIsAdded() {
        extracted.startOffset = 8
        park(10)
        assertTrue(handler.tryWordCapitalization())
        assertEquals("To jest Łódź.", text)
        verify { conn.setSelection(8, 9) }
    }
    @Test fun ordinaryShiftWithoutCursorReturnDoesNotEdit() {
        assertFalse(handler.tryWordCapitalization())
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun ownTypingCallbackKeepsOrdinaryShift() {
        handler.noteEditorTextMutation(conn)
        handler.selection_updated(8, 12)
        assertFalse(handler.tryWordCapitalization())
        park(10)
        assertTrue(handler.tryWordCapitalization())
    }
    @Test fun typingAfterCursorReturnDisarmsEdit() {
        park(10)
        handler.key_down(KeyValue.makeCharKey('x'), false)
        assertFalse(handler.tryWordCapitalization())
    }
    @Test fun resetAtFieldBoundaryDisarmsEdit() {
        park(10); handler.invalidateWordCaseEdit()
        assertFalse(handler.tryWordCapitalization())
    }
    @Test fun afterSpaceKeepsOrdinaryShift() {
        text = "To jest łódź "; park(13)
        assertFalse(handler.tryWordCapitalization())
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun selectionRangeIsNotRewritten() {
        park(10); start = 8; end = 12
        handler.selection_updated(10, 8, 10, 12)
        assertFalse(handler.tryWordCapitalization())
    }
    @Test fun staleCursorPositionCannotEditAnotherWord() {
        park(10); start = 2; end = 2
        assertFalse(handler.tryWordCapitalization())
        assertEquals("To jest łódź.", text)
    }
    @Test fun unavailableConnectionAndContextFallThrough() {
        park(10)
        every { recv.getCurrentInputConnection() } returns null
        assertFalse(handler.tryWordCapitalization())
        every { recv.getCurrentInputConnection() } returns conn
        every { conn.getTextAfterCursor(any(), any()) } returns null
        assertFalse(handler.tryWordCapitalization())
    }
    @Test fun refusingSelectionDoesNotChangeText() {
        park(10); setSelectionAccepted = false
        assertFalse(handler.tryWordCapitalization())
        assertEquals("To jest łódź.", text)
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun selectedTextMismatchDoesNotCommit() {
        park(10); selectedTextOverride = "x"
        assertFalse(handler.tryWordCapitalization())
        assertEquals(10, start)
        assertEquals("To jest łódź.", text)
        verify(exactly = 0) { conn.commitText(any(), any()) }
        verify(exactly = 1) { conn.endBatchEdit() }
    }
    @Test fun failedCommitRestoresOriginalCursorAndText() {
        park(10); commitAccepted = false
        assertFalse(handler.tryWordCapitalization())
        assertEquals(10, start)
        assertEquals("To jest łódź.", text)
        verify(exactly = 0) { recv.onWordCapitalizationChanged(any()) }
    }
    @Test fun privateAndTechnicalFieldsKeepNormalShift() {
        park(10)
        for (variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_URI)) {
            info.inputType = InputType.TYPE_CLASS_TEXT or variation
            assertFalse(handler.tryWordCapitalization())
        }
        info.inputType = InputType.TYPE_CLASS_NUMBER
        assertFalse(handler.tryWordCapitalization())
        info.inputType = InputType.TYPE_NULL
        assertFalse(handler.tryWordCapitalization())
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun webTextEditorUsesTheSameWordEdit() {
        park(10)
        info.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
        assertTrue(handler.tryWordCapitalization())
        assertEquals("To jest Łódź.", text)
        assertEquals(10, start)
    }
    @Test fun terminalKeepsNormalShift() {
        park(10); info.packageName = "com.termux"
        assertFalse(handler.tryWordCapitalization())
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun inlineKeyboardModesDoNotEditAppText() {
        park(10)
        every { recv.isClipboardSearchMode() } returns true
        assertFalse(handler.tryWordCapitalization())
        every { recv.isClipboardSearchMode() } returns false
        every { recv.isClipboardEditMode() } returns true
        assertFalse(handler.tryWordCapitalization())
        every { recv.isClipboardEditMode() } returns false
        every { recv.isClipboardTagMode() } returns true
        assertFalse(handler.tryWordCapitalization())
        every { recv.isClipboardTagMode() } returns false
        every { recv.isEmojiPaneOpen() } returns true
        assertFalse(handler.tryWordCapitalization())
        every { recv.isEmojiPaneOpen() } returns false
        every { recv.isGifPaneOpen() } returns true
        assertFalse(handler.tryWordCapitalization())
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun movingAcrossEveryInteriorPositionWorksWithoutFinalBatchCallbacks() {
        for (word in listOf("łódź", "malina", "warszawska", "znowu")) {
            handler.invalidateWordCaseEdit()
            text = "A $word."
            start = text.length; end = start
            var expected = word
            for (offset in 0..word.length) {
                val old = start
                start = 2 + offset; end = start
                handler.selection_updated(old, start)
                assertTrue("$word at $offset", handler.tryWordCapitalization())
                expected = if (expected.first().isUpperCase()) expected.replaceFirstChar { it.lowercase() }
                    else expected.replaceFirstChar { it.uppercase() }
                assertEquals("A $expected.", text)
                assertEquals(2 + offset, start)
                assertEquals(start, end)
                // No callback is sent for the unchanged restored caret.
            }

        }
    }
}
