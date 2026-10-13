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
                      val windows: List<WindowTiming>, val cases: List<CaseTiming>, val firstPairMs: Double,
                      val durationMs: Double)

    data class CaseTiming(val context: Int, val words: Int, val retainedWords: Int,
                          val batch: Int, val sequence: Int, val targets: Int,
                          val feed: Timing, val inference: Timing, val total: Timing)
    data class Progress(val stage: HerbertMemoryProbe.Stage, val completed: Int = 0, val total: Int = 0)
    private data class Performance(val windows: List<WindowTiming>, val cases: List<CaseTiming>,
                                   val feed: Timing, val inference: Timing, val total: Timing,
                                   val firstPairMs: Double)
    private data class Verification(val tokens: Int, val scores: Int, val error: Float)

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

    /** Fresh session per run; verification remains mandatory before a PASS report.
     * Phase/PSS observations happen outside the timed preparation and score calls.
     */
    fun benchmark(bundle: HerbertImportedBundle, cancelled: () -> Boolean = { false },
                  checkpoint: (HerbertMemoryProbe.Phase, Boolean) -> Unit = { _, _ -> },
                  progress: (Progress) -> Unit = {}): Result {
        fun checkActive() { if (cancelled()) throw CancellationException() }
        fun observe(stage: HerbertMemoryProbe.Stage, force: Boolean = false,
                    context: Int = 0, words: Int = 0) =
            checkpoint(HerbertMemoryProbe.Phase(stage, context, words), force)
        checkActive()
        val beganOverall = System.nanoTime()
        observe(HerbertMemoryProbe.Stage.BASELINE, true)
        progress(Progress(HerbertMemoryProbe.Stage.LOADED))
        val beganLoad = System.nanoTime()
        var loadMs = 0.0
        val (performance, verification) = HerbertBenchmarkLifecycle.run(
            open = { HerbertOnnxScorer.open(bundle) },
            loaded = {
                loadMs = (System.nanoTime() - beganLoad) / 1_000_000.0
                observe(HerbertMemoryProbe.Stage.LOADED, true)
                checkActive()
            },
            workload = { scorer ->
                val contexts = listOf("Płyniemy przez jezioro i czeka na nas ",
                    "Podczas podróży zwiedziliśmy kilka miast. Następnym miejscem będzie ",
                    ("Podczas tej podróży poznaliśmy nowe miejsca i wiele osób. ".repeat(5)) + "Jedziemy teraz do ")
                val pairs = listOf(listOf("łódź", "Łódź"), listOf("malina", "Malina"), listOf("łodzi", "Łodzi"))
                progress(Progress(HerbertMemoryProbe.Stage.FIRST_PAIR))
                val first = System.nanoTime()
                scorer.score(bundle.tokenizer.prepare(contexts[0], pairs[0], 32))
                val firstPairMs = (System.nanoTime() - first) / 1_000_000.0
                observe(HerbertMemoryProbe.Stage.FIRST_PAIR, true)
                checkActive()
                val limits = listOf(32, 64)
                val feeds = Array(3) { Array(2) { ArrayList<Double>() } }
                val inferences = Array(3) { Array(2) { ArrayList<Double>() } }
                val totals = Array(3) { Array(2) { ArrayList<Double>() } }
                val shapes = Array(3) { arrayOfNulls<HerbertPreparedBatch>(2) }
                var completed = 0
                // Short contexts first, then long: observe the first long-32 warmup
                // before long-64 can grow the same session's memory. No arena/GC changes.
                for (i in contexts.indices) {
                    for (w in limits.indices) {
                        checkActive()
                        progress(Progress(HerbertMemoryProbe.Stage.WARMUP, completed, 180))
                        repeat(3) {
                            checkActive()
                            val batch = bundle.tokenizer.prepare(contexts[i], pairs[i], limits[w])
                            shapes[i][w] = batch
                            scorer.score(batch)
                            observe(HerbertMemoryProbe.Stage.WARMUP, context = i + 1, words = limits[w])
                        }
                        observe(HerbertMemoryProbe.Stage.WARMUP, true, i + 1, limits[w])
                    }
                    repeat(30) { trial ->
                        val order = if (trial % 2 == 0) listOf(0, 1) else listOf(1, 0)
                        for (w in order) {
                            checkActive()
                            progress(Progress(HerbertMemoryProbe.Stage.WORKLOAD, completed, 180))
                            val began = System.nanoTime()
                            val batch = bundle.tokenizer.prepare(contexts[i], pairs[i], limits[w])
                            val ready = System.nanoTime()
                            scorer.score(batch)
                            val done = System.nanoTime()
                            feeds[i][w].add((ready - began) / 1_000_000.0)
                            inferences[i][w].add((done - ready) / 1_000_000.0)
                            totals[i][w].add((done - began) / 1_000_000.0)
                            completed++
                            observe(HerbertMemoryProbe.Stage.WORKLOAD, trial == 29, i + 1, limits[w])
                        }
                    }
                }
                progress(Progress(HerbertMemoryProbe.Stage.WORKLOAD, completed, 180))
                val cases = contexts.indices.flatMap { i -> limits.indices.map { w ->
                    val batch = requireNotNull(shapes[i][w])
                    CaseTiming(i + 1, limits[w], HerbertContextWindow.countWords(
                        HerbertContextWindow.retain(contexts[i], limits[w])), batch.size, batch.sequence,
                        batch.targets, timing(feeds[i][w]), timing(inferences[i][w]), timing(totals[i][w]))
                } }
                val windows = limits.indices.map { w -> WindowTiming(limits[w],
                    timing(feeds.flatMap { it[w] }), timing(inferences.flatMap { it[w] }),
                    timing(totals.flatMap { it[w] })) }
                require(cases.size == 6 && cases.all { it.total.samples == 30 } &&
                    windows.all { it.total.samples == 90 })
                Performance(windows, cases, timing(feeds.flatMap { it.flatMap { row -> row } }),
                    timing(inferences.flatMap { it.flatMap { row -> row } }),
                    timing(totals.flatMap { it.flatMap { row -> row } }), firstPairMs)
            },
            verify = { scorer ->
                checkActive()
                progress(Progress(HerbertMemoryProbe.Stage.CONFORMANCE, 0, 232))
                val tokenCount = bundle.reader("tokenizer-conformance.json").use { tokens(bundle.tokenizer, it) }
                checkActive()
                val vectors = bundle.reader("android-score-vectors.json").use { feeds(bundle.tokenizer, it) }
                observe(HerbertMemoryProbe.Stage.CONFORMANCE, true)
                var error = 0f
                for ((index, vector) in vectors.withIndex()) {
                    checkActive()
                    val actual = scorer.score(vector.batch)
                    for (surface in vector.batch.surfaces) error = maxOf(error,
                        kotlin.math.abs(actual.getValue(surface) - vector.expected.getValue(surface)))
                    require(error <= 0.001f) { "Android FP32 score parity failed" }
                    fun rank(values: Map<String, Float>) = vector.batch.surfaces.sortedByDescending { values.getValue(it) }
                    require(rank(actual) == rank(vector.expected)) { "Android FP32 ranking differs" }
                    observe(HerbertMemoryProbe.Stage.CONFORMANCE)
                    progress(Progress(HerbertMemoryProbe.Stage.CONFORMANCE, index + 1, vectors.size))
                }
                observe(HerbertMemoryProbe.Stage.CONFORMANCE, true)
                checkActive()
                Verification(tokenCount, vectors.size, error)
            },
            closed = { observe(HerbertMemoryProbe.Stage.CLOSED, true) },
        )
        checkActive()
        return Result(verification.tokens, verification.scores, verification.error, loadMs,
            performance.feed, performance.inference, performance.total, performance.windows,
            performance.cases, performance.firstPairMs, (System.nanoTime() - beganOverall) / 1_000_000.0)
    }

    internal fun timing(values: List<Double>): Timing {
        require(values.isNotEmpty() && values.all { it.isFinite() && it >= 0 })
        val sorted = values.sorted()
        return Timing(sorted.size, (sorted[(sorted.size - 1) / 2] + sorted[sorted.size / 2]) / 2,
            sorted[kotlin.math.ceil(sorted.size * 0.95).toInt() - 1])
    }
}
