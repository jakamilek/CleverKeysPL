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
        status = text(if (HerbertBenchmarkTrial.trust() == null) R.string.herbert_benchmark_pending
            else R.string.herbert_benchmark_no_model)
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
        button(R.string.herbert_benchmark_copy) {
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
        val trust = HerbertBenchmarkTrial.trust() ?: return
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
            } catch (_: Exception) {
                ui { status.setText(R.string.herbert_benchmark_import_failed) }
            } finally { ui { busy(false) } }
        }
    }

    private fun runBenchmark() {
        cancelled.set(false)
        report = ""
        output.text = ""
        busy(true)
        status.setText(R.string.herbert_benchmark_running)
        worker.execute {
            try {
                var maximumSampledPss = 0L
                var lastSample = 0L
                fun sample() {
                    val now = System.nanoTime()
                    if (now - lastSample >= 250_000_000L) {
                        maximumSampledPss = maxOf(maximumSampledPss, Debug.getPss())
                        lastSample = now
                    }
                }
                sample()
                val bundle = imported ?: error("Missing model")
                val result = HerbertConformance.benchmark(bundle, { cancelled.get() || destroyed.get() }, ::sample)
                val identity = "${Build.MANUFACTURER} ${Build.MODEL}; Android ${Build.VERSION.RELEASE}; ${Build.SUPPORTED_ABIS.firstOrNull()}"
                val value = getString(R.string.herbert_benchmark_report, identity,
                    result.tokenizerVectors, result.scoreVectors, result.maxAbsoluteError.toDouble(),
                    result.loadMs, result.total.samples, result.feed.p50Ms, result.feed.p95Ms,
                    result.inference.p50Ms, result.inference.p95Ms, result.total.p50Ms, result.total.p95Ms,
                    maximumSampledPss / 1024.0)
                ui { report = value; output.text = value; status.setText(R.string.herbert_benchmark_passed) }
            } catch (_: CancellationException) {
                ui { status.setText(R.string.herbert_benchmark_cancelled) }
            } catch (_: Exception) {
                ui { status.setText(R.string.herbert_benchmark_test_failed) }
            } finally { ui { busy(false) } }
        }
    }

    private fun busy(value: Boolean) {
        importButton.isEnabled = !value && HerbertBenchmarkTrial.trust() != null
        runButton.isEnabled = !value && hasBundle
        removeButton.isEnabled = !value && hasBundle
        noticeButton.isEnabled = !value && hasBundle
        cancelButton.isEnabled = value
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
