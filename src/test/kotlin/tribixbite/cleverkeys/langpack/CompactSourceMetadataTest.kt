package tribixbite.cleverkeys.langpack

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class CompactSourceMetadataTest {
    private fun fixture() = SharedLanguageIntelligenceTest().fixture()
        .replace("\"schemaVersion\":2", "\"schemaVersion\":3")
        .replace("\"surfaces\":[\"Malina\"]", "\"surfaceMask\":2")
        .replace("\"surfaces\":[\"malina\"]", "\"surfaceMask\":1")
        .replace("\"surfaces\":[\"Maliną\"]", "\"surfaceMask\":2")
    private fun parse(text: String = fixture(), old: LanguageIntelligenceProvider? = null) =
        IntelligenceJson.parse(text.reader(), "pl", 11, setOf("metadata", "capitalization"), old, 3)
    private fun refused(text: String) {
        try { parse(text); fail("Invalid compact source accepted") } catch (_: Exception) { }
    }

    @Test fun exactLegacyViewsAndSourceIdentitiesSurviveUpgradeAndRepeat() {
        val legacy = IntelligenceJson.parse(SharedLanguageIntelligenceTest().fixture().reader(), "pl", 10,
            setOf("metadata", "capitalization"), null, 2)
        val cold = parse(); val upgraded = parse(old = legacy); val repeated = parse(old = upgraded)
        for (key in listOf("malina", "maliną")) {
            assertEquals(legacy.lookup(key), cold.lookup(key))
            assertSame(legacy.lookup(key), upgraded.lookup(key))
            assertSame(upgraded.lookup(key), repeated.lookup(key))
        }
        assertEquals(11, upgraded.packageInfo().version)
        assertSame(legacy.sharedSources, upgraded.sharedSources)
    }

    @Test fun masksPreserveUpperTitleLowerAndDeterministicMultiFormOrdering() {
        val data = parse(fixture().replace("\"surfaceMask\":1", "\"surfaceMask\":7"))
        val rows = JsonParser.parseString(data.lookup("malina")!!.metadataJson).asJsonObject
            .getAsJsonObject("sourceEvidence").getAsJsonArray("lexicalReadings")
        assertEquals("[\"MALINA\",\"Malina\",\"malina\"]", rows[1].asJsonObject.get("surfaces").toString())
        assertEquals("[\"Maliną\"]", JsonParser.parseString(data.lookup("maliną")!!.metadataJson).asJsonObject
            .getAsJsonObject("sourceEvidence").getAsJsonArray("lexicalReadings")[0].asJsonObject.get("surfaces").toString())
    }

    @Test fun invalidMasksConflictingSurfacesWrongSchemaAndExpandedBudgetRefuse() {
        for (value in listOf("0", "8", "-1", "0.5", "true", "\"2\""))
            refused(fixture().replace("\"surfaceMask\":2", "\"surfaceMask\":$value"))
        refused(fixture().replace("\"surfaceMask\":2", "\"surfaceMask\":2,\"surfaces\":[\"Malina\"]"))
        try {
            IntelligenceJson.parse(fixture().replace("\"schemaVersion\":3", "\"schemaVersion\":2").reader(),
                "pl", 11, setOf("metadata"), null, 2)
            fail("Schema2 source refs without literal surfaces accepted")
        } catch (_: Exception) { }
        val obj = IntelligenceJson.document(fixture().reader())
        val rows = com.google.gson.JsonArray()
        repeat(2500) { rows.add(JsonParser.parseString("""{"sourceRef":0,"surfaceMask":7}""")) }
        obj.getAsJsonArray("entries")[0].asJsonObject.getAsJsonObject("metadata")
            .getAsJsonObject("sourceEvidence").add("lexicalReadings", rows)
        refused(obj.toString())
    }

    @Test fun appendedSourceDefinitionsKeepEarlierSnapshotAndSemanticEntriesReusable() {
        val old = parse()
        val obj = IntelligenceJson.document(fixture().reader())
        obj.getAsJsonArray("sourceLexemes").add(JsonParser.parseString("""{"lemma":"inne","partOfSpeech":"adj"}"""))
        val next = parse(obj.toString(), old)
        assertNotSame(old.sharedSources, next.sharedSources)
        assertTrue(next.sharedSources!!.extendsTables(old.sharedSources!!))
        assertSame(old.lookup("malina"), next.lookup("malina"))
        val newer = IntelligenceJson.document(obj.toString().reader())
        newer.getAsJsonArray("sourceLexemes").add(JsonParser.parseString("""{"lemma":"nowe","partOfSpeech":"adj"}"""))
        val third = parse(newer.toString(), next)
        assertSame(old.lookup("malina"), third.lookup("malina"))
        assertTrue(third.sharedSources!!.extendsTables(old.sharedSources!!))
    }

    @Test fun changedSourceAttributesNeverReuseOldSemanticValue() {
        val old = parse(); val before = old.lookup("malina")!!.metadataJson
        val changed = fixture().replace("\"nazwisko\"", "\"imię\"")
        val next = parse(changed, old)
        assertNotSame(old.lookup("malina"), next.lookup("malina"))
        assertNotEquals(before, next.lookup("malina")!!.metadataJson)
        assertEquals(before, old.lookup("malina")!!.metadataJson)
        try { parse(fixture() + " {}", old); fail("Trailing content accepted") } catch (_: Exception) { }
        assertEquals(before, old.lookup("malina")!!.metadataJson)
    }

    @Test fun schema2OpaqueMaskNamedFieldsRemainOpaqueAndSchema3MustBeDeclared() {
        val text = """{"schemaVersion":2,"languageCode":"pl","sourceAttributes":[],"sourceLexemes":[],"entries":[{"surfaceKey":"a","metadata":{"sourceEvidence":{"lexicalReadings":[{"lemma":"a","partOfSpeech":"subst","surfaces":["a"],"surfaceMask":"opaque"}]}}}]}"""
        val value = IntelligenceJson.parse(text.reader(), "pl", 10, setOf("metadata"), null, 2)
        assertTrue(value.lookup("a")!!.metadataJson!!.contains("\"surfaceMask\":\"opaque\""))
        val hash = "a".repeat(64)
        val declaration = """{"apiVersion":3,"languageIntelligence":{"file":"language-intelligence.json","schemaVersion":3,"sha256":"$hash"}}"""
        assertEquals(3, IntelligenceJson.declaration(IntelligenceJson.document(declaration.reader()))!!.schemaVersion)
        try { IntelligenceJson.declaration(IntelligenceJson.document(declaration.replace("\"apiVersion\":3", "\"apiVersion\":2").reader())); fail("Mismatched API accepted") }
        catch (_: IllegalArgumentException) { }
    }
}
