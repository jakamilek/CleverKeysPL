package tribixbite.cleverkeys.ai

import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader

class HerbertTokenizerTest {
    private fun tokenizer(): HerbertTokenizer {
        val vocab = listOf("<s>", "</s>", "<mask>", "<pad>", "<unk>", "a", "b", "c</w>",
            "ab", "abc</w>", "a</w>", "!</w>", "中</w>", "bc</w>", "a</w>b</w>")
            .mapIndexed { i, s -> s to i }.toMap()
        val flags = ByteArray(0x110000)
        flags[0] = 1; flags[32] = 2; flags[33] = 8; flags[0x200b] = 1; flags[0x4e2d] = 4
        return HerbertTokenizer(vocab, listOf(Triple(6,7,13), Triple(5,6,8), Triple(8,7,9)),
            mapOf("<s>" to 0, "</s>" to 1, "<mask>" to 2, "<pad>" to 3, "<unk>" to 4),
            flags, HerbertPreparedBatch.SpecialTokens(0,1,2,3,4))
    }

    @Test fun lowestRankWinsBeforeLeftmostPair() {
        assertArrayEquals(longArrayOf(5,13), tokenizer().encode("abc"))
    }
    @Test fun suffixWhitespacePunctuationAndCjk() {
        assertArrayEquals(longArrayOf(10,11,12,10), tokenizer().encode("a !中a"))
    }
    @Test fun droppedControlsJoinText() {
        assertArrayEquals(tokenizer().encode("abc"), tokenizer().encode("a\u200bb\u0000c"))
    }
    @Test fun addedTokensRecognizedBeforeNormalization() {
        assertArrayEquals(longArrayOf(10,2,2,10), tokenizer().encode("a<mask><mask>a"))
        assertFalse(2L in tokenizer().encode("<ma\u200bsk>"))
    }
    @Test fun unknownScalarsNotFused() {
        assertArrayEquals(longArrayOf(4,4), tokenizer().encode("😀x"))
    }
    @Test fun emptyInputAndOversizedInput() {
        assertArrayEquals(longArrayOf(), tokenizer().encode(""))
        assertThrows(IllegalArgumentException::class.java) { tokenizer().encode("a".repeat(4097)) }
    }
    @Test fun unpairedSurrogatesRefused() {
        for (text in listOf("\ud800", "\udc00", "x\ud800y")) {
            assertThrows(IllegalArgumentException::class.java) { tokenizer().encode(text) }
        }
    }
    @Test fun originalTablesCannotBeReplacedByDuplicateJsonMembers() {
        assertThrows(IllegalArgumentException::class.java) {
            HerbertTokenizer.parse(StringReader("{\"schemaVersion\":1,\"schemaVersion\":1}"))
        }
    }
    @Test fun prepareMasksWholeTargetAndKeepsCasedContext() {
        val batch = tokenizer().prepare("a ", listOf("a", "abc"))
        assertArrayEquals(longArrayOf(0,10,2,1,3,0,10,2,2,1), batch.inputIds())
        assertArrayEquals(longArrayOf(10,0,5,13), batch.targetTokenIds())
    }
    @Test fun unknownTargetRefused() {
        assertThrows(IllegalArgumentException::class.java) { tokenizer().prepare("a ", listOf("x")) }
    }
}
