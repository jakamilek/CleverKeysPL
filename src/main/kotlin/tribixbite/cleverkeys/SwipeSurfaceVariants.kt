package tribixbite.cleverkeys

import tribixbite.cleverkeys.langpack.LanguageIntelligenceProvider
import java.util.Locale

/** Presentation only: each source surface keeps its own decoder key's score/language. */
internal object SwipeSurfaceVariants {
    const val MAX_GROUP_SURFACES = 4
    const val FAMILY_SEARCH_CANDIDATES = 5

    /** Keyboard-equivalent Polish letters, not a claim that the words share a lemma. */
    fun foldPolish(surface: String): String = surface.lowercase(Locale.ROOT).map {
        when (it) {
            'ą' -> 'a'; 'ć' -> 'c'; 'ę' -> 'e'; 'ł' -> 'l'; 'ń' -> 'n'
            'ó' -> 'o'; 'ś' -> 's'; 'ź', 'ż' -> 'z'; else -> it
        }
    }.joinToString("")
    data class Slate(
        val words: List<String>, val scores: List<Int>, val languages: List<String>?,
        val exactCase: List<Boolean>,
        // Model membership is independent of a surface's capitalization policy.
        val formGroupSize: Int = 0,
    )

    fun expand(
        words: List<String>, scores: List<Int>, languages: List<String>?,
        provider: LanguageIntelligenceProvider?, capitalize: Boolean, capsLock: Boolean,
        showCaseVariants: Boolean = true,
    ): Slate {
        fun unchanged() = Slate(words, scores, languages, List(words.size) { false })
        if (words.isEmpty() || provider == null || capsLock ||
            (languages != null && languages.size != words.size)) return unchanged()
        val primaryKey = words.first().lowercase(Locale.ROOT)
        data class Forms(val surfaces: List<String>, val exact: Boolean)
        fun forms(word: String): Forms {
            val key = word.lowercase(Locale.ROOT)
            val caps = provider.lookup(word)?.capitalization?.takeIf {
                showCaseVariants && "capitalization" in provider.capabilities() && it.variants.isNotEmpty()
            } ?: return Forms(listOf(word), false)
            val source = caps.variants.map { it.surface }
            val preferred = when {
                capitalize -> source.firstOrNull { it.firstOrNull()?.isUpperCase() == true } ?: word
                word in source && word != key -> word
                else -> caps.defaultSurface ?: source.first()
            }
            return Forms(if (source.size == 1) listOf(preferred) else (listOf(preferred) + source).distinct(), true)
        }
        val first = forms(words.first())
        val outWords = first.surfaces.toMutableList()
        val outScores = MutableList(outWords.size) { scores.firstOrNull() ?: 0 }
        val outLanguages = languages?.let { source -> MutableList(outWords.size) { source.first() } }
        val exact = MutableList(outWords.size) { first.exact }
        val groupKeys = mutableSetOf(primaryKey)
        // At most one additional decoded key: keyboard-equivalent spelling OR a source lemma.
        // The model does not need capitalization metadata to compare ordinary word forms.
        if (provider.packageInfo().languageCode == "pl" && scores.size == words.size && outWords.size <= 2) {
            for (i in 1 until minOf(words.size, FAMILY_SEARCH_CANDIDATES)) {
                val key = words[i].lowercase(Locale.ROOT)
                if (key in groupKeys || (languages != null && (languages[0] != "pl" || languages[i] != "pl"))) continue
                if (foldPolish(key) != foldPolish(primaryKey) && !provider.sharesSourceLemma(primaryKey, key)) continue
                val other = forms(words[i])
                if (outWords.size + other.surfaces.size > MAX_GROUP_SURFACES) continue
                outWords.addAll(other.surfaces)
                outScores.addAll(List(other.surfaces.size) { scores[i] })
                outLanguages?.addAll(List(other.surfaces.size) { languages!![i] })
                exact.addAll(List(other.surfaces.size) { other.exact })
                groupKeys.add(key)
                break
            }
        }
        val groupSize = outWords.size
        for (i in 1 until words.size) {
            // Preserve independently decoded case choices if no source expansion replaced them.
            val key = words[i].lowercase(Locale.ROOT)
            val sameLanguage = languages == null || languages[i] == languages[0]
            if (key in groupKeys && sameLanguage &&
                (words[i] in outWords || (key == primaryKey && first.exact) ||
                    (key != primaryKey && provider.lookup(words[i])?.capitalization != null && showCaseVariants))) continue
            outWords.add(words[i])
            outScores.add(scores.getOrElse(i) { 0 })
            outLanguages?.add(languages!![i])
            exact.add(false)
        }
        return Slate(outWords, outScores, outLanguages, exact, groupSize)
    }
}
