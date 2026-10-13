package tribixbite.cleverkeys.langpack

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

/** Isolated small-heap gate: previous snapshot + a v7-sized replacement stream + live-data ballast.
 * With two arguments it checks the actual previous/replacement packs; CI generates a representative
 * source-reading workload on disk, without bundling dictionary files into the APK.
 */
object LanguageIntelligenceMemoryCheck {
    private fun fixture(file: File, count: Int) {
        file.bufferedWriter().use { out ->
            out.write("{\"entries\":[")
            for (i in 0 until count) {
                if (i > 0) out.write(",")
                val key = "word$i"
                // Fit 98K entries under the unchanged whole-document 2M-node limit.
                val old = i < 10_000
                out.write("{\"surfaceKey\":\"$key\",")
                if (old) out.write("\"canonicalForm\":\"$key\",\"capitalization\":{\"defaultSurface\":\"$key\",\"variants\":[" +
                    "{\"surface\":\"$key\",\"casePolicy\":\"lowercase\"}," +
                    "{\"surface\":\"Word$i\",\"casePolicy\":\"capitalized\"}]},")
                out.write("\"metadata\":{\"sourceEvidence\":{\"lexicalReadings\":[")
                for (r in 0 until if (old) 4 else 2) {
                    if (r > 0) out.write(",")
                    out.write("{\"lemma\":\"lemma$r\",\"partOfSpeech\":\"subst\",\"surfaces\":[\"$key\"]")
                    if (old) out.write(",\"nameClasses\":[\"nazwa_pospolita\"],\"labels\":[]")
                    out.write("}")
                }
                out.write("]},\"note\":\"source metadata padding\"}}")
            }
            out.write("],\"languageCode\":\"pl\",\"schemaVersion\":1}")
        }
    }

    private fun pack(path: String, version: Int, previous: LanguageIntelligenceProvider? = null): LanguageIntelligenceProvider = ZipFile(path).use { zip ->
        zip.getInputStream(zip.getEntry(IntelligenceJson.FILE)).reader(Charsets.UTF_8).use {
            IntelligenceJson.parse(it, "pl", version, setOf("capitalization", "metadata"), previous)
        }
    }

    @JvmStatic fun main(args: Array<String>) {
        check(Runtime.getRuntime().maxMemory() <= 192L * 1024 * 1024) {
            "Run this gate in an isolated JVM with -Xmx192m or less"
        }
        val ballast = ByteArray(16 * 1024 * 1024)
        val directory = Files.createTempDirectory("intelligence-memory").toFile()
        try {
            val old: LanguageIntelligenceProvider
            val expanded: LanguageIntelligenceProvider
            if (args.size == 2) {
                old = pack(args[0], 6)
                expanded = pack(args[1], 7, old)
                for (key in listOf("łódź", "malina", "maliną")) check(old.lookup(key) === expanded.lookup(key))
                for (key in listOf("dodam", "grzeje", "kasami", "nawilżane", "odpowiadam",
                    "patrzysz", "poczekaj", "podpowie", "pozdrawiam")) {
                    check(expanded.lookup(key)?.sourceLemmas?.isNotEmpty() == true)
                }
            } else {
                check(args.isEmpty())
                val before = File(directory, "before.json")
                val after = File(directory, "after.json")
                fixture(before, 91_527); fixture(after, 98_138)
                check(after.length() <= IntelligenceJson.MAX_BYTES)
                old = before.reader().use { IntelligenceJson.parse(it, "pl", 6, setOf("metadata", "capitalization")) }
                expanded = after.reader().use { IntelligenceJson.parse(it, "pl", 7, setOf("metadata", "capitalization"), old) }
                check(old.lookup("word0") === expanded.lookup("word0"))
                check(old.lookup("word91526") === expanded.lookup("word91526"))
                check(expanded.lookup("word98137") != null)
                check(expanded.sharesSourceLemma("word16199", "word91526"))
                println("Representative sidecar bytes: ${after.length()}")
            }
            check(ballast.first() == 0.toByte() && ballast.last() == 0.toByte())
            check(old.lookup(if (args.isEmpty()) "word0" else "łódź") != null)
            println("Language intelligence streaming memory: previous + expanded + 16MiB ballast, 192MiB heap PASS")
        } finally { directory.deleteRecursively() }
    }
}
