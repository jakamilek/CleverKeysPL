package tribixbite.cleverkeys

import android.os.Handler
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import tribixbite.cleverkeys.ai.HerbertLiveRuntime
import tribixbite.cleverkeys.ai.HerbertFormGroup
import tribixbite.cleverkeys.langpack.*
import io.mockk.*
import android.content.res.Resources
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.google.common.truth.Truth.assertWithMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.objenesis.ObjenesisStd

/**
 * Proper-noun casing on swipe (device-confirmed gap): a swiped "bowie" committed lowercase
 * even at a sentence start.
 *
 * Tap typing is capitalized there because [Autocapitalisation] latches the SHIFT fake pointer
 * and each tapped key is shifted. The swipe path instead snapshots the shift latch at swipe
 * START (Pointers.onTouchDown → recognizer) — a snapshot that goes stale whenever autocap's
 * delayed (50ms) latch, a suggestion-commit the autocap cursor tracker never saw, or a
 * selection-update race un-latches shift around the gesture. The fix applies the SAME decision
 * the tap path's Autocapitalisation uses, but at COMMIT time: when neither shift nor caps lock
 * was latched, `Autocapitalisation.shouldCapitalizeAtCursor` (autocap setting + the field's
 * CAP flags + `getCursorCapsMode`) decides whether the swiped slate is capitalized.
 *
 * Deliberately NOT proper-noun dictionary casing — that is a different feature; this only
 * mirrors what tap typing would have produced at the same cursor.
 *
 * Harness: mock tier, Objenesis-allocated SuggestionHandler with only the fields the swipe
 * path reads seeded — same pattern as [SuggestionTapAddAndIWordTest].
 */
class SwipeAutocapCommitTest {

    private val objenesis = ObjenesisStd()

    private lateinit var contextTracker: PredictionContextTracker
    private lateinit var coordinator: PredictionCoordinator
    private lateinit var dictionary: DictionaryManager
    private lateinit var predictor: WordPredictor
    private lateinit var bar: SuggestionBar
    private lateinit var ic: InputConnection
    private lateinit var resources: Resources
    private lateinit var config: Config
    private lateinit var inputCoordinator: InputCoordinator

    /** The words the handler last pushed to the bar; getTopSuggestion answers from it. */
    private var barWords: List<String> = emptyList()
    private var barMetas: List<SuggestionMeta> = emptyList()
    private var barScores: List<Int> = emptyList()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        mockkObject(Config.Companion)
        config = mockk(relaxed = true)
        config.autocapitalisation = true
        config.auto_space_after_suggestion = true
        every { Config.globalConfig() } returns config

        dictionary = mockk(relaxed = true)
        every { dictionary.getLanguageIntelligenceProvider(any()) } returns null
        predictor = mockk(relaxed = true)
        every { predictor.applyUserWordCaseToList(any()) } answers { firstArg() }
        coordinator = mockk(relaxed = true)
        every { coordinator.getDictionaryManager() } returns dictionary
        every { coordinator.getWordPredictor() } returns predictor

        contextTracker = mockk(relaxed = true)
        every { contextTracker.getLastCommitSource() } returns PredictionSource.UNKNOWN
        every { contextTracker.getLastAutoInsertedWord() } returns null
        every { contextTracker.wasLastInputSwipe() } returns true
        every { contextTracker.getCurrentWordLength() } returns 0
        every { contextTracker.getCurrentWord() } returns ""
        every { contextTracker.shouldSyncForInputType(any()) } returns true

        barWords = emptyList()
        bar = mockk(relaxed = true)
        barMetas = emptyList()
        barScores = emptyList()
        every { bar.getMetaForSuggestion(any()) } answers {
            barMetas.getOrNull(barWords.indexOf(firstArg<String>()))
        }
        every { bar.setSuggestionsWithScores(any(), any(), any()) } answers {
            barWords = firstArg<List<String>>().toList()
            barScores = secondArg<List<Int>>().toList()
            barMetas = thirdArg<List<SuggestionMeta>>().toList()
        }
        every { bar.getTopSuggestion() } answers { barWords.firstOrNull() }

        inputCoordinator = mockk(relaxed = true)
        every { inputCoordinator.getCurrentSwipeData() } returns null

        ic = mockk(relaxed = true)
        every { ic.getTextBeforeCursor(any(), any()) } returns ""
        every { ic.getTextAfterCursor(any(), any()) } returns ""
        resources = mockk(relaxed = true)
    }

    @After
    fun teardown() = unmockkAll()

    // ------------------------------------------------------------------ fixtures

    private fun handler(): SuggestionHandler {
        val handler = objenesis.newInstance(SuggestionHandler::class.java)
        handler.setField("contextTracker", contextTracker)
        handler.setField("predictionCoordinator", coordinator)
        handler.setField("suggestionBar", bar)
        handler.setField("config", config)
        handler.setField("contractionManager", mockk<ContractionManager>(relaxed = true))
        handler.setField("keyeventhandler", mockk<KeyEventHandler>(relaxed = true))
        handler.setField("predictionTasks", mockk<PredictionTaskRunner>(relaxed = true))
        return handler
    }

    /** A sentence-capitalizing text field (what a notes/message body declares). */
    private fun capSentencesField(): EditorInfo =
        objenesis.newInstance(EditorInfo::class.java).apply {
            packageName = "com.example.notes"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

    /** A plain text field that declares NO capitalization behavior at all. */
    private fun noCapsField(): EditorInfo =
        objenesis.newInstance(EditorInfo::class.java).apply {
            packageName = "com.example.notes"
            inputType = InputType.TYPE_CLASS_TEXT
        }

    private fun swipe(
        editorInfo: EditorInfo,
        shiftActive: Boolean = false,
        shiftLocked: Boolean = false,
    ) {
        handler().handleSwipePredictionResults(
            listOf("bowie", "bowls"), listOf(100, 90), ic, editorInfo, resources,
            shiftActive, shiftLocked, inputCoordinator
        )
    }

    private fun liveFixture(family: Boolean = false, contextText: String = "Jedziemy do "): Pair<SuggestionHandler, EditorInfo> {
        config.herbert_live_enabled = true
        config.herbert_context_words = 32
        config.herbert_wait_ms = 350
        config.autocapitalisation = false
        config.auto_space_before_suggestion = true
        config.edit_behavior = EditBehaviorOptions()
        every { dictionary.getCurrentLanguage() } returns "pl"
        val provider = if (family) IntelligenceJson.parse(
            javaClass.getResource("/polish-surface-family-v5.json")!!.readText(Charsets.UTF_8).reader(),
            "pl", 5, setOf("capitalization", "metadata")) else LanguageIntelligenceProvider(IntelligencePackageInfo("pl", 5, null),
            setOf("capitalization"), mapOf("łódź" to LanguageIntelligence("łódź", null,
                CapitalizationInfo("łódź", listOf(SurfaceVariant("łódź", "lower"), SurfaceVariant("Łódź", "title"))), null)))
        every { dictionary.getLanguageIntelligenceProvider("pl") } returns provider
        every { ic.getTextBeforeCursor(any(), any()) } returns contextText
        every { ic.getTextAfterCursor(any(), any()) } returns ""
        val selection = objenesis.newInstance(ExtractedText::class.java).apply {
            startOffset = 0; selectionStart = contextText.length; selectionEnd = contextText.length
        }
        every { ic.getExtractedText(any(), any()) } returns selection
        val h = handler()
        val info = noCapsField()
        h.liveEditorProvider = { ic to info }
        h.setField("fieldAllowsPersonalizedLearning", true)
        h.setField("herbertExtractionRequest", objenesis.newInstance(ExtractedTextRequest::class.java))
        h.setField("mainHandler", mockk<Handler>(relaxed = true))
        mockkObject(HerbertLiveRuntime)
        every { HerbertLiveRuntime.state } returns HerbertLiveRuntime.State.READY
        every { HerbertLiveRuntime.revision } returns 7L
        every { HerbertLiveRuntime.noteResult(any()) } just Runs
        return h to info
    }

    @Test fun ordinarySwipeFormsAreRankedBeforeCommitEvenWhenCaseVariantsAreDisabled() {
        val (h, info) = liveFixture(family = true, contextText = "Tak, była w podpowiedziach właściwa ")
        config.edit_behavior = EditBehaviorOptions(showCaseVariants = false)
        var complete: ((Map<String, Float>?, Long) -> Unit)? = null
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } answers {
            assertWithMessage("original editor context reaches the model").that(arg<String>(0))
                .isEqualTo("Tak, była w podpowiedziach właściwa ")
            assertWithMessage("ordinary decoded surfaces only").that(arg<HerbertFormGroup>(1).surfaces)
                .containsExactly("kapitalizacją", "kapitalizacja").inOrder()
            complete = arg(3); true
        }
        h.handleSwipePredictionResults(listOf("kapitalizacją", "kapitalizacja", "kapitalizm"),
            listOf(220, 190, 90), ic, info, resources, false, false, inputCoordinator, SuggestionOrigin.GEOMETRIC)
        verify(exactly = 0) { ic.commitText(any(), any()) }
        complete!!(mapOf("kapitalizacją" to -8f, "kapitalizacja" to -1f), 7L)
        verify(exactly = 1) { ic.commitText("kapitalizacja ", 1) }
        assertWithMessage("both original forms remain selectable").that(barWords)
            .containsExactly("kapitalizacja", "kapitalizacją", "kapitalizm").inOrder()
        assertWithMessage("geometry travels with the form").that(barScores).containsExactly(190, 220, 90).inOrder()
        assertWithMessage("no invented capitalization policy").that(barMetas.map { it.preserveExactCase })
            .containsExactly(false, false, false).inOrder()
    }

    @Test fun actualSourceLemmaAllowsAnEndingChangeThroughTheSingleCommitPath() {
        val (h, info) = liveFixture(family = true, contextText = "To jest bardzo ważna ")
        var complete: ((Map<String, Float>?, Long) -> Unit)? = null
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } answers {
            assertWithMessage("source-confirmed endings reach SI").that(arg<HerbertFormGroup>(1).surfaces)
                .containsExactly("pracy", "Pracy", "praca", "Praca").inOrder()
            complete = arg(3); true
        }
        h.handleSwipePredictionResults(listOf("pracy", "praca", "malina"), listOf(220, 190, 90),
            ic, info, resources, false, false, inputCoordinator, SuggestionOrigin.GEOMETRIC)
        verify(exactly = 0) { ic.commitText(any(), any()) }
        complete!!(mapOf("pracy" to -7f, "Pracy" to -8f, "praca" to -1f, "Praca" to -6f), 7L)
        verify(exactly = 1) { ic.commitText("praca ", 1) }
        assertWithMessage("ending alternatives stay available").that(barWords)
            .containsExactly("praca", "pracy", "malina", "Praca", "Pracy").inOrder()
    }

    private fun familySwipe(h: SuggestionHandler, info: EditorInfo) {
        h.handleSwipePredictionResults(listOf("Malina", "maliną", "mamoną"), listOf(220, 160, 150),
            ic, info, resources, false, false, inputCoordinator, SuggestionOrigin.GEOMETRIC)
    }

    @Test fun commonWordPublishesThreeDifferentChoicesAfterCompleteFourSurfaceSIAnalysis() {
        val (h, info) = liveFixture(family = true, contextText = "To jest bardzo ważna ")
        var complete: ((Map<String, Float>?, Long) -> Unit)? = null
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } answers {
            assertWithMessage("display compaction never removes model inputs").that(arg<HerbertFormGroup>(1).surfaces)
                .containsExactly("praca", "Praca", "pracą", "Pracą").inOrder()
            complete = arg(3); true
        }
        h.handleSwipePredictionResults(listOf("praca", "pracą", "pralka", "prawda"),
            listOf(220, 180, 120, 90), ic, info, resources, false, false, inputCoordinator, SuggestionOrigin.GEOMETRIC)
        verify(exactly = 0) { ic.commitText(any(), any()) }
        complete!!(mapOf("praca" to -1f, "pracą" to -2f, "Praca" to -7f, "Pracą" to -8f), 7L)
        verify(exactly = 1) { ic.commitText("praca ", 1) }
        assertWithMessage("another decoded word is visible before unused case variants").that(barWords)
            .containsExactly("praca", "pracą", "pralka", "Praca", "Pracą", "prawda").inOrder()
        assertWithMessage("parallel geometric weights remain aligned").that(barScores)
            .containsExactly(220, 180, 120, 220, 180, 90).inOrder()
        assertWithMessage("exact-case policy travels with each surface").that(barMetas.map { it.preserveExactCase })
            .containsExactly(true, true, false, true, true, false).inOrder()
        complete!!(mapOf("praca" to -8f, "pracą" to -7f, "Praca" to -1f, "Pracą" to -2f), 7L)
        verify(exactly = 1) { ic.commitText(any(), any()) }
    }

    @Test fun capitalizedWinnerStillCommitsFirstAndProtectedShiftUsesTheSameCompactPresentation() {
        val (h, info) = liveFixture()
        var complete: ((Map<String, Float>?, Long) -> Unit)? = null
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } answers { complete = arg(3); true }
        h.handleSwipePredictionResults(listOf("łódź", "kosz", "luz"), listOf(190, 129, 90),
            ic, info, resources, false, false, inputCoordinator, SuggestionOrigin.GEOMETRIC)
        complete!!(mapOf("łódź" to -8f, "Łódź" to -2f), 7L)
        verify(exactly = 1) { ic.commitText("Łódź ", 1) }
        assertWithMessage("named winner wins without reserving a second slot").that(barWords)
            .containsExactly("Łódź", "kosz", "luz", "łódź").inOrder()
        clearMocks(HerbertLiveRuntime, ic, answers = false)
        h.handleSwipePredictionResults(listOf("łódź", "kosz", "luz"), listOf(190, 129, 90),
            ic, info, resources, true, false, inputCoordinator, SuggestionOrigin.GEOMETRIC)
        verify(exactly = 0) { HerbertLiveRuntime.rank(any(), any(), any(), any()) }
        verify(exactly = 1) { ic.commitText("Łódź ", 1) }
        assertWithMessage("explicit Shift keeps capitalized commit and all alternatives").that(barWords)
            .containsExactly("Łódź", "Kosz", "Luz", "łódź").inOrder()
    }

    @Test fun liveFamilyRanksFourActualSourceFormsAndCommitsInstrumentalNameOnce() {
        val (h, info) = liveFixture(family = true)
        var complete: ((Map<String, Float>?, Long) -> Unit)? = null
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } answers {
            assertWithMessage("source group sent to worker").that(arg<HerbertFormGroup>(1).surfaces)
                .containsExactly("Malina", "malina", "maliną", "Maliną").inOrder()
            complete = arg(3); true
        }
        familySwipe(h, info)
        verify(exactly = 0) { ic.commitText(any(), any()) }
        complete!!(mapOf("Malina" to -6f, "malina" to -8f, "maliną" to -3f, "Maliną" to -1f), 7L)
        verify(exactly = 1) { ic.commitText("Maliną ", 1) }
        assertWithMessage("all alternatives survive").that(barWords)
            .containsExactly("Maliną", "Malina", "mamoną", "maliną", "malina").inOrder()
        assertWithMessage("each word keeps its decoder score").that(barScores)
            .containsExactly(160, 220, 150, 160, 220).inOrder()
        complete!!(mapOf("Malina" to -1f, "malina" to -8f, "maliną" to -3f, "Maliną" to -6f), 7L)
        verify(exactly = 1) { ic.commitText(any(), any()) }
    }

    @Test fun familyAlternativesRemainAvailableWithoutLiveSI() {
        val (h, info) = liveFixture(family = true)
        config.herbert_live_enabled = false
        familySwipe(h, info)
        verify(exactly = 0) { HerbertLiveRuntime.rank(any(), any(), any(), any()) }
        verify(exactly = 1) { ic.commitText("Malina ", 1) }
        assertWithMessage("no SI needed to offer the source forms").that(barWords)
            .containsExactly("Malina", "maliną", "mamoną", "malina", "Maliną").inOrder()
    }

    @Test fun familyDeadlineFallsBackOnceAndLateCallbackCannotReplaceText() {
        val (h, info) = liveFixture(family = true)
        val timeout = slot<Runnable>()
        val main = mockk<Handler>(relaxed = true)
        h.setField("mainHandler", main)
        every { main.postDelayed(capture(timeout), any<Long>()) } returns true
        var complete: ((Map<String, Float>?, Long) -> Unit)? = null
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } answers { complete = arg(3); true }
        familySwipe(h, info)
        timeout.captured.run()
        complete!!(mapOf("Malina" to -6f, "malina" to -8f, "maliną" to -3f, "Maliną" to -1f), 7L)
        verify(exactly = 1) { ic.commitText("Malina ", 1) }
        verify(exactly = 1) { ic.commitText(any(), any()) }
        assertWithMessage("fallback retains every choice and leaves room for another word").that(barWords)
            .containsExactly("Malina", "maliną", "mamoną", "malina", "Maliną").inOrder()
    }

    private fun liveSwipe(h: SuggestionHandler, info: EditorInfo, shift: Boolean = false) {
        h.handleSwipePredictionResults(listOf("łódź", "kosz"), listOf(190, 129), ic, info,
            resources, shift, false, inputCoordinator, SuggestionOrigin.GEOMETRIC)
    }

    @Test fun liveHerbertOrdersBothSourceFormsBeforeTheSingleCommit() {
        val (h, info) = liveFixture()
        var complete: ((Map<String, Float>?, Long) -> Unit)? = null
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } answers { complete = arg(3); true }
        liveSwipe(h, info)
        verify(exactly = 0) { ic.commitText(any(), any()) }
        complete!!(mapOf("łódź" to -8f, "Łódź" to -2f), 7L)
        verify(exactly = 1) { ic.commitText("Łódź ", 1) }
        assertWithMessage("winner stays first and alternate remains selectable").that(barWords)
            .containsExactly("Łódź", "kosz", "łódź").inOrder()
        complete!!(mapOf("łódź" to -1f, "Łódź" to -9f), 7L)
        verify(exactly = 1) { ic.commitText(any(), any()) }
    }

    @Test fun nextTouchCommitsBaselineOnceAndIgnoresLateModelResult() {
        val (h, info) = liveFixture()
        var complete: ((Map<String, Float>?, Long) -> Unit)? = null
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } answers { complete = arg(3); true }
        liveSwipe(h, info)
        h.flushPendingHerbertSwipe()
        complete!!(mapOf("łódź" to -8f, "Łódź" to -2f), 7L)
        verify(exactly = 1) { ic.commitText("łódź ", 1) }
        verify(exactly = 1) { ic.commitText(any(), any()) }
    }

    @Test fun changedEditorCannotReceiveALateResultOrTimeoutCommit() {
        val (h, info) = liveFixture()
        var complete: ((Map<String, Float>?, Long) -> Unit)? = null
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } answers { complete = arg(3); true }
        liveSwipe(h, info)
        h.liveEditorProvider = { mockk<InputConnection>(relaxed = true) to info }
        h.onEditorCursorChanged()
        complete!!(mapOf("łódź" to -8f, "Łódź" to -2f), 7L)
        h.flushPendingHerbertSwipe()
        verify(exactly = 0) { ic.commitText(any(), any()) }
    }

    @Test fun explicitShiftAndPrivateFieldsSkipTheModelBeforeReadingContext() {
        val (h, info) = liveFixture()
        every { HerbertLiveRuntime.rank(any(), any(), any(), any()) } returns false
        liveSwipe(h, info, shift = true)
        verify(exactly = 0) { HerbertLiveRuntime.rank(any(), any(), any(), any()) }
        info.imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        clearMocks(ic, answers = false)
        liveSwipe(h, info)
        verify(exactly = 0) { ic.getTextBeforeCursor(4097, 0) }
        verify(exactly = 0) { HerbertLiveRuntime.rank(any(), any(), any(), any()) }
    }

    // ------------------------------------------------------------------ the gap

    @Test fun staleEditorCapsAfterAnOrdinaryPeriodStillCapitalizesSwipeCommit() {
        mockkStatic(TextUtils::class)
        every { TextUtils.getCapsMode("To łódź. ", 9, InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) } returns
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        every { ic.getCursorCapsMode(any()) } returns 0
        every { ic.getTextBeforeCursor(any(), any()) } returns "To łódź. "
        swipe(capSentencesField())
        verify { ic.commitText("Bowie ", 1) }
        assertWithMessage("commit and slate agree").that(barWords.first()).isEqualTo("Bowie")
    }

    @Test
    fun aSentenceStartSwipeCommitIsCapitalized() {
        // The editor reports "capitalize here" — exactly what autocap consults for taps.
        every { ic.getCursorCapsMode(any()) } returns InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        swipe(capSentencesField())

        verify { ic.commitText("Bowie ", 1) }
        assertWithMessage("the bar must show the same casing the commit used")
            .that(barWords.first()).isEqualTo("Bowie")
    }

    @Test
    fun midSentenceTheSwipeCommitStaysLowercase() {
        every { ic.getCursorCapsMode(any()) } returns 0

        swipe(capSentencesField())

        verify { ic.commitText("bowie ", 1) }
    }

    @Test
    fun withAutocapDisabledTheSentenceStartSwipeStaysLowercase() {
        config.autocapitalisation = false
        every { ic.getCursorCapsMode(any()) } returns InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        swipe(capSentencesField())

        verify { ic.commitText("bowie ", 1) }
    }

    @Test
    fun aFieldWithoutCapFlagsIsNeverQueriedAndStaysLowercase() {
        // Mirrors Autocapitalisation.started(): capsMode == 0 disables the feature outright,
        // so a misbehaving editor's getCursorCapsMode can never flip the casing.
        swipe(noCapsField())

        verify(exactly = 0) { ic.getCursorCapsMode(any()) }
        verify { ic.commitText("bowie ", 1) }
    }

    // ------------------------------------------------- explicit shift still wins

    @Test
    fun capsLockStillUppercasesTheWholeWord() {
        every { ic.getCursorCapsMode(any()) } returns 0

        swipe(capSentencesField(), shiftLocked = true)

        verify { ic.commitText("BOWIE ", 1) }
    }

    @Test
    fun aLatchedShiftStillCapitalizesWithoutConsultingTheCursor() {
        swipe(capSentencesField(), shiftActive = true)

        verify { ic.commitText("Bowie ", 1) }
    }

    @Test
    fun sourceVariantsAutoInsertPrimaryAndLowercaseTapReplacesItExactly() {
        val provider = javaClass.getResource("/language-intelligence-trial.json")!!.openStream().reader().use {
            tribixbite.cleverkeys.langpack.IntelligenceJson.parse(it, "pl", 3, setOf("capitalization"))
        }
        every { dictionary.getCurrentLanguage() } returns "pl"
        every { dictionary.getLanguageIntelligenceProvider("pl") } returns provider
        every { ic.getCursorCapsMode(any()) } returns InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        val handler = handler()
        handler.handleSwipePredictionResults(listOf("łódź", "lód"), listOf(100, 80), ic,
            capSentencesField(), resources, false, false, inputCoordinator)
        assertWithMessage("source case alternatives do not displace the other decoded word")
            .that(barWords.take(3)).containsExactly("Łódź", "Lód", "łódź").inOrder()
        verify { ic.commitText("Łódź ", 1) }

        every { contextTracker.getLastCommitSource() } returns PredictionSource.SWIPE
        every { contextTracker.getLastAutoInsertedWord() } returns "Łódź"
        every { contextTracker.getCurrentWord() } returns "Łódź"
        every { ic.getTextBeforeCursor(any(), any()) } returns "Łódź "
        handler.onSuggestionSelected("łódź", ic, capSentencesField(), resources, isManualSelection = true)
        verify { ic.deleteSurroundingText(5, 0) }
        verify { ic.commitText("łódź ", 1) }
    }

    // ----------------------------------------------- complete swipe spacing path

    /** Surrounding text changes immediately on commits, as in an ordinary editor. */
    private fun editorBuffer(initial: String): StringBuilder {
        val text = StringBuilder(initial)
        every { ic.getTextBeforeCursor(any(), any()) } answers {
            text.toString().takeLast(firstArg<Int>())
        }
        every { ic.commitText(any(), any()) } answers {
            text.append(firstArg<CharSequence>())
            true
        }
        every { contextTracker.getCurrentWordLength() } returns 3
        every { contextTracker.getCurrentWord() } returns "hel"
        return text
    }

    @Test
    fun swipeAfterTypedWordAddsItsSeparatorInTheSameCommit() {
        config.auto_space_before_suggestion = true
        val text = editorBuffer("hel")

        swipe(noCapsField())

        assertWithMessage("preserve typed text and apply shared before/after preferences")
            .that(text.toString()).isEqualTo("hel bowie ")
        verify(exactly = 1) { ic.commitText(" bowie ", 1) }
        verify(exactly = 0) { ic.commitText(" ", 1) }
    }

    @Test
    fun swipeAfterTypedWordRespectsDisabledLeadingSpace() {
        config.auto_space_before_suggestion = false
        val text = editorBuffer("hel")

        swipe(noCapsField())

        assertWithMessage("the full swipe wrapper must not override the before preference")
            .that(text.toString()).isEqualTo("helbowie ")
        verify(exactly = 0) { ic.commitText(" ", 1) }
    }

    @Test
    fun searchSwipeAfterTypedWordKeepsSpacingLiteral() {
        config.auto_space_before_suggestion = true
        val text = editorBuffer("hel")
        val field = noCapsField().apply { imeOptions = EditorInfo.IME_ACTION_SEARCH }

        swipe(field)

        assertWithMessage("search exclusions must cover the wrapper as well as the commit")
            .that(text.toString()).isEqualTo("helbowie")
        verify(exactly = 0) { ic.commitText(" ", 1) }
    }

    @Test
    fun optedInPasswordSwipeAfterTypedWordKeepsSpacingLiteral() {
        config.auto_space_before_suggestion = true
        config.swipe_on_password_fields = true
        val text = editorBuffer("hel")
        val field = noCapsField().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        swipe(field)

        assertWithMessage("opt-in password swipe must not inject either separator")
            .that(text.toString()).isEqualTo("helbowie")
        verify(exactly = 0) { ic.commitText(" ", 1) }
    }

    @Test
    fun staleTrackedWordDoesNotDuplicateAnExistingEditorSpace() {
        config.auto_space_before_suggestion = true
        val text = editorBuffer("hel ")

        swipe(noCapsField())

        assertWithMessage("actual editor whitespace wins over stale tracked typing")
            .that(text.toString()).isEqualTo("hel bowie ")
        verify(exactly = 0) { ic.commitText(" ", 1) }
    }

    @Test
    fun numberEndingPeriodAndNextSwipeWordStartASentence() {
        config.auto_space_before_suggestion = true
        val text = editorBuffer("Mam 3.")
        every { ic.getCursorCapsMode(any()) } returns 0

        swipe(capSentencesField())

        assertWithMessage("the separator is decided after casing, so a next-word swipe needs the boundary fallback")
            .that(text.toString()).isEqualTo("Mam 3. Bowie ")
    }

    @Test
    fun decimalNumberBeforeSwipeDoesNotCapitalizeTheWord() {
        config.auto_space_before_suggestion = true
        val text = editorBuffer("Mam 3.4")
        every { ic.getCursorCapsMode(any()) } returns 0

        swipe(capSentencesField())

        assertWithMessage("the decimal's internal period is not sentence punctuation")
            .that(text.toString()).isEqualTo("Mam 3.4 bowie ")
    }

    @Test
    fun numericBoundaryRespectsAutocapAndSearchExclusions() {
        config.auto_space_before_suggestion = true
        config.autocapitalisation = false
        val text = editorBuffer("3.")
        swipe(capSentencesField())
        assertWithMessage("disabled autocap stays disabled").that(text.toString()).isEqualTo("3. bowie ")

        config.autocapitalisation = true
        val searchText = editorBuffer("3.")
        swipe(capSentencesField().apply { imeOptions = EditorInfo.IME_ACTION_SEARCH })
        assertWithMessage("the fallback must not format search input").that(searchText.toString()).isEqualTo("3.bowie")
    }

    // ------------------------------------------------------------------ reflection

    private fun Any.setField(name: String, value: Any?) {
        val field = javaClass.declaredFields.firstOrNull { it.name == name }
        assertWithMessage(
            "field '$name' not found on ${javaClass.simpleName} — it was renamed or removed; " +
                "declared: ${javaClass.declaredFields.map { it.name }}"
        ).that(field).isNotNull()
        field!!.isAccessible = true
        field.set(this, value)
    }
}
