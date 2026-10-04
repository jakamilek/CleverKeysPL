package tribixbite.cleverkeys.ai

import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Optional until the new producer run supplies trusted original-tokenizer fixtures. */
class HerbertRealConformanceTest {
    @Test fun originalFastTokenizerAndAll232PreparedBatchesMatch() {
        val path = System.getProperty("herbertFixtureDir")
        assumeTrue("Real producer metadata not supplied; this is not a conformance pass", !path.isNullOrBlank())
        val directory = File(path!!)
        val tokenizer = File(directory, "portable-tokenizer.json").reader(Charsets.UTF_8).use { HerbertTokenizer.parse(it) }
        File(directory, "tokenizer-conformance.json").reader(Charsets.UTF_8).use {
            HerbertConformance.tokens(tokenizer, it)
        }
        val vectors = File(directory, "android-score-vectors.json").reader(Charsets.UTF_8).use {
            HerbertConformance.feeds(tokenizer, it)
        }
        assertEquals(232, vectors.size)
    }
}
