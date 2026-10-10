package tribixbite.cleverkeys

import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipboardSuggestionBarTest {
    @Test fun holdingWordDispatchesDictionaryActionWithoutCommittingAndKeepsRecycledIndex() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val bar = SuggestionBar(instrumentation.targetContext)
            var selected: String? = null
            var held: String? = null
            bar.setOnSuggestionSelectedListener { selected = it }
            bar.setOnSuggestionInspectedListener { index, word, _ -> assertEquals(0, index); held = word }
            bar.setClipboardSuggestion("tekst", {}, {})
            bar.setSuggestions(listOf("grzeje", "praca"))
            val word = (0 until bar.childCount).map { bar.getChildAt(it) }
                .filterIsInstance<TextView>().first { it.text.toString() == "grzeje" }
            assertTrue(word.performLongClick())
            assertEquals("grzeje", held)
            assertNull(selected)
            bar.setSuggestions(listOf("obcy", "praca"))
            assertTrue(word.performLongClick())
            assertEquals("obcy", held)
            word.performClick()
            assertEquals("obcy", selected)
        }
    }
    @Test fun pasteChipStaysSeparateFromWordCandidatesAndSurvivesIdleRefresh() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val bar = SuggestionBar(instrumentation.targetContext)
            var pasted = false
            var opened = false
            bar.setClipboardSuggestion("tekst", { pasted = true }, { opened = true })
            bar.setSuggestions(listOf("praca", "pracą"))
            bar.clearSuggestions()
            assertTrue(bar.getCurrentSuggestions().isEmpty())
            assertEquals(1, bar.childCount)
            val chip = bar.getChildAt(0) as TextView
            assertEquals(instrumentation.targetContext.getString(R.string.clipboard_suggestion_paste), chip.text.toString())
            assertNotNull(chip.compoundDrawablesRelative[0])
            assertTrue(chip.performLongClick())
            assertTrue(opened)
            assertFalse(pasted)
            chip.performClick()
            assertTrue(pasted)
        }
    }
    @Test fun dismissingChipKeepsCandidateOrderAndPasswordHidesIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val bar = SuggestionBar(instrumentation.targetContext)
            bar.setSuggestions(listOf("praca", "pracą"))
            bar.setClipboardSuggestion("tekst", {})
            bar.setClipboardSuggestion(null, null)
            assertEquals(listOf("praca", "pracą"), bar.getCurrentSuggestions())
            bar.setClipboardSuggestion("tekst", {})
            bar.setPasswordMode(true)
            bar.setPasswordMode(false)
            assertFalse((0 until bar.childCount).map { bar.getChildAt(it) }
                .filterIsInstance<TextView>().any { it.text.toString() == instrumentation.targetContext.getString(R.string.clipboard_suggestion_paste) })
        }
    }
}
