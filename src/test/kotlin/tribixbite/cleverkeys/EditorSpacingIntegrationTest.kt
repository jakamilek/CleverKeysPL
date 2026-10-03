package tribixbite.cleverkeys

import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Real key-up routing with a text buffer, including field switches and deletion refusal. */
class EditorSpacingIntegrationTest {
    private lateinit var recv: KeyEventHandler.IReceiver
    private lateinit var conn: InputConnection
    private lateinit var info: EditorInfo
    private lateinit var config: Config
    private lateinit var handler: KeyEventHandler
    private var before = ""
    private var after = ""
    private var deletionAccepted = true

    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        mockkObject(Config.Companion)
        config = mockk(relaxed = true)
        config.smart_punctuation = true
        config.double_space_to_period = true
        every { Config.globalConfig() } returns config
        info = mockk(relaxed = true)
        info.inputType = InputType.TYPE_CLASS_TEXT
        info.packageName = "example.notes"
        recv = mockk(relaxed = true)
        conn = mockk(relaxed = true)
        every { recv.getHandler() } returns mockk<Handler>(relaxed = true)
        every { recv.getCurrentInputConnection() } returns conn
        every { recv.getCurrentEditorInfo() } returns info
        every { conn.getTextBeforeCursor(any(), any()) } answers { before.takeLast(firstArg<Int>()) }
        every { conn.getTextAfterCursor(any(), any()) } answers { after.take(firstArg<Int>()) }
        every { conn.getExtractedText(any(), any()) } returns null
        every { conn.deleteSurroundingText(any(), any()) } answers {
            if (deletionAccepted) before = before.dropLast(firstArg<Int>())
            deletionAccepted
        }
        every { conn.commitText(any(), any()) } answers {
            before += firstArg<CharSequence>().toString()
            true
        }
        handler = KeyEventHandler(recv)
    }
    @After fun teardown() = unmockkAll()
    private fun type(char: Char) =
        handler.key_up(KeyValue.makeCharKey(char), Pointers.Modifiers.EMPTY, false)
    @Test fun ordinaryCommaUsesTheActualManualSpace() {
        before = "Łódź "; type(',')
        assertEquals("Łódź, ", before)
        verify { recv.markAutoSpacePending(any()) }
    }
    @Test fun ordinaryPunctuationWithoutSpaceStillGetsATrailingSpace() {
        before = "malina"; type(':')
        assertEquals("malina: ", before)
    }
    @Test fun existingSpaceAfterTheCaretIsNotDoubled() {
        before = "Łódź "; after = " dalej"; type(',')
        assertEquals("Łódź,", before)
        assertEquals(" dalej", after)
    }
    @Test fun questionAndExclamationFormOneCluster() {
        before = "Co "; type('?'); type('!')
        assertEquals("Co?! ", before)
    }
    @Test fun decimalTypingKeepsDigitsTogether() {
        before = "3"; type(','); type('1'); type('4')
        assertEquals("3,14", before)
    }
    @Test fun searchActionDisablesPunctuationAndDoubleSpaceFormatting() {
        info.imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_FULLSCREEN
        before = "Łódź "; type(','); type(' '); type(' ')
        assertEquals("Łódź ,  ", before)
        verify(exactly = 0) { conn.deleteSurroundingText(any(), any()) }
    }
    @Test fun passwordVariantsRemainLiteral() {
        for (variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) {
            info.inputType = InputType.TYPE_CLASS_TEXT or variation
            before = "ab "; type('.')
            assertEquals("ab .", before)
        }
    }
    @Test fun technicalAndNumericFieldsRemainLiteral() {
        for (type in listOf(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME)) {
            info.inputType = type
            before = "ab "; type(',')
            assertEquals("ab ,", before)
        }
    }
    @Test fun changingTheLiveFieldDoesNotReuseOrdinaryTextFormatting() {
        before = "Łódź"; type(',')
        info.imeOptions = EditorInfo.IME_ACTION_SEARCH
        type('.')
        assertEquals("Łódź, .", before)
    }
    @Test fun disabledPreferenceAndMissingEditorRemainLiteral() {
        config.smart_punctuation = false
        before = "word "; type(',')
        assertEquals("word ,", before)
        config.smart_punctuation = true
        every { recv.getCurrentEditorInfo() } returns null
        before = "word "; type(',')
        assertEquals("word ,", before)
        assertFalse(EditorSpacingPolicy.allowsAutomaticSpacing(null))
    }
    @Test fun refusedDeletionFallsBackToLiteralKey() {
        before = "word "; deletionAccepted = false; type(',')
        assertEquals("word ,", before)
    }
    @Test fun inlineSearchNeverReachesTheAppConnection() {
        every { recv.isClipboardSearchMode() } returns true
        before = "word "; type(',')
        assertEquals("word ", before)
        verify { recv.appendToClipboardSearch(",") }
        verify(exactly = 0) { conn.commitText(any(), any()) }
    }
    @Test fun swipeApostropheKeepsTheLexicalJoinerBehavior() {
        every { recv.wasLastSpaceAutoInserted() } returns true
        every { recv.getAutoSpaceStampedPosition() } returns -1
        before = "kids "; type('\'')
        assertEquals("kids'", before)
    }
    @Test fun ordinaryLettersDoNotFetchPunctuationContext() {
        before = "hel"; type('i')
        assertEquals("heli", before)
        verify(exactly = 0) { conn.getTextBeforeCursor(500, any()) }
        verify(exactly = 0) { conn.getTextAfterCursor(any(), any()) }
    }
    @Test fun staleOwedSpaceCannotLeakIntoSearch() {
        info.imeOptions = EditorInfo.IME_ACTION_SEARCH
        every { recv.takeOwedTrailingSpace() } returns "word"
        before = "word"; type('a')
        assertEquals("worda", before)
    }
}
