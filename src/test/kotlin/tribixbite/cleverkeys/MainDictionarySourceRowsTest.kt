package tribixbite.cleverkeys

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Exercise the actual UI row extractor, including the large shared-prefix regression. */
class MainDictionarySourceRowsTest {
    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        MainDictionarySource.invalidateCache()
    }

    @After fun cleanup() {
        MainDictionarySource.invalidateCache()
        unmockkStatic(Log::class)
    }

    private fun rows(index: NormalizedPrefixIndex, disabled: Set<String> = emptySet()): List<DictionaryWord> {
        val source = MainDictionarySource(mockk<Context>(relaxed = true),
            DisabledDictionarySource(mockk<SharedPreferences>(relaxed = true), "pl"), "pl")
        val output = mutableListOf<DictionaryWord>()
        MainDictionarySource::class.java.getDeclaredMethod("extractWordsFromIndex",
            NormalizedPrefixIndex::class.java, List::class.java, Set::class.java).apply {
            isAccessible = true
        }.invoke(source, index, output, disabled)
        return output
    }

    @Test fun exactProjectionRetainsTheFormerCanonicalRanksAndDisabledPolicy() {
        val index = NormalizedPrefixIndex().apply { buildFromList(listOf(
            "Łódź" to 40, "łódź" to 90, "Łodzi" to 50, "łodzi" to 80,
            "praca" to 80, "pracą" to 30, "pracować" to 20, "malina" to 10
        )) }
        val disabled = setOf("lodz", "pracą")
        val former = index.getAllNormalizedWords().map { key ->
            val match = index.getWordsWithPrefix(key).first { it.normalized == key }
            DictionaryWord(match.bestCanonical, (10000 - match.bestFrequencyRank * 39).coerceIn(1, 10000),
                WordSource.MAIN, key !in disabled && match.bestCanonical !in disabled)
        }
        assertEquals(former, rows(index, disabled))
        assertFalse(rows(index, disabled).first { it.word == "Łódź" }.enabled)
        assertFalse(rows(index, disabled).first { it.word == "pracą" }.enabled)
        assertTrue(rows(index, disabled).first { it.word == "malina" }.enabled)
    }

    @Test fun densePrefixesRequireOneExactLookupPerRowWithoutScanningCompletions() {
        val index = spyk(NormalizedPrefixIndex().apply {
            buildFromList((0 until 3000).map { "wspolny$it" to (it % 256) })
        })
        val output = rows(index)
        assertEquals(3000, output.size)
        assertEquals(3000, output.map { it.word }.toSet().size)
        verify(exactly = 3000) { index.lookup(any()) }
        verify(exactly = 0) { index.getWordsWithPrefix(any()) }
    }

    @Test fun rankEndpointsAndSourceRemainUnchanged() {
        val index = NormalizedPrefixIndex().apply { buildFromList(listOf("częsty" to 0, "rzadki" to 255)) }
        val output = rows(index).associateBy { it.word }
        assertEquals(10000, output.getValue("częsty").frequency)
        assertEquals(55, output.getValue("rzadki").frequency)
        assertTrue(output.values.all { it.source == WordSource.MAIN && it.enabled })
    }
}
