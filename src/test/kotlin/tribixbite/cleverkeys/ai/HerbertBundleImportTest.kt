package tribixbite.cleverkeys.ai

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
}
