package tribixbite.cleverkeys

import android.content.ClipData
import android.content.ClipboardManager
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.objenesis.ObjenesisStd

class ClipboardPasteSuggestionsTest {
    private val clipboard = mockk<ClipboardManager>(relaxed = true)
    private val connection = mockk<InputConnection>(relaxed = true)
    private lateinit var info: EditorInfo
    private lateinit var current: Pair<InputConnection?, EditorInfo?>
    private lateinit var suggestions: ClipboardPasteSuggestions
    private var offer: String? = null
    private var action: (() -> Unit)? = null
    private val pasted = mutableListOf<String>()
    private var clip: ClipData? = null
    private var panelsOpened = 0

    @Before fun setup() {
        info = ObjenesisStd().newInstance(EditorInfo::class.java).apply { inputType = InputType.TYPE_CLASS_TEXT }
        current = connection to info
        every { clipboard.primaryClip } answers { clip }
        suggestions = ClipboardPasteSuggestions(clipboard, { text, paste -> offer = text; action = paste },
            { current }, { pasted.add(it) }, { panelsOpened++ })
    }
    @After fun teardown() { suggestions.stop(); unmockkAll() }
    @Test fun holdingOpensPanelWithoutPastingAndDisarmsOldPasteAction() {
        copy("tekst"); suggestions.start(connection, info)
        val queued = action!!
        suggestions.openPanel(); queued()
        assertEquals(1, panelsOpened)
        assertTrue(pasted.isEmpty())
        assertNull(offer)
    }
    @Test fun holdingAfterFirstEditOrFieldSwitchDoesNotOpenPanel() {
        copy("tekst"); suggestions.start(connection, info)
        suggestions.dismiss(); suggestions.openPanel()
        suggestions.start(connection, info)
        current = mockk<InputConnection>() to info
        suggestions.openPanel()
        assertEquals(0, panelsOpened)
    }
    @Test fun holdingDoesNotOpenAnAbsentClipboard() {
        copy("tekst"); suggestions.start(connection, info)
        clip = null; suggestions.openPanel()
        assertEquals(0, panelsOpened)
    }
    private fun copy(text: String) {
        clip = mockk(relaxed = true)
        every { clip!!.itemCount } returns 1
        every { clip!!.getItemAt(0).text } returns text
        every { clip!!.description.extras } returns null
    }
    @Test fun offersAndPastesExactTextIncludingPunctuationWithoutLearningOrSpacing() {
        copy("Zażółć 🙂\n3. Tekst.")
        suggestions.start(connection, info)
        assertEquals("Zażółć 🙂\n3. Tekst.", offer)
        action!!.invoke()
        assertEquals(listOf("Zażółć 🙂\n3. Tekst."), pasted)
        assertNull(offer)
    }
    @Test fun firstEditDisarmsAQueuedTapAndFurtherClipboardEvents() {
        copy("tekst"); suggestions.start(connection, info)
        val queued = action!!
        suggestions.dismiss(); queued()
        assertTrue(pasted.isEmpty())
        assertNull(offer)
    }
    @Test fun aChangedClipboardCannotBePastedFromAnOldChip() {
        copy("pierwszy"); suggestions.start(connection, info)
        val queued = action!!
        copy("drugi"); queued()
        assertTrue(pasted.isEmpty())
    }
    @Test fun switchingEditorDisarmsEvenIdenticalText() {
        copy("tekst"); suggestions.start(connection, info)
        val queued = action!!
        current = mockk<InputConnection>() to info; queued()
        assertTrue(pasted.isEmpty())
    }
    @Test fun passwordAndPrivateFieldsDoNotReadClipboard() {
        info.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        suggestions.start(connection, info)
        info.inputType = InputType.TYPE_CLASS_TEXT
        info.imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        suggestions.start(connection, info)
        verify(exactly = 0) { clipboard.primaryClip }
        assertNull(offer)
    }
    @Test fun anOldTapCannotPasteTheSameClipIntoANewSession() {
        copy("tekst"); suggestions.start(connection, info)
        val queued = action!!
        val next = mockk<InputConnection>(relaxed = true)
        current = next to info
        suggestions.start(next, info)
        queued()
        assertTrue(pasted.isEmpty())
        assertEquals("tekst", offer)
    }
    @Test fun absentBlankOversizedOrNonTextClipsAreNotOffered() {
        suggestions.start(connection, info); assertNull(offer)
        for (text in listOf(" ", "x".repeat(65537))) {
            copy(text); suggestions.start(connection, info); assertNull(offer)
        }
        every { clip!!.getItemAt(0).text } returns null
        suggestions.start(connection, info); assertNull(offer)
    }
    @Test fun clipboardChangesRefreshOnlyTheActiveSessionAndStopRemovesListener() {
        val listeners = mutableListOf<ClipboardManager.OnPrimaryClipChangedListener>()
        every { clipboard.addPrimaryClipChangedListener(any()) } answers { listeners.add(firstArg()); Unit }
        copy("a"); suggestions.start(connection, info)
        copy("b"); listeners.last().onPrimaryClipChanged(); assertEquals("b", offer)
        val queued = action!!
        suggestions.stop(); listeners.last().onPrimaryClipChanged(); queued()
        assertNull(offer); assertTrue(pasted.isEmpty())
        verify { clipboard.removePrimaryClipChangedListener(listeners.last()) }
    }
}
