package tribixbite.cleverkeys.langpack

import java.util.zip.ZipFile
import tribixbite.cleverkeys.SwipeSurfaceVariants
import tribixbite.cleverkeys.swipe.geometric.CkdtDictionaryReader

/** Actual external v8/v9 gates. Host heap evidence is not Android PSS/phone timing. */
object SharedMetadataPackCheck {
    private fun pack(path: String, previous: LanguageIntelligenceProvider? = null): LanguageIntelligenceProvider =
        ZipFile(path).use { z ->
            val manifest = z.getInputStream(z.getEntry("manifest.json")).reader().use(IntelligenceJson::document)
            val member = IntelligenceJson.declaration(manifest)!!
            z.getInputStream(z.getEntry(member.file)).reader().use {
                IntelligenceJson.parse(it, "pl", IntelligenceJson.integer(manifest,"version"),
                    IntelligenceJson.capabilities(manifest), previous, member.schemaVersion)
            }
        }

    @JvmStatic fun main(args: Array<String>) {
        check(args.size == 3)
        val mode = args[0]
        if (mode != "full") check(Runtime.getRuntime().maxMemory() <= 192L*1024*1024)
        val ballast = if (mode != "full") ByteArray(16*1024*1024) else null
        val old = when (mode) { "cold" -> null; "repeat" -> pack(args[2]); else -> pack(args[1]) }
        val new = pack(args[2], old)
        check(new.packageInfo().version == 9)
        for (key in listOf("łódź","malina","maliną","grzeje","poczekaj","twitter","twittera","usb","jana")) {
            check(new.lookup(key)?.sourceLemmas?.isNotEmpty() == true)
            old?.lookup(key)?.let { check(it == new.lookup(key)) }
        }
        if (mode == "repeat") {
            check(old!!.sharedSources === new.sharedSources)
            check(old.lookup("malina") === new.lookup("malina"))
        }
        if (mode == "full") {
            val words = ZipFile(args[1]).use { z ->
                CkdtDictionaryReader.readEntries(z.getInputStream(z.getEntry("dictionary.bin")).readBytes())
            }
            check(words.size == 190813)
            var metadata = 0
            for (word in words) {
                val before = old!!.lookup(word.word)
                val after = new.lookup(word.word)
                check(before == after) { "Changed metadata at ${word.word}" }
                if (before != null) metadata++
            }
            check(metadata == 100649)
            for (choices in listOf(listOf("malina","maliną","mamoną"), listOf("łódź","łodzi","lód"),
                listOf("praca","pracą","pracy"), listOf("Twitter","Twittera","Instagram"))) {
                val scores = listOf(100,80,60)
                for (capitalize in listOf(false,true)) check(
                    SwipeSurfaceVariants.expand(choices,scores,listOf("pl","pl","pl"),old,capitalize,false) ==
                    SwipeSurfaceVariants.expand(choices,scores,listOf("pl","pl","pl"),new,capitalize,false))
            }
            println("Real v8/v9 PASS:190813 words;100649 exact metadata views/source identities;case/form presentation unchanged")
        } else {
            check(ballast!!.first() == 0.toByte() && ballast.last() == 0.toByte())
            println("Real shared metadata $mode +16MiB ballast at192MiB PASS")
        }
    }
}
