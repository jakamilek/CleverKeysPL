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
        val groupKeys = mutableSetOf(key)
        // One additional, already decoded near key; never generate an inflection or look up
        // the whole folded dictionary. This presentation also works with SI switched off.
        if (provider.packageInfo().languageCode == "pl" && scores.size == words.size &&
            (languages == null || languages.size == words.size) && surfaces.size <= 2) {
            for (i in 1 until minOf(words.size, FAMILY_SEARCH_CANDIDATES)) {
                val otherKey = words[i].lowercase(Locale.ROOT)
                if (otherKey in groupKeys || foldPolish(otherKey) != foldPolish(key) ||
                    (languages != null && (languages[0] != "pl" || languages[i] != "pl"))) continue
                val other = provider.lookup(words[i])?.capitalization ?: continue
                val otherForms = other.variants.map { it.surface }
                if (otherForms.isEmpty() || otherForms.size > 2 ||
                    outWords.size + otherForms.size > MAX_GROUP_SURFACES) continue
                val first = when {
                    capitalize -> otherForms.firstOrNull { it.firstOrNull()?.isUpperCase() == true } ?: otherForms.first()
                    words[i] in otherForms && words[i] != otherKey -> words[i]
                    else -> other.defaultSurface ?: otherForms.first()
                }
                val expanded = (listOf(first) + otherForms).distinct()
                outWords.addAll(expanded)
                outScores.addAll(List(expanded.size) { scores[i] })
                outLanguages?.addAll(List(expanded.size) { languages!![i] })
                exact.addAll(List(expanded.size) { true })
                groupKeys.add(otherKey)
                break
            }
        }
        for (i in 1 until words.size) {
            // Do not suppress a same-spelled candidate from a different language.
            if (words[i].lowercase(Locale.ROOT) in groupKeys &&
                (languages == null || languages[i] == languages[0])) continue
            outWords.add(words[i])
            outScores.add(scores.getOrElse(i) { 0 })
            outLanguages?.add(languages!![i])
            exact.add(false)
        }
        return Slate(outWords, outScores, outLanguages, exact)
    }
}
