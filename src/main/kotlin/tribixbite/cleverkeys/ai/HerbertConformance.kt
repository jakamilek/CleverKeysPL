package tribixbite.cleverkeys.ai

import tribixbite.cleverkeys.langpack.IntelligenceJson
import java.io.Reader
import java.util.concurrent.CancellationException

/** Uses only supplied synthetic/archived fixtures; does not read any Android editor. */
internal object HerbertConformance {
    data class ScoreVector(val id: String, val batch: HerbertPreparedBatch, val expected: Map<String, Float>)
    data class Timing(val samples: Int, val p50Ms: Double, val p95Ms: Double)
    data class WindowTiming(val words: Int, val feed: Timing, val inference: Timing, val total: Timing)
    data class Result(val tokenizerVectors: Int, val scoreVectors: Int, val maxAbsoluteError: Float,
                      val loadMs: Double, val feed: Timing, val inference: Timing, val total: Timing,
                      val windows: List<WindowTiming>)

    fun tokens(tokenizer: HerbertTokenizer, reader: Reader): Int {
        val root = IntelligenceJson.document(reader)
        require(IntelligenceJson.integer(root, "schemaVersion") == 1)
        val vectors = root.getAsJsonArray("vectors")
        require(vectors.size() in 1..20_000)
        for (raw in vectors) {
            val row = raw.asJsonObject
            val text = IntelligenceJson.string(row, "text")
            val expected = row.getAsJsonArray("ids").map { it.asBigDecimal.longValueExact() }.toLongArray()
            require(tokenizer.encode(text).contentEquals(expected)) { "HerBERT tokenizer conformance failed" }
        }
        return vectors.size()
    }

    fun feeds(tokenizer: HerbertTokenizer, reader: Reader): List<ScoreVector> {
        val root = IntelligenceJson.document(reader)
        require(IntelligenceJson.integer(root, "schemaVersion") == 1 &&
            IntelligenceJson.string(root, "protocol") == HerbertTokenizer.PROTOCOL)
        val vectors = root.getAsJsonArray("vectors")
        require(vectors.size() == 232)
        val seen = HashSet<String>()
        return vectors.map { raw ->
            val row = raw.asJsonObject
            val id = IntelligenceJson.string(row, "id")
            require(seen.add(id))
            val surfaces = row.getAsJsonArray("surfaces").map { it.asString }
            val batch = tokenizer.prepare(IntelligenceJson.string(row, "context"), surfaces)
            val inputs = row.getAsJsonObject("inputs")
            fun longs(name: String) = inputs.getAsJsonArray(name).flatMap { r ->
                r.asJsonArray.map { it.asBigDecimal.longValueExact() }
            }.toLongArray()
            require(batch.inputIds().contentEquals(longs("input_ids")))
            require(batch.attentionMask().contentEquals(longs("attention_mask")))
            require(batch.targetPositions().contentEquals(longs("target_positions")))
            require(batch.targetTokenIds().contentEquals(longs("target_ids")))
            val mask = inputs.getAsJsonArray("target_mask").flatMap { r -> r.asJsonArray.map { it.asFloat } }.toFloatArray()
            require(batch.targetMask().contentEquals(mask))
            val expected = row.getAsJsonObject("onnxFloat").entrySet().associate { it.key to it.value.asFloat }
            require(expected.keys == surfaces.toSet() && expected.values.all { it.isFinite() })
            ScoreVector(id, batch, expected)
        }
    }

    /** Worker only; cancellation is checked between native calls (no main-thread close). */
    fun benchmark(bundle: HerbertImportedBundle, cancelled: () -> Boolean = { false },
                  checkpoint: () -> Unit = {}): Result {
        fun checkActive() { if (cancelled()) throw CancellationException() }
        checkActive()
        val tokenCount = bundle.reader("tokenizer-conformance.json").use { tokens(bundle.tokenizer, it) }
        checkActive()
        val vectors = bundle.reader("android-score-vectors.json").use { feeds(bundle.tokenizer, it) }
        checkActive()
        val started = System.nanoTime()
        HerbertOnnxScorer.open(bundle).use { scorer ->
            val loadMs = (System.nanoTime() - started) / 1_000_000.0
            checkpoint()
            var error = 0f
            for (vector in vectors) {
                checkActive()
                val actual = scorer.score(vector.batch)
                checkpoint()
                for (surface in vector.batch.surfaces) {
                    error = maxOf(error, kotlin.math.abs(actual.getValue(surface) - vector.expected.getValue(surface)))
                }
                require(error <= 0.001f) { "Android FP32 score parity failed" }
                fun rank(values: Map<String, Float>) = vector.batch.surfaces.sortedByDescending { values.getValue(it) }
                require(rank(actual) == rank(vector.expected)) { "Android FP32 ranking differs" }
            }
            // Separate current intended live workload: exactly two forms, three synthetic
            // context lengths. Compare limits 32/64; three warmups each, then 30
            // timed trials per context/window. This measures speed, not accuracy.
            val contexts = listOf("Płyniemy przez jezioro i czeka na nas ",
                "Podczas podróży zwiedziliśmy kilka miast. Następnym miejscem będzie ",
                ("Podczas tej podróży poznaliśmy nowe miejsca i wiele osób. ".repeat(5)) + "Jedziemy teraz do ")
            val pairs = listOf(listOf("łódź", "Łódź"), listOf("malina", "Malina"), listOf("łodzi", "Łodzi"))
            val feed = ArrayList<Double>()
            val inference = ArrayList<Double>()
            val total = ArrayList<Double>()
            val windows = ArrayList<WindowTiming>()
            val windowFeed = listOf(ArrayList<Double>(), ArrayList<Double>())
            val windowInference = listOf(ArrayList<Double>(), ArrayList<Double>())
            val windowTotal = listOf(ArrayList<Double>(), ArrayList<Double>())
            val wordLimits = listOf(32, 64)
            for (words in wordLimits) {
                for (i in contexts.indices) repeat(3) {
                    checkActive(); scorer.score(bundle.tokenizer.prepare(contexts[i], pairs[i], words))
                }
            }
            repeat(30) { trial ->
                // Alternate which window is first to reduce systematic warm/thermal order bias.
                val order = if (trial % 2 == 0) listOf(0, 1) else listOf(1, 0)
                for (i in contexts.indices) {
                    for (w in order) {
                        checkActive()
                        val began = System.nanoTime()
                        val batch = bundle.tokenizer.prepare(contexts[i], pairs[i], wordLimits[w])
                        val ready = System.nanoTime()
                        scorer.score(batch)
                        val done = System.nanoTime()
                        checkpoint()
                        windowFeed[w].add((ready - began) / 1_000_000.0)
                        windowInference[w].add((done - ready) / 1_000_000.0)
                        windowTotal[w].add((done - began) / 1_000_000.0)
                    }
                }
            }
            for (w in wordLimits.indices) {
                windows.add(WindowTiming(wordLimits[w], timing(windowFeed[w]), timing(windowInference[w]), timing(windowTotal[w])))
                feed.addAll(windowFeed[w]); inference.addAll(windowInference[w]); total.addAll(windowTotal[w])
            }
            return Result(tokenCount, vectors.size, error, loadMs, timing(feed), timing(inference), timing(total), windows)
        }
    }

    private fun timing(values: List<Double>): Timing {
        val sorted = values.sorted()
        return Timing(sorted.size, (sorted[(sorted.size - 1) / 2] + sorted[sorted.size / 2]) / 2,
            sorted[kotlin.math.ceil(sorted.size * 0.95).toInt() - 1])
    }
}
