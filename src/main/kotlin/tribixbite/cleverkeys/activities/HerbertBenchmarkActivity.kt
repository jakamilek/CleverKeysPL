package tribixbite.cleverkeys

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import tribixbite.cleverkeys.ai.HerbertBenchmarkTrial
import tribixbite.cleverkeys.ai.HerbertBundleImport
import tribixbite.cleverkeys.ai.HerbertConformance
import tribixbite.cleverkeys.ai.HerbertImportedBundle
import tribixbite.cleverkeys.ai.HerbertImportFailure
import tribixbite.cleverkeys.ai.HerbertImportReason
import tribixbite.cleverkeys.ai.HerbertMemoryProbe
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File

/** Explicit benchmark only. No IME/editor access, inference/network/logging or preferences. */
class HerbertBenchmarkActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val destroyed = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)
    // Worker-owned; loading, score, native close and staging deletion never overlap.
    private var imported: HerbertImportedBundle? = null
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var importButton: Button
    private lateinit var runButton: Button
    private lateinit var removeButton: Button
    private lateinit var noticeButton: Button
    private lateinit var cancelButton: Button
    private lateinit var copyButton: Button
    private var trialNumber = 0
    private var hasBundle = false // main-thread UI state only
    private var report = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val scroll = ScrollView(this).apply { addView(column) }
        setContentView(scroll)
        fun text(id: Int) = TextView(this).apply { setText(id); column.addView(this) }
        fun button(id: Int, action: () -> Unit) = Button(this).apply {
            setText(id); setOnClickListener { action() }; column.addView(this)
        }
        text(R.string.herbert_benchmark_title).textSize = 22f
        text(R.string.herbert_benchmark_explanation)
        status = text(R.string.herbert_benchmark_no_model)
        importButton = button(R.string.herbert_benchmark_import) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed"))
            }, IMPORT_REQUEST)
        }
        runButton = button(R.string.herbert_benchmark_run) { runBenchmark() }
        cancelButton = button(R.string.herbert_benchmark_cancel) { cancelled.set(true) }
        removeButton = button(R.string.herbert_benchmark_remove) {
            busy(true)
            worker.execute {
                imported?.close(); imported = null
                ui { hasBundle = false; report = ""; output.text = ""; status.setText(R.string.herbert_benchmark_no_model); busy(false) }
            }
        }
        noticeButton = button(R.string.herbert_benchmark_notice) {
            worker.execute {
                val notice = imported?.reader("NOTICE.txt")?.use { it.readText() }.orEmpty()
                ui { AlertDialog.Builder(this).setTitle(R.string.herbert_benchmark_notice)
                    .setMessage(notice).setPositiveButton(android.R.string.ok, null).show() }
            }
        }
        copyButton = button(R.string.herbert_benchmark_copy) {
            if (report.isNotEmpty()) {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText(getString(R.string.herbert_benchmark_title), report))
            }
        }
        button(R.string.herbert_benchmark_back) { finish() }
        output = TextView(this).apply { setTextIsSelectable(true); column.addView(this) }
        busy(false)
        // A killed process may never receive onDestroy. Clear its private staging
        // before this worker can import another snapshot; nothing is in backups.
        worker.execute {
            File(noBackupFilesDir, "herbert-benchmark").listFiles()?.forEach {
                if (it.name.matches(Regex("herbert-trial-[a-f0-9-]{36}"))) it.deleteRecursively()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != IMPORT_REQUEST || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val trust = HerbertBenchmarkTrial.trust()
        cancelled.set(false)
        busy(true)
        status.setText(R.string.herbert_benchmark_importing)
        worker.execute {
            try {
                val staged = contentResolver.openInputStream(uri)?.use {
                    HerbertBundleImport.stage(it, File(noBackupFilesDir, "herbert-benchmark"), trust) {
                        cancelled.get() || destroyed.get()
                    }
                } ?: error("Missing import stream")
                try { imported?.close() } catch (failure: Exception) { staged.close(); throw failure }
                imported = staged
                ui { hasBundle = true; report = ""; output.text = ""; status.setText(R.string.herbert_benchmark_ready) }
            } catch (_: CancellationException) {
                ui {
                    report = ""; output.text = ""
                    status.setText(R.string.herbert_benchmark_cancelled)
                }
            } catch (failure: Exception) {
                val reason = (failure as? HerbertImportFailure)?.reason ?: HerbertImportReason.READ
                val explanation = when (reason) {
                    HerbertImportReason.STORAGE -> R.string.herbert_benchmark_import_storage
                    HerbertImportReason.READ -> R.string.herbert_benchmark_import_read
                    HerbertImportReason.ZIP -> R.string.herbert_benchmark_import_zip
                    HerbertImportReason.CONTENTS -> R.string.herbert_benchmark_import_contents
                    HerbertImportReason.IDENTITY -> R.string.herbert_benchmark_import_identity
                    HerbertImportReason.METADATA -> R.string.herbert_benchmark_import_metadata
                }
                ui {
                    status.setText(R.string.herbert_benchmark_import_failed)
                    report = getString(R.string.herbert_benchmark_import_error_report, reason.name,
                        getString(explanation), "${Build.MANUFACTURER} ${Build.MODEL}; Android ${Build.VERSION.RELEASE}")
                    output.text = report
                }
            } finally { ui { busy(false) } }
        }
    }

    private fun runBenchmark() {
        cancelled.set(false)
        report = ""
        output.text = ""
        val thisTrial = ++trialNumber
        busy(true)
        status.setText(R.string.herbert_benchmark_running)
        worker.execute {
            val memory = HerbertMemoryProbe(readKiB = { Debug.getPss() })
            var identity = ""
            var lastProgress = 0L
            var lastStage: HerbertMemoryProbe.Stage? = null
            try {
                val bundle = imported ?: error("Missing model")
                // Installed base APK fingerprint, never its private filename/URI.
                val digest = MessageDigest.getInstance("SHA-256")
                File(applicationInfo.sourceDir).inputStream().use { source ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (cancelled.get() || destroyed.get()) throw CancellationException()
                        val n = source.read(buffer)
                        if (n < 0) break
                        digest.update(buffer, 0, n)
                    }
                }
                identity = getString(R.string.herbert_benchmark_identity_report,
                    BuildConfig.HERBERT_TRIAL_COMMIT, HerbertBundleImport.hex(digest.digest()),
                    bundle.trust.files.getValue("model.onnx").sha256, thisTrial, android.os.Process.myPid())
                val result = HerbertConformance.benchmark(bundle,
                    cancelled = { cancelled.get() || destroyed.get() },
                    checkpoint = { phase, force -> memory.sample(phase, force) },
                    progress = { value ->
                        val now = System.nanoTime()
                        if (lastStage != value.stage || now - lastProgress >= 500_000_000L ||
                            value.total > 0 && value.completed == value.total) {
                            lastStage = value.stage; lastProgress = now
                            val message = when (value.stage) {
                                HerbertMemoryProbe.Stage.LOADED -> getString(R.string.herbert_benchmark_progress_load)
                                HerbertMemoryProbe.Stage.FIRST_PAIR -> getString(R.string.herbert_benchmark_progress_first)
                                HerbertMemoryProbe.Stage.WARMUP -> getString(R.string.herbert_benchmark_progress_warmup)
                                HerbertMemoryProbe.Stage.WORKLOAD -> getString(R.string.herbert_benchmark_progress_workload, value.completed, value.total)
                                HerbertMemoryProbe.Stage.CONFORMANCE -> getString(R.string.herbert_benchmark_progress_conformance, value.completed, value.total)
                                else -> getString(R.string.herbert_benchmark_running)
                            }
                            ui { status.text = message }
                        }
                    })
                val device = "${Build.MANUFACTURER} ${Build.MODEL}; Android ${Build.VERSION.RELEASE}; ${Build.SUPPORTED_ABIS.firstOrNull()}"
                val windowReport = result.windows.joinToString("\n") { window ->
                    getString(R.string.herbert_benchmark_window_report, window.words, window.total.samples,
                        window.feed.p50Ms, window.feed.p95Ms, window.inference.p50Ms, window.inference.p95Ms,
                        window.total.p50Ms, window.total.p95Ms)
                }
                val caseReport = result.cases.joinToString("\n") { row ->
                    getString(R.string.herbert_benchmark_case_report, row.context, row.words, row.retainedWords,
                        row.batch, row.sequence, row.targets, row.total.samples,
                        row.feed.p50Ms, row.feed.p95Ms, row.inference.p50Ms, row.inference.p95Ms,
                        row.total.p50Ms, row.total.p95Ms)
                }
                val details = listOf(identity,
                    getString(R.string.herbert_benchmark_first_report, result.firstPairMs, result.durationMs),
                    windowReport, caseReport, memoryReport(memory)).joinToString("\n\n")
                val value = getString(R.string.herbert_benchmark_report, device,
                    result.tokenizerVectors, result.scoreVectors, result.maxAbsoluteError.toDouble(),
                    result.loadMs, result.total.samples, result.feed.p50Ms, result.feed.p95Ms,
                    result.inference.p50Ms, result.inference.p95Ms, result.total.p50Ms, result.total.p95Ms,
                    memory.maximumKiB() / 1024.0, details)
                ui { report = value; output.text = value; status.setText(R.string.herbert_benchmark_passed) }
            } catch (_: CancellationException) {
                ui {
                    report = getString(R.string.herbert_benchmark_partial_report,
                        getString(R.string.herbert_benchmark_cancelled), identity, memoryReport(memory))
                    output.text = report; status.setText(R.string.herbert_benchmark_cancelled)
                }
            } catch (_: Exception) {
                ui {
                    report = getString(R.string.herbert_benchmark_partial_report,
                        getString(R.string.herbert_benchmark_test_failed), identity, memoryReport(memory))
                    output.text = report; status.setText(R.string.herbert_benchmark_test_failed)
                }
            } finally { ui { busy(false) } }
        }
    }

    private fun memoryReport(memory: HerbertMemoryProbe): String {
        val rows = memory.snapshot()
        if (rows.isEmpty()) return getString(R.string.herbert_benchmark_memory_unavailable)
        val baseline = memory.baselineKiB()
        fun label(phase: HerbertMemoryProbe.Phase): String = when (phase.stage) {
            HerbertMemoryProbe.Stage.BASELINE -> getString(R.string.herbert_benchmark_phase_baseline)
            HerbertMemoryProbe.Stage.LOADED -> getString(R.string.herbert_benchmark_phase_loaded)
            HerbertMemoryProbe.Stage.FIRST_PAIR -> getString(R.string.herbert_benchmark_phase_first)
            HerbertMemoryProbe.Stage.CONFORMANCE -> getString(R.string.herbert_benchmark_phase_conformance)
            HerbertMemoryProbe.Stage.CLOSED -> getString(R.string.herbert_benchmark_phase_closed)
            HerbertMemoryProbe.Stage.WARMUP -> getString(R.string.herbert_benchmark_phase_case,
                getString(R.string.herbert_benchmark_phase_warmup), phase.context, phase.words)
            HerbertMemoryProbe.Stage.WORKLOAD -> getString(R.string.herbert_benchmark_phase_case,
                getString(R.string.herbert_benchmark_phase_workload), phase.context, phase.words)
        }
        return getString(R.string.herbert_benchmark_memory_explanation) + "\n" + rows.joinToString("\n") { row ->
            getString(R.string.herbert_benchmark_memory_row, label(row.phase), row.firstKiB / 1024.0,
                row.lastKiB / 1024.0, row.maxKiB / 1024.0, (row.maxKiB - baseline) / 1024.0, row.samples)
        }
    }

    private fun busy(value: Boolean) {
        importButton.isEnabled = !value
        runButton.isEnabled = !value && hasBundle
        removeButton.isEnabled = !value && hasBundle
        noticeButton.isEnabled = !value && hasBundle
        cancelButton.isEnabled = value
        copyButton.isEnabled = !value && report.isNotEmpty()
    }

    private fun ui(action: () -> Unit) {
        if (!destroyed.get()) runOnUiThread { if (!destroyed.get()) action() }
    }

    override fun onStop() { cancelled.set(true); super.onStop() }

    override fun onDestroy() {
        destroyed.set(true)
        cancelled.set(true)
        worker.execute { imported?.close(); imported = null }
        worker.shutdown()
        super.onDestroy()
    }

    companion object { private const val IMPORT_REQUEST = 4104 }
}
