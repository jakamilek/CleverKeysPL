package tribixbite.cleverkeys.ai

/** Whole-process samples only. No forced GC, heap claims or timing inside native calls. */
internal class HerbertMemoryProbe(
    private val readKiB: () -> Long,
    private val clockNanos: () -> Long = System::nanoTime,
) {
    enum class Stage { BASELINE, LOADED, FIRST_PAIR, WARMUP, WORKLOAD, CONFORMANCE, CLOSED }
    data class Phase(val stage: Stage, val context: Int = 0, val words: Int = 0) {
        init {
            if (stage == Stage.WARMUP || stage == Stage.WORKLOAD) {
                require(context in 1..3 && words in listOf(32, 64))
            } else require(context == 0 && words == 0)
        }
    }
    data class Sample(val phase: Phase, val firstKiB: Long, val lastKiB: Long,
                      val maxKiB: Long, val samples: Int)
    private val phases = LinkedHashMap<Phase, Sample>()
    private var lastNanos: Long? = null

    /** Force boundaries; otherwise sample at most once every 250 ms globally.
     * The first observation of each phase is never silently omitted.
     */
    fun sample(phase: Phase, force: Boolean = false) {
        val now = clockNanos()
        val previous = phases[phase]
        if (!force && previous != null && lastNanos?.let { now - it < 250_000_000L } == true) return
        val value = readKiB()
        require(value >= 0)
        phases[phase] = Sample(phase, previous?.firstKiB ?: value, value,
            maxOf(previous?.maxKiB ?: value, value), (previous?.samples ?: 0) + 1)
        // Timestamp after the potentially slow platform PSS call, outside timed inference.
        lastNanos = clockNanos()
    }

    fun snapshot(): List<Sample> = java.util.Collections.unmodifiableList(ArrayList(phases.values))
    fun maximumKiB(): Long = phases.values.maxOfOrNull { it.maxKiB } ?: 0
    fun baselineKiB(): Long = phases.getValue(Phase(Stage.BASELINE)).firstKiB
}

/** One newly opened session: intended workload FIRST, mandatory verification SECOND.
 * Failures/cancellation never return a successful result; close precedes its observation.
 */
internal object HerbertBenchmarkLifecycle {
    fun <S : AutoCloseable, P, V> run(open: () -> S, loaded: () -> Unit,
        workload: (S) -> P, verify: (S) -> V, closed: () -> Unit): Pair<P, V> {
        val session = open()
        try {
            loaded()
            val performance = workload(session)
            val verification = verify(session)
            return performance to verification
        } finally {
            try { session.close() } finally { closed() }
        }
    }
}
