package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.os.Trace
import android.util.Log
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import tribixbite.cleverkeys.langpack.LanguagePackManager
import io.mockk.just
import io.mockk.runs
import io.mockk.every
import io.mockk.mockkStatic
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Real binary/display row parity, rather than a copy of the display implementation. */
class MainDictionarySourceRowsTest {
    @Before fun setup() {
        MainDictionarySource.invalidateCache()
        mockkStatic(Trace::class)
        every { Trace.beginSection(any()) } just runs
        every { Trace.endSection() } just runs
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
    }
    @After fun cleanup() {
        MainDictionarySource.invalidateCache()
        unmockkAll()
    }

    private fun compare(file: File, disabled: Set<String>): List<DictionaryWord> {
        val index = NormalizedPrefixIndex()
        val buffer = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        if (buffer.int == 0x54434944) {
            buffer.int
            BinaryDictionaryLoader::class.java.getDeclaredMethod("loadV1IntoNormalizedIndex",
                ByteBuffer::class.java, NormalizedPrefixIndex::class.java).apply {
                isAccessible = true
            }.invoke(BinaryDictionaryLoader, buffer, index)
        } else assertTrue(BinaryDictionaryLoader.loadIntoNormalizedIndexFromFile(file, index))
        val former = index.getAllNormalizedWords().map { key ->
            val match = index.lookup(key)!!
            DictionaryWord(match.bestCanonical, (10000 - match.bestFrequencyRank * 39).coerceIn(1, 10000),
                WordSource.MAIN, key !in disabled && match.bestCanonical !in disabled)
        }
        val actual = BinaryDictionaryLoader.loadDisplayWordsFromFile(file, disabled)!!
        assertEquals(former, actual)
        return actual
    }

    /** Fixture accent map deliberately starts with a less frequent canonical. */
    private fun fixture(words: List<Pair<String, Int>>, v1: Boolean = false): File {
        val buf = ByteBuffer.allocate(1024 * 1024).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(if (v1) 32 else 48)
        val canonicalOffset = buf.position()
        words.forEach { (word, rank) ->
            val bytes = word.toByteArray(Charsets.UTF_8)
            buf.putShort(bytes.size.toShort()); buf.put(bytes)
            if (!v1) buf.put(rank.toByte())
        }
        val normalizedOffset = buf.position()
        val groups = words.indices.groupBy { AccentNormalizer.normalize(words[it].first) }
        if (v1) words.forEach { buf.putInt(10000 - it.second * 39) }
        else {
            buf.putInt(groups.size)
            groups.keys.forEach { key ->
                val bytes = key.toByteArray(Charsets.UTF_8)
                buf.putShort(bytes.size.toShort()); buf.put(bytes)
            }
        }
        val mapOffset = buf.position()
        if (!v1) groups.values.forEach { indices ->
            buf.put(indices.size.toByte()); indices.forEach { buf.putInt(it) }
        }
        val end = buf.position()
        buf.position(0)
        buf.putInt(if (v1) 0x54434944 else 0x54444B43)
        buf.putInt(if (v1) 1 else 2)
        if (!v1) buf.put(byteArrayOf(112, 108, 0, 0))
        buf.putInt(words.size); buf.putInt(canonicalOffset); buf.putInt(normalizedOffset)
        buf.putInt(if (v1) end else mapOffset)
        return File.createTempFile("display-rows", ".bin").apply {
            deleteOnExit(); writeBytes(buf.array().copyOf(end))
        }
    }

    @Test fun v2PreservesFirstMappedCanonicalRanksAndDisabledPolicy() {
        val file = fixture(listOf("Łódź" to 90, "łódź" to 40, "Łodzi" to 80,
            "łodzi" to 50, "pracą" to 30, "praca" to 80, "malina" to 10))
        try {
            val rows = compare(file, setOf("lodz", "pracą"))
            assertEquals("Łódź", rows.first().word)
            assertEquals(6490, rows.first().frequency)
            assertFalse(rows.first().enabled)
            assertFalse(rows.first { it.word == "pracą" }.enabled)
            assertTrue(rows.first { it.word == "malina" }.enabled)
        } finally { file.delete() }
    }

    @Test fun densePrefixesKeepEveryRowWithoutBuildingAPredictionIndex() {
        val file = fixture((0 until 3000).map { "wspolny$it" to (it % 256) })
        try {
            val rows = compare(file, emptySet())
            assertEquals(3000, rows.size)
            assertEquals(3000, rows.map { it.word }.toSet().size)
        } finally { file.delete() }
    }

    @Test fun v1RankConversionAndCanonicalOrderRemainCompatible() {
        val file = fixture(listOf("częsty" to 0, "rzadki" to 255, "Łódź" to 90, "łódź" to 40), true)
        try { compare(file, setOf("lodz")) } finally { file.delete() }
    }

    @Test fun coldSearchAndReplacementSourceShareTheBlockedActualBinaryRead() = runBlocking {
        withTimeout(5000) {
            val file = fixture(listOf("Łódź" to 40, "łódź" to 90, "Łodzi" to 50, "malina" to 10))
            val bytes = file.readBytes()
            file.delete()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val opens = AtomicInteger()
            val ctx = mockk<Context>()
            val assets = mockk<AssetManager>()
            val prefs = mockk<SharedPreferences>()
            every { ctx.applicationContext } returns ctx
            every { ctx.assets } returns assets
            every { prefs.getStringSet(any(), any()) } returns emptySet()
            mockkObject(LanguagePackManager.Companion)
            val manager = mockk<LanguagePackManager>()
            every { LanguagePackManager.getInstance(ctx) } returns manager
            every { manager.getDictionaryPath("pl") } returns null
            every { assets.open("dictionaries/pl_enhanced.bin") } answers {
                opens.incrementAndGet()
                object : ByteArrayInputStream(bytes) {
                    override fun read(out: ByteArray, offset: Int, length: Int): Int {
                        entered.countDown()
                        check(release.await(5, TimeUnit.SECONDS))
                        return super.read(out, offset, length)
                    }
                }
            }
            try {
                val source1 = MainDictionarySource(ctx, DisabledDictionarySource(prefs, "pl"), "pl")
                val firstView = async { source1.getAllWords() }
                assertTrue(withContext(Dispatchers.IO) { entered.await(5, TimeUnit.SECONDS) })
                firstView.cancelAndJoin()
                val source2 = MainDictionarySource(ctx, DisabledDictionarySource(prefs, "pl"), "pl")
                val restoredSearch = async { source2.searchWords("Łod") }
                release.countDown()
                assertEquals(listOf("Łodzi"), restoredSearch.await().map { it.word })
                assertEquals(3, source2.getAllWords().size)
                assertEquals(1, opens.get())
                verify(exactly = 1) { assets.open("dictionaries/pl_enhanced.bin") }
            } finally { release.countDown(); MainDictionarySource.invalidateCache() }
        }
    }

    @Test fun bundledBinaryDictionariesKeepAllRowsAndRanks() {
        val files = File("src/main/assets/dictionaries").listFiles()!!.filter { it.name.endsWith("_enhanced.bin") }
        assertTrue(files.isNotEmpty())
        files.forEach { compare(it, setOf("example", "lodz", "café")) }
    }
}
