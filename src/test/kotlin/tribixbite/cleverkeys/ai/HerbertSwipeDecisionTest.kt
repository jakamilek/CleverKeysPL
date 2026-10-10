package tribixbite.cleverkeys.ai

import org.junit.Assert.*
import org.junit.Test
import tribixbite.cleverkeys.SwipeSurfaceVariants
import tribixbite.cleverkeys.langpack.IntelligenceJson

class HerbertSwipeDecisionTest {
    private fun sourceProvider() = IntelligenceJson.parse(
        javaClass.getResource("/polish-surface-family-v5.json")!!.readText(Charsets.UTF_8).reader(),
        "pl", 5, setOf("capitalization", "metadata"))

    @Test fun commonGeographicHomonymDoesNotReserveTopThreeForEveryCapitalization() {
        val source = SwipeSurfaceVariants.expand(listOf("praca", "pracą", "pralka", "prawda"),
            listOf(220, 180, 120, 90), List(4) { "pl" }, sourceProvider(), false, false)
        assertEquals(listOf("praca", "Praca", "pracą", "Pracą"), source.words.take(4))
        val ranked = HerbertLiveSlate.ordered(source, listOf("praca", "pracą", "Praca", "Pracą"))
        val shown = SwipeSurfaceVariants.present(ranked, "pl")
        assertEquals(listOf("praca", "pracą", "pralka", "Praca", "Pracą", "prawda"), shown.words)
        assertEquals(listOf(220, 180, 120, 220, 180, 90), shown.scores)
        assertEquals(listOf(true, true, false, true, true, false), shown.exactCase)
        assertEquals(0, shown.formGroupSize)
        assertTrue(shown.presentationOnly)
        assertNull(HerbertLiveSlate.group(shown, sourceProvider()))
        assertEquals(shown, SwipeSurfaceVariants.present(shown, "pl"))
    }

    @Test fun presentationKeepsActualModelPreferredSpellingInsteadOfAssumingEveryCapitalIsWrong() {
        val source = SwipeSurfaceVariants.expand(listOf("praca", "pracą", "pralka"),
            listOf(220, 180, 120), null, sourceProvider(), false, false)
        val shown = SwipeSurfaceVariants.present(HerbertLiveSlate.ordered(source,
            listOf("praca", "Praca", "Pracą", "pracą")), "pl")
        assertEquals(listOf("praca", "Pracą", "pralka", "Praca", "pracą"), shown.words)
        assertNull(shown.languages)
    }

    @Test fun namedWinnerIsStillFirstButUnusedCasesCannotHideOtherDecodedWords() {
        val source = SwipeSurfaceVariants.expand(listOf("Malina", "maliną", "mamoną", "malinę"),
            listOf(220, 160, 120, 90), List(4) { "pl" }, sourceProvider(), false, false)
        val shown = SwipeSurfaceVariants.present(HerbertLiveSlate.ordered(source,
            listOf("Maliną", "maliną", "Malina", "malina")), "pl")
        assertEquals(listOf("Maliną", "Malina", "mamoną", "maliną", "malina", "malinę"), shown.words)
        assertEquals(listOf(160, 220, 120, 160, 220, 90), shown.scores)
    }

    @Test fun loneCasePairLeavesTwoOtherDecoderChoicesBeforeItsAlternate() {
        // Use an actual entry in this verbatim v5 fixture; it does not contain łódź.
        val source = SwipeSurfaceVariants.expand(listOf("laska", "kosz", "luz", "licz"),
            listOf(190, 129, 90, 80), List(4) { "pl" }, sourceProvider(), false, false)
        assertEquals(2, source.formGroupSize)
        assertEquals(listOf("laska", "Laska"), HerbertLiveSlate.group(source, sourceProvider())!!.surfaces)
        val shown = SwipeSurfaceVariants.present(HerbertLiveSlate.ordered(source, listOf("Laska", "laska")), "pl")
        assertEquals(listOf("Laska", "kosz", "luz", "laska", "licz"), shown.words)
        assertEquals(listOf(190, 129, 90, 190, 80), shown.scores)
        assertEquals(listOf(true, false, false, true, false), shown.exactCase)
    }

    @Test fun caseDisplayOffOrdinaryFormsAndForeignSlatesAreNotRewritten() {
        val ordinary = SwipeSurfaceVariants.expand(listOf("praca", "pracą", "pralka"),
            listOf(220, 180, 120), null, sourceProvider(), false, false, showCaseVariants = false)
        assertEquals(ordinary, SwipeSurfaceVariants.present(ordinary, "pl"))
        val source = SwipeSurfaceVariants.expand(listOf("praca", "pracą", "pralka"),
            listOf(220, 180, 120), List(3) { "pl" }, sourceProvider(), false, false)
        assertEquals(source, SwipeSurfaceVariants.present(source, "en"))
        assertEquals(source, SwipeSurfaceVariants.present(source, null))
        val foreign = source.copy(languages = List(source.words.size) { "en" })
        assertEquals(foreign, SwipeSurfaceVariants.present(foreign, "pl"))
    }

    @Test fun shortSlatesAndForeignRemainderKeepEverySelectableSurfaceAndParallelIdentity() {
        val source = SwipeSurfaceVariants.expand(listOf("praca", "pracą", "Praca"),
            listOf(220, 180, 120), listOf("pl", "pl", "en"), sourceProvider(), false, false)
        val shown = SwipeSurfaceVariants.present(source, "pl")
        assertEquals(listOf("praca", "pracą", "Praca", "Praca", "Pracą"), shown.words)
        assertEquals(listOf("pl", "pl", "en", "pl", "pl"), shown.languages)
        assertEquals(listOf(220, 180, 120, 220, 180), shown.scores)
        val pairOnly = source.copy(words = listOf("praca", "Praca"), scores = listOf(220, 220),
            languages = null, exactCase = listOf(true, true), formGroupSize = 2)
        assertEquals(pairOnly.words, SwipeSurfaceVariants.present(pairOnly, "pl").words)
        assertNull(HerbertLiveSlate.pair(SwipeSurfaceVariants.present(pairOnly, "pl")))
        for (bad in listOf(source.copy(formGroupSize = -1), source.copy(formGroupSize = 9),
            source.copy(scores = emptyList()), source.copy(exactCase = emptyList()),
            source.copy(languages = emptyList()))) assertEquals(bad, SwipeSurfaceVariants.present(bad, "pl"))
    }

    @Test fun allFourSurfaceRankingsKeepWinnerAllCandidatesAndUnrelatedDecoderOrder() {
        val source = SwipeSurfaceVariants.expand(listOf("praca", "pracą", "pralka", "prawda", "pranie"),
            listOf(220, 180, 120, 90, 70), List(5) { "pl" }, sourceProvider(), false, false)
        fun permutations(words: List<String>): List<List<String>> = if (words.isEmpty()) listOf(emptyList())
            else words.flatMap { word -> permutations(words - word).map { listOf(word) + it } }
        for (ranking in permutations(source.words.take(4))) {
            val ranked = HerbertLiveSlate.ordered(source, ranking)
            val shown = SwipeSurfaceVariants.present(ranked, "pl")
            assertEquals(ranking.first(), shown.words.first())
            assertEquals(3, shown.words.take(3).map { it.lowercase(java.util.Locale.ROOT) }.distinct().size)
            assertEquals(source.words.toSet(), shown.words.toSet())
            assertEquals(source.words.size, shown.words.size)
            assertEquals(listOf("pralka", "prawda", "pranie"), shown.words.filter { it in listOf("pralka", "prawda", "pranie") })
            for (i in shown.words.indices) {
                val old = source.words.indexOf(shown.words[i])
                assertEquals(source.scores[old], shown.scores[i])
                assertEquals(source.languages!![old], shown.languages!![i])
                assertEquals(source.exactCase[old], shown.exactCase[i])
            }
        }
    }

    @Test fun ordinaryInflectionCanWinWithoutAnyExactCapitalizationFlags() {
        val ordinary = SwipeSurfaceVariants.expand(listOf("kapitalizacją", "kapitalizacja", "kapitalizm"),
            listOf(220, 190, 90), listOf("pl", "pl", "pl"), sourceProvider(), false, false)
        val result = HerbertSwipeDecision(ordinary, HerbertLiveSlate.group(ordinary)!!, identity, 100)
            .finish(mapOf("kapitalizacją" to -8f, "kapitalizacja" to -1f), identity, 99)!!
        assertEquals(listOf("kapitalizacja", "kapitalizacją", "kapitalizm"), result.words)
        assertEquals(listOf(190, 220, 90), result.scores)
        assertEquals(ordinary.exactCase, result.exactCase)
        assertEquals(2, result.formGroupSize)
        assertNull(HerbertLiveSlate.group(ordinary.copy(formGroupSize = 3)))
        assertNull(HerbertLiveSlate.group(ordinary.copy(formGroupSize = 7)))
        assertNull(HerbertLiveSlate.group(ordinary.copy(formGroupSize = -1)))
        assertNull(HerbertLiveSlate.group(ordinary.copy(words = ordinary.words.take(2),
            scores = ordinary.scores.take(2), languages = ordinary.languages!!.take(2),
            exactCase = ordinary.exactCase.take(2), formGroupSize = 3)))
    }

    @Test fun differentEndingsRequireActualSharedSourceLemmaAndKeepAlternativeWeights() {
        val provider = sourceProvider()
        val forms = SwipeSurfaceVariants.expand(listOf("pracy", "praca", "malina"),
            listOf(220, 190, 90), null, provider, false, false)
        assertNull(HerbertLiveSlate.group(forms))
        val result = HerbertSwipeDecision(forms, HerbertLiveSlate.group(forms, provider)!!, identity, 100)
            .finish(mapOf("pracy" to -7f, "Pracy" to -8f, "praca" to -1f, "Praca" to -6f), identity, 99)!!
        assertEquals(listOf("praca", "Praca", "pracy", "Pracy", "malina"), result.words)
        assertEquals(listOf(190, 190, 220, 220, 90), result.scores)
        assertEquals(forms.exactCase, result.exactCase)
    }

    @Test fun ordinaryFormTimeoutAndStaleEditorStillCannotRewriteCommittedText() {
        val value = SwipeSurfaceVariants.expand(listOf("kapitalizacją", "kapitalizacja"),
            listOf(220, 190), null, sourceProvider(), false, false)
        val group = HerbertLiveSlate.group(value)!!
        val scores = mapOf("kapitalizacją" to -8f, "kapitalizacja" to -1f)
        val request = HerbertSwipeDecision(value, group, identity, 100)
        assertEquals(value, request.finish(null, identity, 1))
        assertNull(request.finish(scores, identity, 2))
        assertEquals(value, HerbertSwipeDecision(value, group, identity, 100).finish(scores, identity, 100))
        assertNull(HerbertSwipeDecision(value, group, identity, 100).finish(scores, identity.copy(context = "edited"), 1))
    }
    private val family = SwipeSurfaceVariants.Slate(
        listOf("Malina", "malina", "maliną", "Maliną", "mamoną", "Maliną"),
        listOf(220, 220, 160, 160, 190, 150), listOf("pl", "pl", "pl", "pl", "pl", "en"),
        listOf(true, true, true, true, false, false))

    @Test fun familyWinnerTravelsWithItsOwnWeightAndAllOtherKeysAndLanguagesStayIntact() {
        val decision = HerbertSwipeDecision(family, HerbertLiveSlate.group(family)!!, identity, 100)
        val result = decision.finish(mapOf("Malina" to -6f, "malina" to -8f,
            "maliną" to -3f, "Maliną" to -1f), identity, 99)!!
        assertEquals(listOf("Maliną", "maliną", "Malina", "malina", "mamoną", "Maliną"), result.words)
        assertEquals(listOf(160, 160, 220, 220, 190, 150), result.scores)
        assertEquals(family.languages, result.languages)
        assertEquals(family.exactCase, result.exactCase)
        assertNull(decision.finish(null, identity, 99))
    }

    @Test fun familyMissingOrInvalidScoreTimeoutNextTouchAndStaleStateKeepTheOneShotGuards() {
        val valid = mapOf("Malina" to -6f, "malina" to -8f, "maliną" to -3f, "Maliną" to -1f)
        for (scores in listOf(null, valid - "Maliną", valid + ("Maliną" to Float.POSITIVE_INFINITY),
            valid + ("mamoną" to -1f), valid.mapValues { -1f })) {
            val decision = HerbertSwipeDecision(family, HerbertLiveSlate.group(family)!!, identity, 100)
            assertEquals(family, decision.finish(scores, identity, 99))
            assertNull(decision.finish(valid, identity, 99))
        }
        assertEquals(family, HerbertSwipeDecision(family, HerbertLiveSlate.group(family)!!,
            identity, 100).finish(valid, identity, 100))
        assertNull(HerbertSwipeDecision(family, HerbertLiveSlate.group(family)!!,
            identity, 100).finish(valid, identity.copy(textRevision = 99), 1))
    }

    @Test fun familyGateRejectsWrongKeysLanguagesFlagsSizesAndMismatchedSiblingScores() {
        for (value in listOf(family.copy(words = family.words.toMutableList().apply { set(2, "mamoną") }),
            family.copy(languages = listOf("pl", "pl", "en", "en", "pl", "en")),
            family.copy(exactCase = listOf(true, true, false, true, false, false)),
            family.copy(exactCase = listOf(true, true, true, true, true, false)),
            family.copy(exactCase = emptyList()), family.copy(scores = emptyList()),
            family.copy(languages = listOf("pl")),
            family.copy(scores = listOf(220, 219, 160, 160, 190, 150)))) {
            assertNull(HerbertLiveSlate.group(value))
        }
    }

    @Test fun previouslyEligibleSingleKeyPairsIncludingHyphenatedWordsStayEligible() {
        for (key in listOf("łódź", "malina", "kowalska-nowak")) {
            val value = SwipeSurfaceVariants.Slate(listOf(key, key.replaceFirstChar { it.titlecase() }, "kosz"),
                listOf(190, 190, 129), listOf("pl", "pl", "pl"), listOf(true, true, false))
            assertEquals(HerbertLiveSlate.pair(value)!!.surfaces, HerbertLiveSlate.group(value)!!.surfaces)
        }
    }

    private val slate = SwipeSurfaceVariants.Slate(listOf("łódź", "Łódź", "kosz"),
        listOf(190, 190, 129), listOf("pl", "pl", "pl"), listOf(true, true, false))
    private val pair = HerbertLiveSlate.pair(slate)!!
    private val identity = HerbertRequestIdentity(1, 2, 30, 30, 3, "pl", "pack-v5", 4, "Jedziemy do ")
    private val scores = mapOf("łódź" to -8f, "Łódź" to -2f)
    private fun request() = HerbertSwipeDecision(slate, pair, identity, 100)

    @Test fun onlyFirstPairMovesAndAllParallelIdentityIsPreserved() {
        val result = request().finish(scores, identity, 99)!!
        assertEquals(listOf("Łódź", "łódź", "kosz"), result.words)
        assertEquals(slate.scores, result.scores)
        assertEquals(slate.languages, result.languages)
        assertEquals(slate.exactCase, result.exactCase)
    }
    @Test fun deadlineIncludingExactBoundaryKeepsTheSourceOrder() {
        for (now in listOf(100L, 101L)) assertEquals(slate, request().finish(scores, identity, now))
    }
    @Test fun nextTouchOrFailedScoreCommitsBaselineOnceThenIgnoresNativeCallback() {
        val request = request()
        assertEquals(slate, request.finish(null, identity, 1))
        assertNull(request.finish(scores, identity, 2))
    }
    @Test fun staleEditorOrCancellationNeverCommitsEvenFallback() {
        for (current in listOf(null, identity.copy(editorSession = 9), identity.copy(textRevision = 9),
            identity.copy(selectionStart = 29), identity.copy(selectionEnd = 29), identity.copy(requestId = 9),
            identity.copy(language = "en"), identity.copy(packIdentity = "new"),
            identity.copy(settingsRevision = 9), identity.copy(context = "changed"))) {
            assertNull(request().finish(scores, current, 1))
        }
        val request = request(); request.cancel()
        assertNull(request.finish(null, identity, 1))
    }
    @Test fun stableTiesInvalidScoresAndUnknownSurfacesKeepBaseline() {
        for (value in listOf(mapOf("łódź" to -2f, "Łódź" to -2f),
            mapOf("łódź" to Float.NaN, "Łódź" to -1f), mapOf("łódź" to -2f, "Lodz" to -1f))) {
            assertEquals(slate, request().finish(value, identity, 1))
        }
    }
    @Test fun malformedOrUnalignedSlatesNeverReachTheModel() {
        for (value in listOf(slate.copy(exactCase = listOf(false, false, false)),
            slate.copy(words = listOf("łódź", "kosz", "luz")),
            slate.copy(scores = listOf(190, 180, 129)), slate.copy(scores = emptyList()),
            slate.copy(languages = listOf("pl", "en", "pl")),
            slate.copy(exactCase = listOf(true, true, true)))) assertNull(HerbertLiveSlate.pair(value))
    }
}
