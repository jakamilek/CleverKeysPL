package tribixbite.cleverkeys.ai

import tribixbite.cleverkeys.langpack.IntelligenceJson
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile
import java.util.zip.ZipException
import java.util.concurrent.CancellationException

internal data class HerbertFileIdentity(val bytes: Long, val sha256: String)

internal enum class HerbertImportReason { STORAGE, READ, ZIP, CONTENTS, IDENTITY, METADATA }

/** Report only a fixed reason, never provider URIs, paths or exception messages. */
internal class HerbertImportFailure(val reason: HerbertImportReason) : IllegalArgumentException(reason.name)

/** GitHub compression-level 0 uses STORED entries with trailing data descriptors.
 * ZipInputStream rejects them; ZipFile reads their sizes from the central directory.
 * Copy the container privately with a bound, then authenticate each extracted byte.
 */
internal object HerbertZipArchive {
    private const val ZIP_OVERHEAD_BYTES = 1024L * 1024
    private const val STORAGE_RESERVE_BYTES = 16L * 1024 * 1024

    fun requiredFreeBytes(files: Map<String, HerbertFileIdentity>): Long =
        2 * files.values.sumOf { it.bytes } + ZIP_OVERHEAD_BYTES + STORAGE_RESERVE_BYTES

    fun extract(input: InputStream, directory: File, files: Map<String, HerbertFileIdentity>,
                cancelled: () -> Boolean = { false }) {
        require(files.isNotEmpty() && files.keys.all { it in HerbertBundleTrust.MEMBERS })
        require(files.values.all { it.bytes > 0 })
        val archive = File(directory, ".import.zip")
        val expectedBytes = files.values.sumOf { it.bytes }
        fun checkCancelled() { if (cancelled()) throw CancellationException() }
        fun refuse(reason: HerbertImportReason): Nothing = throw HerbertImportFailure(reason)
        try {
            checkCancelled()
            if (directory.usableSpace < requiredFreeBytes(files)) refuse(HerbertImportReason.STORAGE)
            check(archive.createNewFile())
            val buffer = ByteArray(64 * 1024)
            var copied = 0L
            archive.outputStream().use { out ->
                while (true) {
                    checkCancelled()
                    val n = input.read(buffer)
                    if (n < 0) break
                    copied += n
                    if (copied > expectedBytes + ZIP_OVERHEAD_BYTES) refuse(HerbertImportReason.ZIP)
                    out.write(buffer, 0, n)
                }
            }
            checkCancelled()
            checkDirectoryBound(archive, files.size)
            ZipFile(archive).use { zip ->
                val entries = zip.entries()
                val seen = HashSet<String>()
                val members = ArrayList<java.util.zip.ZipEntry>()
                while (entries.hasMoreElements()) {
                    checkCancelled()
                    val entry = entries.nextElement()
                    if (entry.isDirectory || entry.name !in files || !seen.add(entry.name))
                        refuse(HerbertImportReason.CONTENTS)
                    if (entry.size != files.getValue(entry.name).bytes) refuse(HerbertImportReason.IDENTITY)
                    members.add(entry)
                }
                if (seen != files.keys) refuse(HerbertImportReason.CONTENTS)
                var total = 0L
                for (entry in members) {
                    checkCancelled()
                    val identity = files.getValue(entry.name)
                    val destination = File(directory, entry.name)
                    check(destination.createNewFile())
                    val digest = MessageDigest.getInstance("SHA-256")
                    var count = 0L
                    zip.getInputStream(entry).use { source ->
                        destination.outputStream().use { out ->
                            while (true) {
                                checkCancelled()
                                val n = source.read(buffer)
                                if (n < 0) break
                                count += n
                                total += n
                                if (count > identity.bytes || total > expectedBytes)
                                    refuse(HerbertImportReason.IDENTITY)
                                digest.update(buffer, 0, n)
                                out.write(buffer, 0, n)
                            }
                        }
                    }
                    if (count != identity.bytes || HerbertBundleImport.hex(digest.digest()) != identity.sha256)
                        refuse(HerbertImportReason.IDENTITY)
                }
            }
        } catch (failure: ZipException) {
            throw HerbertImportFailure(HerbertImportReason.ZIP)
        } catch (failure: IOException) {
            throw HerbertImportFailure(if (directory.usableSpace < STORAGE_RESERVE_BYTES)
                HerbertImportReason.STORAGE else HerbertImportReason.READ)
        } finally {
            if (archive.exists() && !archive.delete()) directory.deleteRecursively()
        }
    }

    /** Bound the central-directory index before ZipFile can allocate it. This pinned
     * seven-member, <4 GiB trial needs neither multidisk nor ZIP64 directory records.
     * Local ZIP64 extra fields remain supported by ZipFile.
     */
    private fun checkDirectoryBound(archive: File, maximumEntries: Int) {
        fun refuse(): Nothing = throw HerbertImportFailure(HerbertImportReason.ZIP)
        RandomAccessFile(archive, "r").use { file ->
            val length = file.length()
            if (length < 22) refuse()
            val tail = ByteArray(minOf(length, 65_557L).toInt())
            file.seek(length - tail.size)
            file.readFully(tail)
            fun u16(i: Int) = (tail[i].toInt() and 255) or ((tail[i + 1].toInt() and 255) shl 8)
            fun u32(i: Int) = u16(i).toLong() or (u16(i + 2).toLong() shl 16)
            for (i in tail.size - 22 downTo 0) {
                if (u32(i) != 0x06054b50L || i + 22 + u16(i + 20) != tail.size) continue
                val entries = u16(i + 10)
                val size = u32(i + 12)
                val offset = u32(i + 16)
                val endOffset = length - tail.size + i
                if (u16(i + 4) != 0 || u16(i + 6) != 0 || u16(i + 8) != entries ||
                    entries > maximumEntries || size > ZIP_OVERHEAD_BYTES || offset + size != endOffset) refuse()
                return
            }
            refuse()
        }
    }
}

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
            HerbertZipArchive.extract(input, directory, trust.files, cancelled)
            if (cancelled()) throw CancellationException()
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
            if (cancelled()) throw CancellationException()
            val bundle = HerbertImportedBundle(directory, tokenizer, trust)
            accepted = true
            return bundle
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: HerbertImportFailure) {
            throw failure
        } catch (failure: Exception) {
            throw HerbertImportFailure(HerbertImportReason.METADATA)
        } finally {
            if (!accepted) directory.deleteRecursively()
        }
    }

    internal fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
