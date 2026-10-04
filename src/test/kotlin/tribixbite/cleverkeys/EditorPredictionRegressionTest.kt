package tribixbite.cleverkeys

import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.objenesis.ObjenesisStd

/** Runs the real tap/cursor prediction pipeline; UI posts are delivered out of order. */
class EditorPredictionRegressionTest {
    private val objenesis = ObjenesisStd()
    private lateinit var config: Config
    private lateinit var tracker: PredictionContextTracker
    private lateinit var handler: SuggestionHandler
    private lateinit var predictor: WordPredictor
    private lateinit var bar: SuggestionBar
    private lateinit var ic: InputConnection
    private lateinit var info: EditorInfo
    private var before = ""
    private var after = ""
    private val posts = mutableListOf<Runnable>()
    private var displayed = emptyList<String>()

    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        mockkObject(Config.Companion)
        config = mockk(relaxed = true)
        config.word_prediction_enabled = true
        config.show_exact_typed_word = true
        config.primary_language = "pl"
        every { Config.globalConfig() } returns config
        tracker = PredictionContextTracker()
        ic = mockk(relaxed = true)
        every { ic.getTextBeforeCursor(any(), any()) } answers { before.takeLast(firstArg()) }
        every { ic.getTextAfterCursor(any(), any()) } answers { after.take(firstArg()) }
        info = objenesis.newInstance(EditorInfo::class.java).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.example.notes"
        }
        predictor = mockk(relaxed = true)
        every { predictor.predictWordsWithContext(any(), any()) } answers {
            WordPredictor.PredictionResult(listOf(firstArg<String>() + "x"), listOf(10))
        }
        every { predictor.isInDictionary(any()) } returns false
        val coordinator = mockk<PredictionCoordinator>(relaxed = true)
        val dictionary = mockk<DictionaryManager>(relaxed = true)
        every { dictionary.isUserWord(any()) } returns false
        every { coordinator.getWordPredictor() } returns predictor
        every { coordinator.getDictionaryManager() } returns dictionary
        val contractions = mockk<ContractionManager>(relaxed = true)
        every { contractions.getNonPairedMapping(any()) } returns null
        every { contractions.getPairedContractions(any()) } returns emptyList()
        val tasks = mockk<PredictionTaskRunner>(relaxed = true)
        every { tasks.cancelAndSubmit(any()) } answers { firstArg<Runnable>().run() }
        val main = mockk<Handler>(relaxed = true)
        every { main.post(any()) } answers { posts.add(firstArg()); true }
        bar = mockk(relaxed = true)
        every { bar.setSuggestionsWithScores(any(), any(), any()) } answers { displayed = firstArg() }
        handler = objenesis.newInstance(SuggestionHandler::class.java)
        field("contextTracker", tracker)
        field("predictionCoordinator", coordinator)
        field("contractionManager", contractions)
        field("predictionTasks", tasks)
        field("mainHandler", main)
        field("suggestionBar", bar)
        field("config", config)
    }

    @After fun teardown() = unmockkAll()
    private fun field(name: String, value: Any?) {
        SuggestionHandler::class.java.getDeclaredField(name).apply { isAccessible = true }.set(handler, value)
    }
    private fun sync() {
        tracker.synchronizeWithCursor(ic, "pl", info)
        handler.handleCursorSyncPrediction()
    }
    private fun deliver() { posts.toList().also { posts.clear() }.forEach { it.run() } }

    @Test fun aPendingOwnEditAcknowledgementDoesNotSwallowCutAndPasteSync() {
        tracker.appendToCurrentWord("old")
        tracker.expectingSelectionUpdate = true
        before = "Tu mal"
        after = "ina jest"
        sync()
        assertThat(tracker.getCurrentWord()).isEqualTo("mal")
        assertThat(tracker.getCurrentWordSuffix()).isEqualTo("ina")
        deliver()
        assertThat(displayed.first()).isEqualTo("exact_add:malina")
    }

    @Test fun queuedOldResultsCannotOverwriteTheNewWord() {
        before = "Tu stary"
        sync()
        before = "Tu nowy"
        sync()
        posts.last().run()
        posts.first().run()
        assertThat(displayed.first()).isEqualTo("exact_add:nowy")
    }

    @Test fun movingTheCursorInvalidatesPostsBeforeTheDebouncedRead() {
        before = "Tu stary"
        sync()
        handler.onEditorCursorChanged()
        deliver()
        verify(exactly = 0) { bar.setSuggestionsWithScores(any(), any(), any()) }
    }

    @Test fun typingAfterPasteUsesTheLiveWordAndDismissesTheOldSpecialPrompt() {
        tracker.appendToCurrentWord("stary")
        tracker.setLastAutoInsertedWord("stary")
        tracker.setLastCommitSource(PredictionSource.SWIPE)
        field("specialPromptActive", true)
        before = "Tu malina"
        handler.handleRegularTyping("a", ic, info)
        deliver()
        assertThat(tracker.getCurrentWord()).isEqualTo("malina")
        assertThat(tracker.getLastAutoInsertedWord()).isNull()
        assertThat(displayed.first()).isEqualTo("exact_add:malina")
    }

    @Test fun editingASyncedWordDismissesAStaleAddPrompt() {
        field("specialPromptActive", true)
        before = "Tu nowy"
        sync()
        deliver()
        assertThat(displayed.first()).isEqualTo("exact_add:nowy")
    }

    @Test fun cursorSyncAfterCutCannotPreserveAStaleSwipeSlate() {
        tracker.setLastAutoInsertedWord("malina")
        tracker.setLastCommitSource(PredictionSource.SWIPE)
        before = ""
        val cursorPosts = mutableListOf<Runnable>()
        val syncHandler = mockk<Handler>(relaxed = true)
        every { syncHandler.postDelayed(any(), any()) } answers { cursorPosts.add(firstArg()); true }
        val input = objenesis.newInstance(InputCoordinator::class.java)
        fun inputField(name: String, value: Any?) {
            InputCoordinator::class.java.getDeclaredField(name).apply { isAccessible = true }.set(input, value)
        }
        inputField("contextTracker", tracker)
        inputField("syncHandler", syncHandler)
        inputField("cursorSyncDelegate", handler)
        inputField("suggestionBar", bar)
        input.onCursorMoved(0, ic, "pl", info)
        cursorPosts.single().run()
        assertThat(tracker.getLastAutoInsertedWord()).isNull()
        assertThat(tracker.getLastCommitSource()).isEqualTo(PredictionSource.UNKNOWN)
        verify { bar.clearSuggestions() }
    }

    @Test fun unreadableEditorPreservesTheTapFallback() {
        tracker.appendToCurrentWord("ma")
        every { ic.getTextBeforeCursor(any(), any()) } returns null
        handler.handleRegularTyping("l", ic, info)
        deliver()
        assertThat(tracker.getCurrentWord()).isEqualTo("mal")
    }

    @Test fun exactAddChecksTheFullLiveTokenAndSelection() {
        assertThat(SuggestionHandler.exactWordMatchesEditor("łodzi", "Tu ło", "dzi dalej", null)).isTrue()
        assertThat(SuggestionHandler.exactWordMatchesEditor("malina", "Tu malina", "", null)).isTrue()
        assertThat(SuggestionHandler.exactWordMatchesEditor("malina", "Tu xmalina", "", null)).isFalse()
        assertThat(SuggestionHandler.exactWordMatchesEditor("malina", "Tu malina", "mi", null)).isFalse()
        assertThat(SuggestionHandler.exactWordMatchesEditor("malina", "Tu nowe", "", null)).isFalse()
        assertThat(SuggestionHandler.exactWordMatchesEditor("malina", "Tu malina", "", "wycięte")).isFalse()
        assertThat(SuggestionHandler.exactWordMatchesEditor("malina", null, "", null)).isFalse()
    }
}
