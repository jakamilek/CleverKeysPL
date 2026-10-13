package tribixbite.cleverkeys.langpack

import org.junit.Assert.*
import org.junit.Test
import tribixbite.cleverkeys.SwipeSurfaceVariants

class SharedLanguageIntelligenceTest {
    internal fun fixture(): String = """{"schemaVersion":2,"languageCode":"pl",
      "sourceAttributes":[{"labels":[],"nameClasses":["nazwisko"]}],
      "sourceLexemes":[{"lemma":"Malina:Sf","partOfSpeech":"subst","sourceAttributesRef":0}],
      "entries":[{"surfaceKey":"malina","canonicalForm":"malina",
        "capitalization":{"defaultSurface":"malina","variants":[{"surface":"malina","casePolicy":"lowercase"},{"surface":"Malina","casePolicy":"capitalized"}]},
        "metadata":{"sourceEvidence":{"lexicalReadings":[{"sourceRef":0,"surfaces":["Malina"]},{"lemma":"malina","partOfSpeech":"subst","surfaces":["malina"]}]}}},
       {"surfaceKey":"maliną","metadata":{"sourceEvidence":{"lexicalReadings":[{"sourceRef":0,"surfaces":["Maliną"]}]}}}]}"""

    private fun parse(json: String = fixture(), previous: LanguageIntelligenceProvider? = null) =
        IntelligenceJson.parse(json.reader(), "pl", 9, setOf("metadata", "capitalization"), previous, 2)
    private fun refused(json: String) {
        try { parse(json); fail("Invalid shared metadata accepted") } catch (_: Exception) { }
    }

    @Test fun sharedRecordsRestoreExactCaseSensitiveEvidenceAndOrdinaryHomonyms() {
        val data = parse()
        val json = data.lookup("Malina")!!.metadataJson!!
        assertEquals("""{"sourceEvidence":{"lexicalReadings":[{"labels":[],"lemma":"Malina:Sf","nameClasses":["nazwisko"],"partOfSpeech":"subst","surfaces":["Malina"]},{"lemma":"malina","partOfSpeech":"subst","surfaces":["malina"]}]}}""", json)
        assertEquals(setOf(SourceLemma("Malina:Sf", "subst"), SourceLemma("malina", "subst")), data.lookup("malina")!!.sourceLemmas)
        assertTrue(data.sharesSourceLemma("malina", "maliną"))
        assertEquals(listOf("malina", "Malina", "maliną"), SwipeSurfaceVariants.expand(
            listOf("malina", "maliną"), listOf(100, 80), null, data, false, false).words)
        assertEquals(json, data.lookup("malina")!!.metadataJson) // demand reads retain semantics.
    }

    @Test fun tableReuseStillValidatesNewSnapshotAndLanguage() {
        val old = parse()
        val next = parse(previous = old)
        assertSame(old.sharedSources, next.sharedSources)
        assertSame(old.lookup("malina"), next.lookup("malina"))
        assertEquals(9, next.packageInfo().version)
        refused(fixture() + " {}")
        refused(fixture().replace("\"pl\"", "\"en\""))
        assertEquals(old.lookup("malina")!!.metadataJson, next.lookup("malina")!!.metadataJson)
    }

    @Test fun declarationMustMatchApiAndSchemaAndLegacyRemainsSupported() {
        val hash = "a".repeat(64)
        val declaration = """{"apiVersion":2,"languageIntelligence":{"file":"language-intelligence.json","schemaVersion":2,"sha256":"$hash"}}"""
        assertEquals(2, IntelligenceJson.declaration(IntelligenceJson.document(declaration.reader()))!!.schemaVersion)
        for (bad in listOf(declaration.replace("\"apiVersion\":2", "\"apiVersion\":1"),
            declaration.replace("\"schemaVersion\":2", "\"schemaVersion\":1"), declaration.replace("\"apiVersion\":2", "\"apiVersion\":3"))) {
            try { IntelligenceJson.declaration(IntelligenceJson.document(bad.reader())); fail("mismatch accepted") }
            catch (_: IllegalArgumentException) { }
        }
        try { IntelligenceJson.parse(fixture().reader(), "pl", 9, setOf("metadata")); fail("schema was not declared") }
        catch (_: IllegalArgumentException) { }
        val legacy = IntelligenceJson.parse("""{"entries":[],"languageCode":"pl","schemaVersion":1}""".reader(), "pl", 8, setOf("metadata"))
        assertNull(legacy.sharedSources)
    }

    @Test fun invalidReferencesConflictsDuplicatesAndLateTablesAreRejected() {
        for (bad in listOf(
            fixture().replace("\"sourceRef\":0", "\"sourceRef\":-1"),
            fixture().replace("\"sourceRef\":0", "\"sourceRef\":1"),
            fixture().replace("\"sourceRef\":0", "\"sourceRef\":0.5"),
            fixture().replace("\"surfaces\":[\"Malina\"]", "\"surfaces\":[false]"),
            fixture().replace("\"surfaces\":[\"Malina\"]", "\"surfaces\":[]"),
            fixture().replace("\"sourceAttributesRef\":0", "\"sourceAttributesRef\":1"),
            fixture().replace("\"sourceRef\":0", "\"sourceRef\":0,\"lemma\":\"invented\""),
            fixture().replace("\"sourceAttributesRef\":0", "\"sourceAttributesRef\":0,\"labels\":[]"),
            fixture().replace("\"schemaVersion\":2", "\"schemaVersion\":2,\"schemaVersion\":2"),
            fixture().replace("\"labels\":[]", "\"labels\":[0]"))) refused(bad)
        val obj = IntelligenceJson.document(fixture().reader())
        val records = obj.getAsJsonArray("sourceLexemes")
        records.add(records[0].deepCopy()); refused(obj.toString())
        val late = """{"schemaVersion":2,"languageCode":"pl","entries":[],"sourceAttributes":[],"sourceLexemes":[]}"""
        refused(late)
        val missing = """{"schemaVersion":2,"languageCode":"pl","sourceAttributes":[],"entries":[]}"""
        refused(missing)
    }

    @Test fun repeatedReferencesCannotBypassExpandedEntryBudget() {
        val obj = IntelligenceJson.document(fixture().reader())
        val arrays = com.google.gson.JsonArray()
        repeat(3000) { arrays.add(com.google.gson.JsonParser.parseString("""{"sourceRef":0,"surfaces":["Malina"]}""")) }
        obj.getAsJsonArray("entries")[0].asJsonObject.getAsJsonObject("metadata")
            .getAsJsonObject("sourceEvidence").add("lexicalReadings", arrays)
        refused(obj.toString()) // encoded ~12K nodes; expanded >16K.
    }
}
