package tribixbite.cleverkeys.ai

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/** Mandatory original fast-tokenizer fixtures from verified producer run 37230171787. */
class HerbertRealConformanceTest {
    private fun bytes(name: String): ByteArray {
        val directory = System.getProperty("herbertFixtureDir")
        val bytes = if (directory.isNullOrBlank()) {
            val path = "/herbert-fp32-benchmark-v1/$name" + if (name == "portable-tokenizer.json") ".gz" else ""
            val resource = javaClass.getResourceAsStream(path) ?: error("Missing original HerBERT fixture")
            if (name == "portable-tokenizer.json") GZIPInputStream(resource).use { it.readBytes() }
            else resource.use { it.readBytes() }
        } else File(directory, name).readBytes()
        val identity = HerbertBenchmarkTrial.trust().files.getValue(name)
        assertEquals(identity.bytes, bytes.size.toLong())
        assertEquals(identity.sha256, HerbertBundleImport.hex(MessageDigest.getInstance("SHA-256").digest(bytes)))
        return bytes
    }

    @Test fun originalFastTokenizerAndAll232PreparedBatchesMatch() {
        val tokenizer = ByteArrayInputStream(bytes("portable-tokenizer.json")).reader(Charsets.UTF_8).use { HerbertTokenizer.parse(it) }
        val tokenVectors = ByteArrayInputStream(bytes("tokenizer-conformance.json")).reader(Charsets.UTF_8).use {
            HerbertConformance.tokens(tokenizer, it)
        }
        assertEquals(2471, tokenVectors)
        val vectors = ByteArrayInputStream(bytes("android-score-vectors.json")).reader(Charsets.UTF_8).use {
            HerbertConformance.feeds(tokenizer, it)
        }
        assertEquals(232, vectors.size)
        assertEquals(532, vectors.sumOf { it.batch.size })
        println("HerBERT original conformance: 2471 token vectors; 232 batches / 532 candidates / five inputs PASS")
    }

    @Test fun fourFamilySurfacesHaveTheSameUnpaddedFeedsAsSeparateOriginalTokenizerRequests() {
        checkUnpaddedFeeds(listOf("malina", "Malina", "maliną", "Maliną"), "Jutro będę widział się z ")
    }

    @Test fun ordinaryAndSourceLemmaEndingsUseOriginalTokenizerWithoutUnknownTargetsOrLostMasks() {
        checkUnpaddedFeeds(listOf("kapitalizacją", "kapitalizacja"), "Tak, była w podpowiedziach właściwa ")
        checkUnpaddedFeeds(listOf("pracy", "Pracy", "praca", "Praca"), "To jest bardzo ważna ")
    }

    private fun checkUnpaddedFeeds(forms: List<String>, context: String) {
        val tokenizer = ByteArrayInputStream(bytes("portable-tokenizer.json")).reader(Charsets.UTF_8).use { HerbertTokenizer.parse(it) }
        val batch = tokenizer.prepare(context, forms, 32)
        assertEquals(forms.size, batch.size)
        assertEquals(forms, batch.surfaces)
        for ((row, surface) in forms.withIndex()) {
            val single = tokenizer.prepare(context, listOf(surface), 32)
            val ids = batch.inputIds(); val attention = batch.attentionMask()
            val active = (0 until batch.sequence).filter { attention[row * batch.sequence + it] == 1L }
                .map { ids[row * batch.sequence + it] }.toLongArray()
            assertArrayEquals(single.inputIds(), active)
            val mask = batch.targetMask(); val target = batch.targetTokenIds()
            val indices = (0 until batch.targets).filter { mask[row * batch.targets + it] == 1f }
            val targetIds = indices.map { target[row * batch.targets + it] }.toLongArray()
            assertArrayEquals(tokenizer.encode(surface), targetIds)
            assertArrayEquals(single.targetTokenIds(), targetIds)
            assertArrayEquals(single.targetPositions(), indices.map {
                batch.targetPositions()[row * batch.targets + it]
            }.toLongArray())
            assertTrue(indices.isNotEmpty())
        }
    }
}
