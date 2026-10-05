package tribixbite.cleverkeys.ai

import tribixbite.cleverkeys.langpack.IntelligenceJson
import java.io.Reader
import java.util.PriorityQueue

/** Cased Char-BPE over original exported tables, not a newly authored vocabulary.
 * Unicode behaviour comes from the pinned Rust tokenizer, never Android categories.
 * No cross-request cache retains editor strings. Requires real-vector conformance.
 */
internal class HerbertTokenizer internal constructor(
    vocabulary: Map<String, Int>,
    mergeRows: List<Triple<Int, Int, Int>>,
    addedTokens: Map<String, Int>,
    unicodeFlags: ByteArray,
    val special: HerbertPreparedBatch.SpecialTokens,
) {
    private val vocab = HashMap(vocabulary)
    private val added = HashMap(addedTokens)
    private val flags = unicodeFlags.copyOf()
    private data class Merge(val rank: Int, val token: Int)
    private val merges = HashMap<Long, Merge>()

    init {
        require(vocab.size in 5..HerbertPreparedBatch.VOCAB_SIZE)
        require(vocab.values.toSet() == (0 until vocab.size).toSet())
        require(flags.size == 0x110000 && flags.all { it.toInt() == 0 || it.toInt() == 1 ||
            it.toInt() == 2 || it.toInt() == 4 || it.toInt() == 8 })
        require(added.keys == setOf("<s>", "</s>", "<mask>", "<pad>", "<unk>"))
        require(added.all { (text, id) -> vocab[text] == id })
        require(listOf("<s>" to special.cls, "</s>" to special.sep, "<mask>" to special.mask,
            "<pad>" to special.pad, "<unk>" to special.unk).all { (s, id) -> added[s]?.toLong() == id })
        for ((rank, row) in mergeRows.withIndex()) {
            val (a, b, c) = row
            require(listOf(a, b, c).all { it in 0 until vocab.size })
            require(merges.put(pair(a, b), Merge(rank, c)) == null)
        }
    }

    fun encode(text: String): LongArray {
        require(text.length <= 4096) { "HerBERT text budget" }
        var scalarAt = 0
        while (scalarAt < text.length) {
            val ch = text[scalarAt]
            require(!Character.isLowSurrogate(ch)) { "Unpaired surrogate" }
            if (Character.isHighSurrogate(ch)) {
                require(scalarAt + 1 < text.length && Character.isLowSurrogate(text[scalarAt + 1]))
                scalarAt++
            }
            scalarAt++
        }
        val result = ArrayList<Long>()
        var position = 0
        while (position < text.length) {
            var foundAt = text.length
            var found: String? = null
            for (token in added.keys) {
                val at = text.indexOf(token, position)
                if (at >= 0 && (at < foundAt || at == foundAt && token.length > (found?.length ?: 0))) {
                    foundAt = at
                    found = token
                }
            }
            ordinary(text.substring(position, foundAt), result)
            val token = found ?: break
            result.add(added.getValue(token).toLong())
            position = foundAt + token.length
        }
        return result.toLongArray()
    }

    private fun ordinary(text: String, out: MutableList<Long>) {
        val buffer = StringBuilder()
        fun flush() {
            if (buffer.isNotEmpty()) {
                word(buffer.toString(), out)
                buffer.setLength(0)
            }
        }
        var at = 0
        while (at < text.length) {
            val cp = text.codePointAt(at)
            at += Character.charCount(cp)
            when (flags[cp].toInt()) {
                1 -> Unit // dropped controls join their surrounding text
                2 -> flush()
                4, 8 -> { flush(); word(String(Character.toChars(cp)), out) }
                else -> buffer.appendCodePoint(cp)
            }
        }
        flush()
    }

    private data class Pending(val rank: Int, val left: Int, val right: Int, val token: Int)

    private fun word(text: String, out: MutableList<Long>) {
        val scalars = text.codePoints().toArray()
        val ids = IntArray(scalars.size) { i ->
            val token = String(Character.toChars(scalars[i])) + if (i == scalars.lastIndex) "</w>" else ""
            vocab[token] ?: special.unk.toInt()
        }
        if (ids.isEmpty()) return
        val previous = IntArray(ids.size) { it - 1 }
        val next = IntArray(ids.size) { if (it == ids.lastIndex) -1 else it + 1 }
        val active = BooleanArray(ids.size) { true }
        val queue = PriorityQueue<Pending>(compareBy<Pending> { it.rank }.thenBy { it.left })
        fun enqueue(left: Int) {
            if (left < 0 || !active[left]) return
            val right = next[left]
            if (right < 0) return
            val merge = merges[pair(ids[left], ids[right])] ?: return
            queue.add(Pending(merge.rank, left, right, merge.token))
        }
        for (i in ids.indices) enqueue(i)
        while (queue.isNotEmpty()) {
            val pending = queue.remove()
            val l = pending.left
            val r = pending.right
            if (!active[l] || !active[r] || next[l] != r) continue
            val merge = merges[pair(ids[l], ids[r])] ?: continue
            if (merge.rank != pending.rank || merge.token != pending.token) continue
            ids[l] = merge.token
            active[r] = false
            next[l] = next[r]
            if (next[l] >= 0) previous[next[l]] = l
            enqueue(previous[l])
            enqueue(l)
        }
        var i = 0
        while (i >= 0) { out.add(ids[i].toLong()); i = next[i] }
    }

    /** Truncate only the oldest complete context words when the graph budget requires it. */
    fun prepare(context: String, surfaces: List<String>,
                contextWords: Int = HerbertContextWindow.DEFAULT_WORDS): HerbertPreparedBatch {
        require(surfaces.size in 1..HerbertPreparedBatch.MAX_CANDIDATES)
        val targets = surfaces.map { encode(it) }
        require(targets.all { it.size in 1..HerbertPreparedBatch.MAX_TARGETS && special.unk !in it })
        var left = HerbertContextWindow.retain(context, contextWords)
        var ids = encode(left)
        val maximumTarget = targets.maxOf { it.size }
        while (ids.size + maximumTarget + 2 > HerbertPreparedBatch.MAX_SEQUENCE) {
            var boundary = 0
            while (boundary < left.length && flags[left.codePointAt(boundary)].toInt() != 2) {
                boundary += Character.charCount(left.codePointAt(boundary))
            }
            require(boundary < left.length) { "Context word exceeds token budget" }
            while (boundary < left.length && flags[left.codePointAt(boundary)].toInt() == 2) {
                boundary += Character.charCount(left.codePointAt(boundary))
            }
            left = left.substring(boundary)
            ids = encode(left)
        }
        return HerbertPreparedBatch.create(surfaces.mapIndexed { i, surface ->
            HerbertPreparedBatch.CandidateTokens(surface, ids, targets[i])
        }, special)
    }

    companion object {
        const val PROTOCOL = "herbert-fp32-benchmark-v1"
        private fun pair(a: Int, b: Int) = (a.toLong() shl 32) or b.toLong()

        fun parse(reader: Reader): HerbertTokenizer {
            val root = IntelligenceJson.document(reader)
            require(IntelligenceJson.integer(root, "schemaVersion") == 1)
            require(IntelligenceJson.string(root, "protocol") == PROTOCOL)
            val tokens = root.getAsJsonArray("vocabulary")
            require(tokens.size() == HerbertPreparedBatch.VOCAB_SIZE)
            val vocab = LinkedHashMap<String, Int>()
            for ((i, token) in tokens.withIndex()) {
                require(token.isJsonPrimitive && token.asJsonPrimitive.isString)
                val text = token.asString
                require(text.isNotEmpty() && text.length <= 4096 && vocab.put(text, i) == null)
            }
            val rows = root.getAsJsonArray("merges")
            require(rows.size() <= 100_000)
            val merges = rows.map { element ->
                val row = element.asJsonArray
                require(row.size() == 3)
                fun integer(i: Int): Int {
                    require(row[i].isJsonPrimitive && row[i].asJsonPrimitive.isNumber)
                    return row[i].asBigDecimal.intValueExact()
                }
                Triple(integer(0), integer(1), integer(2))
            }
            val flags = ByteArray(0x110000)
            val ranges = root.getAsJsonArray("unicodeRanges")
            require(ranges.size() <= 8192)
            var previous = -1
            for (element in ranges) {
                val row = element.asJsonArray
                require(row.size() == 3 && row.all { it.isJsonPrimitive && it.asJsonPrimitive.isNumber })
                val first = row[0].asBigDecimal.intValueExact()
                val last = row[1].asBigDecimal.intValueExact()
                val flag = row[2].asBigDecimal.intValueExact()
                require(first > previous && first in 0..0x10ffff && last in first..0x10ffff)
                require(last < 0xd800 || first > 0xdfff)
                require(flag in setOf(1, 2, 4, 8))
                flags.fill(flag.toByte(), first, last + 1)
                previous = last
            }
            val addedRows = root.getAsJsonArray("addedTokens")
            require(addedRows.size() == 5)
            val added = LinkedHashMap<String, Int>()
            for (element in addedRows) {
                val row = element.asJsonObject
                for (field in listOf("single_word", "lstrip", "rstrip", "normalized", "special")) {
                    val value = row.get(field)
                    require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean)
                    require(value.asBoolean == (field == "special"))
                }
                require(added.put(IntelligenceJson.string(row, "content"), IntelligenceJson.integer(row, "id")) == null)
            }
            val special = root.getAsJsonObject("specialTokenIds")
            return HerbertTokenizer(vocab, merges, added, flags, HerbertPreparedBatch.SpecialTokens(
                IntelligenceJson.integer(special, "cls").toLong(), IntelligenceJson.integer(special, "sep").toLong(),
                IntelligenceJson.integer(special, "mask").toLong(), IntelligenceJson.integer(special, "pad").toLong(),
                IntelligenceJson.integer(special, "unk").toLong()))
        }
    }
}
