package tribixbite.cleverkeys.ai

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException

class HerbertBenchmarkMetricsTest {
    @Test fun workloadPrecedesMandatoryVerificationAndSessionClosePrecedesClosedSample() {
        val events = ArrayList<String>()
        val session = AutoCloseable { events.add("close") }
        val result = HerbertBenchmarkLifecycle.run(
            open = { events.add("open"); session }, loaded = { events.add("loaded") },
            workload = { assertSame(session, it); events.add("workload"); 180 },
            verify = { assertSame(session, it); events.add("verify"); 232 },
            closed = { events.add("closed sample") })
        assertEquals(180 to 232, result)
        assertEquals(listOf("open", "loaded", "workload", "verify", "close", "closed sample"), events)
    }

    @Test fun failedVerificationCannotReturnPerformanceAsPassedAndStillCloses() {
        val events = ArrayList<String>()
        assertThrows(IllegalArgumentException::class.java) {
            HerbertBenchmarkLifecycle.run(
                open = { AutoCloseable { events.add("close") } }, loaded = {},
                workload = { events.add("workload"); 180 },
                verify = { events.add("verify"); throw IllegalArgumentException("score differs") },
                closed = { events.add("closed sample") })
        }
        assertEquals(listOf("workload", "verify", "close", "closed sample"), events)
    }

    @Test fun cancelledWorkloadDoesNotRunVerificationAndClosesExactlyOnce() {
        val events = ArrayList<String>()
        assertThrows(CancellationException::class.java) {
            HerbertBenchmarkLifecycle.run(
                open = { AutoCloseable { events.add("close") } }, loaded = {},
                workload = { throw CancellationException() }, verify = { events.add("verify") },
                closed = { events.add("closed sample") })
        }
        assertEquals(listOf("close", "closed sample"), events)
    }

    @Test fun cancellationAfterLoadClosesBeforeAnyWorkAndFailedOpenHasNoSessionToClose() {
        val events = ArrayList<String>()
        assertThrows(CancellationException::class.java) {
            HerbertBenchmarkLifecycle.run(
                open = { AutoCloseable { events.add("close") } }, loaded = { throw CancellationException() },
                workload = { events.add("workload") }, verify = { events.add("verify") },
                closed = { events.add("closed sample") })
        }
        assertEquals(listOf("close", "closed sample"), events)
        events.clear()
        assertThrows(IllegalStateException::class.java) {
            HerbertBenchmarkLifecycle.run<AutoCloseable, Unit, Unit>(
                open = { throw IllegalStateException("open failed") }, loaded = { events.add("loaded") },
                workload = { events.add("workload") }, verify = { events.add("verify") },
                closed = { events.add("closed sample") })
        }
        assertTrue(events.isEmpty())
    }

    @Test fun closedObservationRunsEvenIfNativeCloseThrows() {
        var observed = false
        assertThrows(IllegalStateException::class.java) {
            HerbertBenchmarkLifecycle.run(
                open = { AutoCloseable { throw IllegalStateException("close failed") } }, loaded = {},
                workload = { 180 }, verify = { 232 }, closed = { observed = true })
        }
        assertTrue(observed)
    }

    @Test fun forcedBoundariesAndFirstPhaseAreKeptWhileRepeatsAreThrottled() {
        var now = 0L
        var value = 100L
        var reads = 0
        val probe = HerbertMemoryProbe({ reads++; value }, { now })
        val baseline = HerbertMemoryProbe.Phase(HerbertMemoryProbe.Stage.BASELINE)
        val workload = HerbertMemoryProbe.Phase(HerbertMemoryProbe.Stage.WORKLOAD, 1, 32)
        probe.sample(baseline, true)
        now = 1; value = 120; probe.sample(workload)
        now = 2; value = 900; probe.sample(workload)
        assertEquals(2, reads)
        now = 250_000_001; value = 150; probe.sample(workload)
        now = 250_000_002; value = 110; probe.sample(workload, true)
        val row = probe.snapshot().last()
        assertEquals(120L, row.firstKiB)
        assertEquals(110L, row.lastKiB)
        assertEquals(150L, row.maxKiB)
        assertEquals(3, row.samples)
        assertEquals(100L, probe.baselineKiB())
        assertEquals(150L, probe.maximumKiB())
    }

    @Test fun phaseSnapshotsDoNotChangeAndCloseCanShowDropWithoutErasingPriorMaximum() {
        var value = 100L
        val probe = HerbertMemoryProbe({ value }, { 0L })
        probe.sample(HerbertMemoryProbe.Phase(HerbertMemoryProbe.Stage.BASELINE), true)
        val original = probe.snapshot()
        value = 500; probe.sample(HerbertMemoryProbe.Phase(HerbertMemoryProbe.Stage.LOADED), true)
        value = 80; probe.sample(HerbertMemoryProbe.Phase(HerbertMemoryProbe.Stage.CLOSED), true)
        assertEquals(1, original.size)
        assertEquals(500L, probe.maximumKiB())
        assertEquals(80L, probe.snapshot().last().lastKiB)
        assertEquals(-20L, probe.snapshot().last().maxKiB - probe.baselineKiB())
        assertThrows(UnsupportedOperationException::class.java) {
            (original as MutableList<HerbertMemoryProbe.Sample>).clear()
        }
    }

    @Test fun invalidMemoryAndCasePhasesRejectedAndTimingQuantilesUseNearestRank() {
        assertThrows(IllegalArgumentException::class.java) {
            HerbertMemoryProbe({ -1 }, { 0L }).sample(HerbertMemoryProbe.Phase(HerbertMemoryProbe.Stage.BASELINE))
        }
        assertThrows(IllegalArgumentException::class.java) { HerbertMemoryProbe.Phase(HerbertMemoryProbe.Stage.WORKLOAD) }
        assertThrows(IllegalArgumentException::class.java) { HerbertMemoryProbe.Phase(HerbertMemoryProbe.Stage.LOADED, 1, 32) }
        val timing = HerbertConformance.timing((1..100).map { it.toDouble() })
        assertEquals(100, timing.samples)
        assertEquals(50.5, timing.p50Ms, 0.0)
        assertEquals(95.0, timing.p95Ms, 0.0)
        assertEquals(2.0, HerbertConformance.timing(listOf(3.0, 1.0, 2.0)).p50Ms, 0.0)
        for (values in listOf(emptyList(), listOf(Double.NaN), listOf(-1.0))) {
            assertThrows(IllegalArgumentException::class.java) { HerbertConformance.timing(values) }
        }
    }

    @Test fun reportedWordCountsFollowSameUnicodeBoundariesAndWindowLimits() {
        assertEquals(3, HerbertContextWindow.countWords(" Ala\u00a0ma\u0085kota. "))
        val context = "Podczas tej podróży poznaliśmy nowe miejsca i wiele osób. ".repeat(5) + "Jedziemy teraz do "
        assertEquals(48, HerbertContextWindow.countWords(context))
        assertEquals(32, HerbertContextWindow.countWords(HerbertContextWindow.retain(context, 32)))
        assertEquals(48, HerbertContextWindow.countWords(HerbertContextWindow.retain(context, 64)))
    }
}
