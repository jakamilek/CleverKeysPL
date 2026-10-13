package tribixbite.cleverkeys

import java.util.Locale

/** Literal completions of explicit personal entries, separate from prose prediction. */
class PersonalDictionaryCompletion(words: Collection<String>) {
    data class Token(val prefix: String, val suffix: String)

    private val buckets: Map<String, List<String>> = words.asSequence()
        .filter { PersonalDictionaryToken.read(it, "", null)?.word == it }
        .distinct().sortedWith(compareBy<String> { it.length }.thenBy { it })
        .flatMap { word -> (1..minOf(3, word.length)).asSequence().map {
            word.take(it).lowercase(Locale.ROOT) to word
        } }.groupBy({ it.first }, { it.second })

    fun matches(prefix: String): List<String> {
        if (prefix.isEmpty() || prefix.length > PersonalDictionaryToken.MAX_LENGTH) return emptyList()
        val folded = prefix.lowercase(Locale.ROOT)
        return buckets[prefix.take(3).lowercase(Locale.ROOT)].orEmpty().asSequence()
            .filter { it.lowercase(Locale.ROOT).startsWith(folded) }
            .toList()
    }

    companion object {
        /** Read the whole token even across @, dots, digits or hyphens; never cross whitespace. */
        fun read(before: String?, after: String?, selected: String?): Token? {
            if (before == null || after == null || !selected.isNullOrEmpty()) return null
            val prefix = before.takeLastWhile { !it.isWhitespace() }
            val suffix = after.takeWhile { !it.isWhitespace() }
            val word = prefix + suffix
            if (prefix.isEmpty() || word.length > PersonalDictionaryToken.MAX_LENGTH ||
                !prefix.first().isLetterOrDigit() || word.any { it.isISOControl() } ||
                (prefix.length == before.length && before.length > PersonalDictionaryToken.MAX_LENGTH) ||
                (suffix.length == after.length && after.length > PersonalDictionaryToken.MAX_LENGTH)) return null
            return Token(prefix, suffix)
        }
    }
}
