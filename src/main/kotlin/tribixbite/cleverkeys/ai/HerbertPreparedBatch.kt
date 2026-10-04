package tribixbite.cleverkeys.ai

/** Exact feed for the exported WWM graph. Construct only from a conformance-verified tokenizer. */
internal class HerbertPreparedBatch private constructor(
    val size: Int,
    val sequence: Int,
    val targets: Int,
    private val ids: LongArray,
    private val attention: LongArray,
    private val positions: LongArray,
    private val targetIds: LongArray,
    private val targetMask: FloatArray,
    val surfaces: List<String>,
) {
    fun inputIds() = ids.copyOf()
    fun attentionMask() = attention.copyOf()
    fun targetPositions() = positions.copyOf()
    fun targetTokenIds() = targetIds.copyOf()
    fun targetMask() = targetMask.copyOf()

    data class SpecialTokens(val cls: Long, val sep: Long, val mask: Long, val pad: Long, val unk: Long) {
        init { require(listOf(cls, sep, mask, pad, unk).all { it in 0 until VOCAB_SIZE.toLong() }) }
    }

    data class CandidateTokens(val surface: String, val left: LongArray, val target: LongArray)

    companion object {
        const val VOCAB_SIZE = 50_000
        const val MAX_SEQUENCE = 512
        const val MAX_CANDIDATES = 12
        const val MAX_TARGETS = 32

        fun create(candidates: List<CandidateTokens>, special: SpecialTokens): HerbertPreparedBatch {
            require(candidates.size in 1..MAX_CANDIDATES)
            require(candidates.map { it.surface }.distinct().size == candidates.size)
            require(candidates.all { it.surface.isNotBlank() && it.surface.length <= 96 })
            val sequence = candidates.maxOf { it.left.size + it.target.size + 2 }
            val targets = candidates.maxOf { it.target.size }
            require(sequence in 3..MAX_SEQUENCE && targets in 1..MAX_TARGETS)
            val size = candidates.size
            val ids = LongArray(size * sequence) { special.pad }
            val attention = LongArray(size * sequence)
            val positions = LongArray(size * targets)
            val targetIds = LongArray(size * targets)
            val targetMask = FloatArray(size * targets)
            for ((b, candidate) in candidates.withIndex()) {
                require(candidate.target.isNotEmpty() && special.unk !in candidate.target)
                require(candidate.left.all { it in 0 until VOCAB_SIZE.toLong() } &&
                    candidate.target.all { it in 0 until VOCAB_SIZE.toLong() })
                val base = b * sequence
                ids[base] = special.cls
                candidate.left.copyInto(ids, base + 1)
                val targetStart = candidate.left.size + 1
                for (t in candidate.target.indices) {
                    val index = b * targets + t
                    positions[index] = (targetStart + t).toLong()
                    targetIds[index] = candidate.target[t]
                    targetMask[index] = 1f
                    ids[base + targetStart + t] = special.mask
                }
                ids[base + targetStart + candidate.target.size] = special.sep
                for (s in 0 until targetStart + candidate.target.size + 1) attention[base + s] = 1L
            }
            return HerbertPreparedBatch(size, sequence, targets, ids, attention, positions,
                targetIds, targetMask, java.util.Collections.unmodifiableList(candidates.map { it.surface }))
        }
    }
}
