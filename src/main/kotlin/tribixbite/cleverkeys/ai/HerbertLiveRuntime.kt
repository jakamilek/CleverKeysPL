package tribixbite.cleverkeys.ai

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** One process-local owner. No editor objects, strings in reports, network, or weight backups. */
internal object HerbertLiveRuntime {
    enum class State { ABSENT, OFF, LOADING, READY, FAILED }
    @Volatile var state = State.OFF; private set
    @Volatile var revision = 0L; private set
    @Volatile var analyses = 0L; private set
    @Volatile var adopted = 0L; private set
    @Volatile var fallbacks = 0L; private set
    @Volatile var lastMs = 0.0; private set
    private val main by lazy { Handler(Looper.getMainLooper()) }
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "Herbert-live") }
    private val scoring = AtomicBoolean(false)
    @Synchronized private fun stateIfCurrent(token: Long, value: State) {
        if (token == revision) state = value
    }
    private var initialized = false
    private var wanted = false
    // Only the worker may read or close these. Closing a session always precedes file deletion.
    private var bundle: HerbertImportedBundle? = null
    private var scorer: HerbertOnnxScorer? = null
    private fun root(context: Context) = File(context.noBackupFilesDir, "herbert-live")
    private fun pointer(context: Context) = AtomicFile(File(root(context), "active"))

    @Synchronized fun configure(context: Context, enabled: Boolean) {
        if (initialized && wanted == enabled) return
        initialized = true; wanted = enabled
        val token = ++revision
        state = if (enabled) State.LOADING else State.OFF
        if (!enabled) { worker.execute { closeSession() }; return }
        val app = context.applicationContext
        worker.execute {
            closeSession()
            if (token != revision) return@execute
            try {
                val stored = restore(app) ?: run { stateIfCurrent(token, State.ABSENT); return@execute }
                bundle = stored
                val opened = HerbertOnnxScorer.open(stored)
                scorer = opened
                fun active() { if (token != revision) throw CancellationException() }
                active()
                val tokens = stored.reader("tokenizer-conformance.json").use { HerbertConformance.tokens(stored.tokenizer, it) }
                require(tokens == 2471)
                val vectors = stored.reader("android-score-vectors.json").use { HerbertConformance.feeds(stored.tokenizer, it) }
                require(vectors.size == 232 && vectors.sumOf { it.batch.size } == 532)
                for (vector in vectors) {
                    active()
                    val actual = opened.score(vector.batch)
                    require(actual.keys == vector.expected.keys)
                    require(vector.batch.surfaces.sortedByDescending { actual.getValue(it) } ==
                        vector.batch.surfaces.sortedByDescending { vector.expected.getValue(it) })
                    require(actual.all { (surface, score) -> kotlin.math.abs(score - vector.expected.getValue(surface)) <= 0.001f })
                }
                active()
                stateIfCurrent(token, State.READY)
            } catch (_: Exception) {
                closeSession()
                stateIfCurrent(token, State.FAILED)
            }
        }
    }

    /** SAF import uses the immutable seven-file benchmark identity; provenance is not rewritten. */
    @Synchronized fun importBundle(context: Context, uri: Uri, complete: (Boolean) -> Unit) {
        initialized = true; wanted = false
        val token = ++revision
        state = State.LOADING
        val app = context.applicationContext
        worker.execute {
            closeSession()
            var staged: HerbertImportedBundle? = null
            var installed = false
            try {
                val parent = root(app).apply { mkdirs() }
                val value = app.contentResolver.openInputStream(uri)?.use {
                    HerbertBundleImport.stage(it, parent, HerbertBenchmarkTrial.trust()) { token != revision }
                } ?: error("Missing stream")
                staged = value
                require(value.reader("tokenizer-conformance.json").use { HerbertConformance.tokens(value.tokenizer, it) } == 2471)
                value.reader("android-score-vectors.json").use { HerbertConformance.feeds(value.tokenizer, it) }
                if (token != revision) throw CancellationException()
                val active = pointer(app)
                val output = active.startWrite()
                try {
                    output.write(value.directory.name.toByteArray(Charsets.US_ASCII)); active.finishWrite(output)
                } catch (failure: Exception) { active.failWrite(output); throw failure }
                installed = true
                // Native conformance runs on enable, before any live request can be accepted.
                cleanup(parent, value.directory)
                stateIfCurrent(token, State.OFF)
            } catch (_: Exception) {
                if (!installed) staged?.close()
                stateIfCurrent(token, State.FAILED)
            }
            main.post { complete(installed && token == revision) }
        }
    }

    @Synchronized fun remove(context: Context, complete: () -> Unit) {
        initialized = true; wanted = false
        val token = ++revision
        state = State.OFF
        val app = context.applicationContext
        worker.execute {
            closeSession()
            pointer(app).delete()
            cleanup(root(app), null)
            stateIfCurrent(token, State.ABSENT)
            main.post { complete() }
        }
    }

    /** At most one score is in flight: busy/loading/off gives immediate geometric fallback. */
    fun rank(context: String, group: HerbertFormGroup, words: Int, complete: (Map<String, Float>?, Long) -> Unit): Boolean {
        if (state != State.READY || !scoring.compareAndSet(false, true)) return false
        val token = revision
        worker.execute {
            var scores: Map<String, Float>? = null
            val start = System.nanoTime()
            try {
                if (token == revision && state == State.READY) {
                    val b = bundle ?: error("No bundle")
                    scores = scorer?.score(b.tokenizer.prepare(context, group.surfaces, words))
                    analyses++
                    lastMs = (System.nanoTime() - start) / 1_000_000.0
                }
            } catch (_: Exception) { /* No editor strings or exception messages escape. */ }
            finally { scoring.set(false) }
            val result = scores.takeIf { token == revision && state == State.READY }
            main.post { complete(result, token) }
        }
        return true
    }

    fun noteResult(used: Boolean) { if (used) adopted++ else fallbacks++ }
    fun hasModel(context: Context): Boolean = pointer(context).baseFile.isFile

    private fun closeSession() {
        try { scorer?.close() } finally { scorer = null; bundle = null }
    }

    private fun restore(context: Context): HerbertImportedBundle? {
        val active = pointer(context)
        if (!active.baseFile.isFile) return null
        require(active.baseFile.length() in 1..64)
        val name = active.openRead().use { String(it.readBytes(), Charsets.US_ASCII) }
        require(name.matches(Regex("herbert-trial-[a-f0-9-]{36}")))
        val directory = File(root(context), name)
        val trust = HerbertBenchmarkTrial.trust()
        require(directory.listFiles()?.map { it.name }?.toSet() == trust.files.keys)
        for ((member, identity) in trust.files) {
            val file = File(directory, member)
            require(file.isFile && file.length() == identity.bytes)
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
            }
            require(HerbertBundleImport.hex(digest.digest()) == identity.sha256)
        }
        val tokenizer = File(directory, "portable-tokenizer.json").reader(Charsets.UTF_8).use { HerbertTokenizer.parse(it) }
        cleanup(root(context), directory)
        return HerbertImportedBundle(directory, tokenizer, trust)
    }

    private fun cleanup(parent: File, keep: File?) {
        parent.listFiles()?.filter { it != keep && it.name.matches(Regex("herbert-trial-[a-f0-9-]{36}")) }
            ?.forEach { check(it.deleteRecursively()) }
    }
}
