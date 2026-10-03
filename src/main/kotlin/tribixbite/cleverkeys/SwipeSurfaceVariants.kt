package tribixbite.cleverkeys

import tribixbite.cleverkeys.langpack.LanguageIntelligenceProvider
import java.util.Locale

/** Presentation only: two surfaces share one decoder score and one lexical identity. */
internal object SwipeSurfaceVariants {
    data class Slate(
        val words: List<String>, val scores: List<Int>, val languages: List<String>?,
        val exactCase: List<Boolean>,
    )

    fun expand(
        words: List<String>, scores: List<Int>, languages: List<String>?,
        provider: LanguageIntelligenceProvider?, capitalize: Boolean, capsLock: Boolean,
    ): Slate {
        fun unchanged() = Slate(words, scores, languages, List(words.size) { false })
        if (words.isEmpty() || provider == null || capsLock || "capitalization" !in provider.capabilities()) return unchanged()
        val primary = words.first()
        val key = primary.lowercase(Locale.ROOT)
        val caps = provider.lookup(primary)?.capitalization ?: return unchanged()
        if (caps.variants.isEmpty()) return unchanged()
        val forms = caps.variants.map { it.surface }
        val preferred = when {
            capitalize -> forms.firstOrNull { it.firstOrNull()?.isUpperCase() == true }
                ?: primary // sentence capitalization also applies to single lowercase nouns
            primary in forms && primary != key -> primary // existing user case preference
            else -> caps.defaultSurface ?: forms.first()
        }
        val surfaces = if (forms.size == 1) listOf(preferred) else (listOf(preferred) + forms).distinct()
        val outWords = surfaces.toMutableList()
        val outScores = MutableList(surfaces.size) { scores.firstOrNull() ?: 0 }
        val outLanguages = languages?.let { source -> MutableList(surfaces.size) { source.first() } }
        val exact = MutableList(surfaces.size) { true }
        for (i in 1 until words.size) {
            // Do not suppress a same-spelled candidate from a different language.
            if (words[i].lowercase(Locale.ROOT) == key &&
                (languages == null || languages[i] == languages[0])) continue
            outWords.add(words[i])
            outScores.add(scores.getOrElse(i) { 0 })
            outLanguages?.add(languages!![i])
            exact.add(false)
        }
        return Slate(outWords, outScores, outLanguages, exact)
    }
}
