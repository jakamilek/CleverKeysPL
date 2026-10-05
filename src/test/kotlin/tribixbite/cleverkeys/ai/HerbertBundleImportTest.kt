package tribixbite.cleverkeys.ai

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipInputStream
import java.util.zip.ZipException
import java.util.concurrent.CancellationException

class HerbertBundleImportTest {
    private fun trust() = HerbertBundleTrust(HerbertBundleTrust.MEMBERS.associateWith {
        if (it == "model.onnx") HerbertFileIdentity(HerbertBundleTrust.MODEL_BYTES, HerbertBundleTrust.MODEL_SHA256)
        else HerbertFileIdentity(1, "0".repeat(64))
    })
    private fun archive(name: String, content: String): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray()); zip.closeEntry()
        }
        return buffer.toByteArray()
    }
    private fun rejects(bytes: ByteArray) {
        val parent = Files.createTempDirectory("herbert-import-test").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                HerbertBundleImport.stage(ByteArrayInputStream(bytes), parent, trust())
            }
            assertEquals(emptyList<File>(), parent.listFiles()!!.toList())
        } finally { parent.deleteRecursively() }
    }
    @Test fun traversalUnknownAndDirectoriesRefused() {
        for (name in listOf("../NOTICE.txt", "/NOTICE.txt", "folder/NOTICE.txt", "folder/", "extra.txt")) {
            rejects(archive(name, "x"))
        }
    }
    @Test fun forgedManifestCannotAuthenticateItself() { rejects(archive("manifest.json", "x")) }
    @Test fun oversizedActualMemberRefusedAndStagingCleaned() { rejects(archive("NOTICE.txt", "xx")) }
    @Test fun truncatedOrIncompleteBundleRefused() {
        rejects(archive("model.onnx", "x"))
        rejects(byteArrayOf())
    }
    @Test fun trustRequiresEveryMemberAndVerifiedFp32Identity() {
        assertThrows(IllegalArgumentException::class.java) { HerbertBundleTrust(emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) {
            HerbertBundleTrust(trust().files + ("model.onnx" to HerbertFileIdentity(1, "0".repeat(64))))
        }
    }

    /** Same ZIP envelope as upload-artifact with compression-level: 0:
     * STORED streams, bit 3, zero local sizes and a trailing descriptor.
     * Tiny members test the container reader, never authenticate a fake model.
     */
    private fun actionsArchive(rows: List<Pair<String, ByteArray>>): ByteArray {
        val out = ByteArrayOutputStream()
        fun u16(value: Int) { repeat(2) { out.write((value ushr (8 * it)) and 255) } }
        fun u32(value: Long) { repeat(4) { out.write(((value ushr (8 * it)) and 255).toInt()) } }
        val offsets = ArrayList<Int>()
        for ((name, bytes) in rows) {
            val key = name.toByteArray(Charsets.UTF_8)
            val crc = CRC32().apply { update(bytes) }.value
            offsets.add(out.size())
            u32(0x04034b50); u16(20); u16(8); u16(0); u16(0); u16(0)
            u32(0); u32(0); u32(0); u16(key.size); u16(0); out.write(key); out.write(bytes)
            u32(0x08074b50); u32(crc); u32(bytes.size.toLong()); u32(bytes.size.toLong())
        }
        val centralOffset = out.size()
        for ((i, row) in rows.withIndex()) {
            val (name, bytes) = row
            val key = name.toByteArray(Charsets.UTF_8)
            val crc = CRC32().apply { update(bytes) }.value
            u32(0x02014b50); u16(20); u16(20); u16(8); u16(0); u16(0); u16(0)
            u32(crc); u32(bytes.size.toLong()); u32(bytes.size.toLong())
            u16(key.size); u16(0); u16(0); u16(0); u16(0); u32(0)
            u32(offsets[i].toLong()); out.write(key)
        }
        val centralSize = out.size() - centralOffset
        u32(0x06054b50); u16(0); u16(0); u16(rows.size); u16(rows.size)
        u32(centralSize.toLong()); u32(centralOffset.toLong()); u16(0)
        return out.toByteArray()
    }

    private fun identities(rows: Map<String, ByteArray>) = rows.mapValues { (_, bytes) ->
        HerbertFileIdentity(bytes.size.toLong(), HerbertBundleImport.hex(
            java.security.MessageDigest.getInstance("SHA-256").digest(bytes)))
    }

    private fun withSnapshot(action: (File) -> Unit) {
        val parent = Files.createTempDirectory("herbert-zip-test").toFile()
        try { action(parent) } finally { parent.deleteRecursively() }
    }

    @Test fun githubStoredDescriptorsReadAndVerifiedWithoutModelSubstitution() {
        val rows = linkedMapOf("NOTICE.txt" to "fixture notice".toByteArray(),
            "manifest.json" to "fixture metadata".toByteArray())
        val bytes = actionsArchive(rows.toList())
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            assertThrows(ZipException::class.java) { zip.nextEntry }
        }
        withSnapshot { directory ->
            HerbertZipArchive.extract(ByteArrayInputStream(bytes), directory, identities(rows))
            for ((name, expected) in rows) assertArrayEquals(expected, File(directory, name).readBytes())
            assertFalse(File(directory, ".import.zip").exists())
        }
    }

    @Test fun deflatedDescriptorsStillReadAndVerified() {
        val rows = mapOf("NOTICE.txt" to "fixture".toByteArray())
        withSnapshot { directory ->
            HerbertZipArchive.extract(ByteArrayInputStream(archive("NOTICE.txt", "fixture")),
                directory, identities(rows))
            assertArrayEquals(rows.getValue("NOTICE.txt"), File(directory, "NOTICE.txt").readBytes())
            assertFalse(File(directory, ".import.zip").exists())
        }
    }

    @Test fun storedDescriptorsStillRejectWrongHashesDuplicatesAndUnsafeNames() {
        val rows = linkedMapOf("NOTICE.txt" to "fixture".toByteArray(),
            "manifest.json" to "fixture".toByteArray())
        val fixtures = listOf(
            actionsArchive(listOf("NOTICE.txt" to "changed".toByteArray(), "manifest.json" to rows.getValue("manifest.json"))) to HerbertImportReason.IDENTITY,
            actionsArchive(listOf("NOTICE.txt" to rows.getValue("NOTICE.txt"), "NOTICE.txt" to rows.getValue("NOTICE.txt"))) to HerbertImportReason.CONTENTS,
            actionsArchive(listOf("../NOTICE.txt" to rows.getValue("NOTICE.txt"))) to HerbertImportReason.CONTENTS,
            actionsArchive(listOf("NOTICE.txt" to rows.getValue("NOTICE.txt"))) to HerbertImportReason.CONTENTS,
        )
        for ((bytes, reason) in fixtures) withSnapshot { directory ->
            val failure = assertThrows(HerbertImportFailure::class.java) {
                HerbertZipArchive.extract(ByteArrayInputStream(bytes), directory, identities(rows))
            }
            assertEquals(reason, failure.reason)
            assertFalse(File(directory, ".import.zip").exists())
        }
    }

    @Test fun sameSizeTamperingRejectedAfterHashAndTemporaryContainerRemoved() {
        val rows = mapOf("NOTICE.txt" to "fixture".toByteArray())
        withSnapshot { directory ->
            val failure = assertThrows(HerbertImportFailure::class.java) {
                HerbertZipArchive.extract(ByteArrayInputStream(actionsArchive(
                    listOf("NOTICE.txt" to "changed".toByteArray()))), directory, identities(rows))
            }
            assertEquals(HerbertImportReason.IDENTITY, failure.reason)
            assertFalse(File(directory, ".import.zip").exists())
        }
    }

    @Test fun incompleteOrExcessiveCentralDirectoryAndContainerRejectedBeforeExtraction() {
        val rows = mapOf("NOTICE.txt" to "fixture".toByteArray())
        val valid = actionsArchive(rows.toList())
        val badCount = valid.copyOf().apply { this[size - 14] = 127; this[size - 12] = 127 }
        for (bytes in listOf(valid.copyOf(valid.size - 4), badCount, ByteArray(1024 * 1024 + 8))) {
            withSnapshot { directory ->
                val failure = assertThrows(HerbertImportFailure::class.java) {
                    HerbertZipArchive.extract(ByteArrayInputStream(bytes), directory, identities(rows))
                }
                assertEquals(HerbertImportReason.ZIP, failure.reason)
                assertEquals(emptyList<File>(), directory.listFiles()!!.toList())
            }
        }
    }

    @Test fun cancellingDuringCopyRemovesEntireUniqueStagingSnapshot() {
        withSnapshot { parent ->
            var checks = 0
            assertThrows(CancellationException::class.java) {
                HerbertBundleImport.stage(ByteArrayInputStream(archive("NOTICE.txt", "fixture")),
                    parent, trust()) { ++checks >= 3 }
            }
            assertEquals(emptyList<File>(), parent.listFiles()!!.toList())
        }
    }

    @Test fun storageBoundIncludesContainerExtractionAndReserve() {
        val required = HerbertZipArchive.requiredFreeBytes(HerbertBenchmarkTrial.trust().files)
        assertTrue(required > 2 * HerbertBundleTrust.MODEL_BYTES)
        assertTrue(required < 1_400_000_000L)
    }
}
