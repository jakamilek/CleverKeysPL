package tribixbite.cleverkeys.langpack

import org.junit.Assert.*
import org.junit.Test
import tribixbite.cleverkeys.SwipeSurfaceVariants

class LanguageIntelligenceTest {
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
