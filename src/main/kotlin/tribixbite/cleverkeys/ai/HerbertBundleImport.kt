package tribixbite.cleverkeys.ai

import tribixbite.cleverkeys.langpack.IntelligenceJson
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream
import java.util.concurrent.CancellationException

internal data class HerbertFileIdentity(val bytes: Long, val sha256: String)

/** Construct from verified CI provenance/compiled trial constants, never an imported manifest. */
internal class HerbertBundleTrust(files: Map<String, HerbertFileIdentity>) {
    val files: Map<String, HerbertFileIdentity> = java.util.Collections.unmodifiableMap(HashMap(files))
    init {
        require(this.files.keys == MEMBERS)
        require(this.files.values.all { it.bytes > 0 && it.sha256.matches(Regex("[a-f0-9]{64}")) })
        require(this.files.getValue("model.onnx") == HerbertFileIdentity(MODEL_BYTES, MODEL_SHA256))
        require(this.files.filterKeys { it != "model.onnx" }.values.all { it.bytes <= MAX_METADATA_BYTES })
        require(this.files.filterKeys { it != "model.onnx" }.values.sumOf { it.bytes } <= MAX_METADATA_BYTES)
    }

    companion object {
        const val MODEL_BYTES = 651_798_883L
        const val MODEL_SHA256 = "f851436ba9ca35d0c7313ff873cd869b744b295a8a16fc95b4571d5e11b299f2"
        const val MAX_METADATA_BYTES = 32L * 1024 * 1024
        val MEMBERS = setOf("model.onnx", "tokenizer.json", "portable-tokenizer.json",
            "tokenizer-conformance.json", "android-score-vectors.json", "NOTICE.txt", "manifest.json")
    }
}

/** A private, unique staging snapshot. Its owner must serialize open/inference/delete on one worker. */
internal class HerbertImportedBundle internal constructor(
    val directory: File,
    val tokenizer: HerbertTokenizer,
    val trust: HerbertBundleTrust,
) : AutoCloseable {
    val model: File get() = File(directory, "model.onnx")
    fun reader(name: String): InputStreamReader {
        require(name in trust.files.keys && name != "model.onnx")
        return InputStreamReader(File(directory, name).inputStream(), Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT))
    }
    override fun close() { check(directory.deleteRecursively()) { "Cannot remove HerBERT staging files" } }
}

internal object HerbertBundleImport {
    /** Parent must be app-private noBackupFilesDir. No overwrite of an existing model. */
    fun stage(input: InputStream, parent: File, trust: HerbertBundleTrust,
              cancelled: () -> Boolean = { false }): HerbertImportedBundle {
        require(parent.isDirectory || parent.mkdirs())
        val directory = File(parent, "herbert-trial-${UUID.randomUUID()}")
        check(directory.mkdir())
        var accepted = false
        try {
            val seen = HashSet<String>()
            var total = 0L
            ZipInputStream(input).use { zip ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    if (cancelled()) throw CancellationException()
                    val entry = zip.nextEntry ?: break
                    require(!entry.isDirectory && entry.name in trust.files && seen.add(entry.name)) {
                        "Unknown, duplicate or unsafe HerBERT member"
                    }
                    val identity = trust.files.getValue(entry.name)
                    require(entry.size == -1L || entry.size == identity.bytes) { "HerBERT member size differs" }
                    val destination = File(directory, entry.name)
                    check(destination.createNewFile())
                    val digest = MessageDigest.getInstance("SHA-256")
                    var count = 0L
                    destination.outputStream().use { out ->
                        while (true) {
                            if (cancelled()) throw CancellationException()
                            val n = zip.read(buffer)
                            if (n < 0) break
                            count += n
                            total += n
                            require(count <= identity.bytes && total <= HerbertBundleTrust.MODEL_BYTES +
                                HerbertBundleTrust.MAX_METADATA_BYTES) { "HerBERT decompressed size limit" }
                            digest.update(buffer, 0, n)
                            out.write(buffer, 0, n)
                        }
                    }
                    require(count == identity.bytes && hex(digest.digest()) == identity.sha256) {
                        "HerBERT member hash/size mismatch"
                    }
                    zip.closeEntry()
                }
            }
            require(seen == trust.files.keys) { "Incomplete HerBERT bundle" }
            fun reader(name: String) = InputStreamReader(File(directory, name).inputStream(),
                Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT))
            val manifest = reader("manifest.json").use { IntelligenceJson.document(it) }
            require(IntelligenceJson.integer(manifest, "schemaVersion") == 1)
            require(IntelligenceJson.string(manifest, "protocol") == HerbertTokenizer.PROTOCOL)
            require(IntelligenceJson.string(manifest, "onnxRuntimeVersion") == "1.21.1")
            require(manifest.get("benchmarkOnly").asBoolean && manifest.get("floatParityPassed").asBoolean &&
                !manifest.get("phoneReady").asBoolean)
            val declarations = manifest.getAsJsonObject("files")
            require(declarations.keySet() == trust.files.keys - "manifest.json")
            for ((name, identity) in trust.files.filterKeys { it != "manifest.json" }) {
                val row = declarations.getAsJsonObject(name)
                require(IntelligenceJson.string(row, "sha256") == identity.sha256 &&
                    row.get("bytes").asBigDecimal.longValueExact() == identity.bytes)
            }
            val tokenizer = reader("portable-tokenizer.json").use { HerbertTokenizer.parse(it) }
            val bundle = HerbertImportedBundle(directory, tokenizer, trust)
            accepted = true
            return bundle
        } finally {
            if (!accepted) directory.deleteRecursively()
        }
    }

    internal fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
