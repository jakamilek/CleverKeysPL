package tribixbite.cleverkeys.langpack

import org.junit.Assert.*
import org.junit.Test
import tribixbite.cleverkeys.SwipeSurfaceVariants

class LanguageIntelligenceTest {
    private fun familyProvider(): LanguageIntelligenceProvider = IntelligenceJson.parse(
        javaClass.getResource("/polish-surface-family-v5.json")!!.readText(Charsets.UTF_8).reader(),
        "pl", 5, setOf("capitalization", "metadata"))

    @Test fun ordinaryDecodedFormsDoNotNeedCapitalizationMetadataAndNeverInventCaseVariants() {
        val data = familyProvider()
        assertNull(data.lookup("kapitalizacja"))
        val result = SwipeSurfaceVariants.expand(listOf("kapitalizacją", "kapitalizacja", "kapitalizm"),
            listOf(220, 190, 90), listOf("pl", "pl", "pl"), data, false, false)
        assertEquals(listOf("kapitalizacją", "kapitalizacja", "kapitalizm"), result.words)
        assertEquals(listOf(220, 190, 90), result.scores)
        assertEquals(listOf(false, false, false), result.exactCase)
        assertEquals(2, result.formGroupSize)
        assertEquals(1, SwipeSurfaceVariants.expand(listOf("kapitalizacją", "kapitalizm"),
            listOf(220, 190), null, data, false, false).formGroupSize)
    }

    @Test fun sourceLemmaLinksActualInflectionsWithoutRemovingLettersOrGuessingStems() {
        val data = familyProvider()
        assertTrue(data.sharesSourceLemma("praca", "pracy"))
        assertFalse(data.sharesSourceLemma("praca", "malina"))
        assertFalse(data.sharesSourceLemma("kapitalizacja", "kapitalizacji"))
        assertTrue(data.lookup("pracy")!!.sourceLemmas.contains(SourceLemma("praca", "subst")))
        val result = SwipeSurfaceVariants.expand(listOf("pracy", "malina", "praca"),
            listOf(220, 190, 160), null, data, false, false)
        assertEquals(listOf("pracy", "Pracy", "praca", "Praca", "malina"), result.words)
        assertEquals(listOf(220, 220, 160, 160, 190), result.scores)
        assertEquals(4, result.formGroupSize)
    }

    @Test fun lemmaRelationRequiresDeclaredMetadataAndImmutableSurfaceMatchedEvidence() {
        val json = javaClass.getResource("/polish-surface-family-v5.json")!!.readText(Charsets.UTF_8)
        val capsOnly = IntelligenceJson.parse(json.reader(), "pl", 5, setOf("capitalization"))
        assertFalse(capsOnly.sharesSourceLemma("praca", "pracy"))
        val provider = familyProvider()
        try {
            (provider.lookup("praca")!!.sourceLemmas as MutableSet<SourceLemma>).clear()
            fail("source identities must be immutable")
        } catch (_: UnsupportedOperationException) { }
        assertTrue(provider.sharesSourceLemma("praca", "pracy"))
        val root = IntelligenceJson.document(json.reader())
        for (entry in root.getAsJsonArray("entries")) {
            val obj = entry.asJsonObject
            if (obj.get("surfaceKey").asString != "pracy") continue
            for (reading in obj.getAsJsonObject("metadata").getAsJsonObject("sourceEvidence")
                .getAsJsonArray("lexicalReadings")) {
                reading.asJsonObject.addProperty("partOfSpeech", "other-pos")
            }
        }
        val changedPos = IntelligenceJson.parse(root.toString().reader(), "pl", 5, setOf("metadata"))
        assertFalse(changedPos.sharesSourceLemma("praca", "pracy"))
    }

    @Test fun capitalizationDisplayToggleDoesNotTurnOffInflectionGroupsOrDeleteDecodedCaseChoices() {
        val data = familyProvider()
        val result = SwipeSurfaceVariants.expand(listOf("pracy", "Pracy", "praca"),
            listOf(220, 200, 160), null, data, false, false, showCaseVariants = false)
        assertEquals(listOf("pracy", "praca", "Pracy"), result.words)
        assertEquals(listOf(220, 160, 200), result.scores)
        assertEquals(listOf(false, false, false), result.exactCase)
        assertEquals(2, result.formGroupSize)
        assertEquals(listOf("malina", "Malina", "maliną"), SwipeSurfaceVariants.expand(
            listOf("malina", "Malina", "maliną"), listOf(220, 200, 160), null, data,
            false, true, showCaseVariants = false).words)
    }

    @Test fun anotherLanguageAndAbsentDecodedInflectionNeverEnterOrdinaryGroups() {
        val data = familyProvider()
        val result = SwipeSurfaceVariants.expand(listOf("kapitalizacją", "kapitalizacja"),
            listOf(220, 190), listOf("pl", "en"), data, false, false)
        assertEquals(1, result.formGroupSize)
        assertEquals(listOf("kapitalizacją", "kapitalizacja"), result.words)
        assertEquals(listOf("praca", "Praca"), SwipeSurfaceVariants.expand(listOf("praca"),
            listOf(220), null, data, false, false).words)
    }

    @Test fun decodedDiacriticAlternativeExpandsUsingActualV5SourceAndKeepsBothWeights() {
        val data = familyProvider()
        assertTrue(data.lookup("maliną")!!.metadataJson!!.contains("Malina:Sf"))
        val slate = SwipeSurfaceVariants.expand(listOf("Malina", "mamoną", "maliną", "Maliną"),
            listOf(220, 190, 160, 150), listOf("pl", "pl", "pl", "pl"), data, false, false)
        assertEquals(listOf("Malina", "malina", "maliną", "Maliną", "mamoną"), slate.words)
        assertEquals(listOf(220, 220, 160, 160, 190), slate.scores)
        assertEquals(listOf(true, true, true, true, false), slate.exactCase)
        assertEquals(List(5) { "pl" }, slate.languages)
    }

    @Test fun groupNeverInventsMissingDecodedOrSourceFormsAndIgnoresAnotherLanguage() {
        val data = familyProvider()
        assertEquals(listOf("malina", "Malina", "mamoną"), SwipeSurfaceVariants.expand(
            listOf("malina", "mamoną"), listOf(100, 90), null, data, false, false).words)
        assertEquals(listOf("malina", "Malina", "maliną"), SwipeSurfaceVariants.expand(
            listOf("malina", "maliną"), listOf(100, 90), listOf("pl", "en"), data, false, false).words)
        assertEquals(listOf(true, true, false), SwipeSurfaceVariants.expand(
            listOf("malina", "maliną"), listOf(100, 90), null, provider(), false, false).exactCase)
        assertEquals(listOf("malina", "maliną"), SwipeSurfaceVariants.expand(
            listOf("malina", "maliną"), listOf(100, 90), null, null, false, false).words)
    }

    @Test fun searchAndSurfaceBoundsKeepOtherDecodedKeysInTheirExistingOrder() {
        val data = familyProvider()
        val bounded = SwipeSurfaceVariants.expand(listOf("laska", "łaska", "laską", "łaską"),
            listOf(100, 90, 80, 70), null, data, false, false)
        assertEquals(listOf("laska", "Laska", "łaska", "Łaska", "laską", "łaską"), bounded.words)
        assertEquals(listOf(true, true, true, true, false, false), bounded.exactCase)
        assertEquals(listOf(100, 100, 90, 90, 80, 70), bounded.scores)
        val far = listOf("malina", "kot", "dom", "pies", "mamoną", "maliną")
        assertEquals(listOf("malina", "Malina") + far.drop(1), SwipeSurfaceVariants.expand(
            far, listOf(100, 90, 80, 70, 60, 50), null, data, false, false).words)
    }

    private fun provider(json: String = fixture()): LanguageIntelligenceProvider =
        IntelligenceJson.parse(json.reader(), "pl", 3, setOf("capitalization", "metadata"))

    private fun fixture(): String = javaClass.getResource("/language-intelligence-trial.json")!!
        .readText(Charsets.UTF_8)

    @Test fun realProducerFixtureRetainsNineKeysAndSourceEvidence() {
        val data = provider()
        assertEquals("łódź", data.lookup("ŁÓDŹ")!!.surfaceKey)
        assertNull(data.lookup("lodz"))
        assertEquals(listOf("łódź", "Łódź"), data.lookup("łódź")!!.capitalization!!.variants.map { it.surface })
        assertEquals(1, data.lookup("łódzki")!!.capitalization!!.variants.size)
        for (key in listOf("jagoda", "malina", "polska", "róża", "warszawska", "warszawski", "łodzi", "łódzki", "łódź")) {
            assertNotNull(data.lookup(key))
            assertTrue(data.lookup(key)!!.metadataJson!!.contains("generatedFormProofs"))
        }
        assertTrue(data.packageInfo().provenanceJson!!.contains("sgjp-2026.06.01"))
        assertTrue(data.capabilities().contains("capitalization"))
    }

    @Test fun twoFormsShareOneScoreAndNextDistinctCandidateIsThird() {
        for (key in listOf("łódź", "malina", "warszawska", "łodzi")) {
            val slate = SwipeSurfaceVariants.expand(listOf(key, key.replaceFirstChar { it.titlecase() }, "lód"),
                listOf(100, 95, 80), listOf("pl", "pl", "pl"), provider(), false, false)
            assertEquals(listOf(key, key.replaceFirstChar { it.titlecase() }, "lód"), slate.words)
            assertEquals(listOf(100, 100, 80), slate.scores)
            assertEquals(listOf("pl", "pl", "pl"), slate.languages)
            assertEquals(listOf(true, true, false), slate.exactCase)
        }
    }

    @Test fun sentenceStartKeepsLowercaseAvailableAndUserCaseIsRespected() {
        val slate = SwipeSurfaceVariants.expand(listOf("Łódź", "Lód"), listOf(100, 80), null, provider(), true, false)
        assertEquals(listOf("Łódź", "łódź", "Lód"), slate.words)
        assertEquals(listOf("Malina", "malina", "lód"), SwipeSurfaceVariants.expand(
            listOf("Malina", "lód"), listOf(100, 80), null, provider(), false, false).words)
    }

    @Test fun absentMetadataAndCapsLockKeepExistingSlate() {
        for (word in listOf("nieznane", "ŁÓDŹ")) {
            val slate = SwipeSurfaceVariants.expand(listOf(word, "lód"), listOf(100, 80), null,
                provider(), false, word == "ŁÓDŹ")
            assertEquals(listOf(word, "lód"), slate.words)
            assertEquals(listOf(false, false), slate.exactCase)
        }
        assertEquals(listOf("łódź"), SwipeSurfaceVariants.expand(listOf("łódź"), emptyList(), null,
            null, false, false).words)
    }

    @Test fun adjectiveStaysSingleAndAnotherLanguageIsNotRemoved() {
        assertEquals(listOf("łódzki", "lód"), SwipeSurfaceVariants.expand(listOf("łódzki", "lód"),
            listOf(100, 80), null, provider(), false, false).words)
        assertEquals(listOf("Łódzki", "Lód"), SwipeSurfaceVariants.expand(listOf("Łódzki", "Lód"),
            listOf(100, 80), null, provider(), true, false).words)
        val slate = SwipeSurfaceVariants.expand(listOf("malina", "Malina", "lód"), listOf(100, 90, 80),
            listOf("pl", "en", "pl"), provider(), false, false)
        assertEquals(listOf("pl", "pl", "en", "pl"), slate.languages)
    }

    private fun refused(json: String) {
        try { provider(json); fail("invalid sidecar accepted") } catch (_: IllegalArgumentException) { }
    }

    @Test fun invalidSchemaLanguageDuplicateKeysAndCasePolicyAreRejected() {
        refused(fixture().replace("\"schemaVersion\":1", "\"schemaVersion\":2"))
        refused(fixture().replace("\"languageCode\":\"pl\"", "\"languageCode\":\"en\""))
        refused(fixture().replace("\"surfaceKey\":\"malina\"", "\"surfaceKey\":\"jagoda\""))
        refused(fixture().replace("\"casePolicy\":\"capitalized\"", "\"casePolicy\":\"invented\""))
        refused(fixture().replace("\"surfaceKey\":\"łódź\"", "\"surfaceKey\":\"Łódź\""))
        refused(fixture().replace("\"defaultSurface\":\"łódź\"", "\"defaultSurface\":\"lód\""))
    }

    @Test fun duplicateJsonMembersAndTrailingContentAreRejected() {
        for (json in listOf("{\"a\":1,\"a\":2}", "{} {}", "{/*comment*/}")) {
            try { IntelligenceJson.document(json.reader()); fail("invalid JSON accepted") } catch (_: Exception) { }
        }
    }

    @Test fun legacyManifestAndFutureCapabilitiesAreCompatibleButUnsupportedApiIsNot() {
        assertNull(IntelligenceJson.declaration(IntelligenceJson.document("{}".reader())))
        assertEquals(setOf("future"), IntelligenceJson.capabilities(
            IntelligenceJson.document("{\"capabilities\":[\"future\"]}".reader())))
        try {
            IntelligenceJson.declaration(IntelligenceJson.document("{\"apiVersion\":2}".reader()))
            fail("unsupported API accepted")
        } catch (_: IllegalArgumentException) { }
    }
}
