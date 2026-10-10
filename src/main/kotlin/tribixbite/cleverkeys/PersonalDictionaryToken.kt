package tribixbite.cleverkeys

/** Explicit whole-entry add only; never widens autocorrect or implicit learning. */
object PersonalDictionaryToken {
    const val MAX_LENGTH = 128
    data class Snapshot(val word: String, val prefix: String, val suffix: String, val separator: String)

    fun read(before: String?, after: String?, selected: String?): Snapshot? {
        if (before == null || after == null || !selected.isNullOrEmpty()) return null
        val separator = if (before.endsWith(" ")) " " else ""
        val left = before.removeSuffix(separator)
        val prefix = left.takeLastWhile { !it.isWhitespace() }
        val suffix = if (separator.isEmpty()) after.takeWhile { !it.isWhitespace() } else ""
        // Both reads request MAX_LENGTH+2: reaching the read boundary without a
        // delimiter cannot prove a complete token. Never offer a truncated fragment.
        if ((left.length >= MAX_LENGTH + 1 && prefix.length == left.length) ||
            (after.length >= MAX_LENGTH + 1 && suffix.length == after.length)) return null
        val word = prefix + suffix
        if (word.length !in 3..MAX_LENGTH || prefix.isEmpty() ||
            !word.first().isLetterOrDigit() || !word.last().isLetterOrDigit() ||
            word.none { it.isLetter() } || word.any { it.isISOControl() || it.isWhitespace() } ||
            word.none { it in ".-@_/+:\\%#&=" }) return null
        return Snapshot(word, prefix, suffix, separator)
    }
}
