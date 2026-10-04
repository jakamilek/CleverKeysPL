package tribixbite.cleverkeys

/** Pure policy for the hold preview and keyboard-wide reversible drag. */
object BackspaceGesture {
    enum class Mode { WORD_PREVIEW, WORD_GAP, DRAG }
    const val WORD_PREVIEW_MS = 350L
    const val WORD_GAP_MS = 200L
    data class Span(val start: Int, val end: Int)

    fun previousWord(before: String, hasEarlierText: Boolean = false): Span? {
        var end = before.length
        while (end > 0 && before[end - 1] in " \t") end--
        if (end == 0 || before[end - 1].isWhitespace()) return null
        var start = end
        while (start > 0 && !before[start - 1].isWhitespace()) start--
        // The bounded editor read may have started in the middle of a long token.
        if (start == 0 && hasEarlierText) return null
        return Span(start, before.length)
    }

    /** Apply a signed character count without splitting Unicode surrogate pairs. */
    fun step(before: String, cursor: Int, direction: Int): Int {
        val at = cursor.coerceIn(0, before.length)
        return when {
            direction < 0 && at > 0 -> Character.offsetByCodePoints(before, at,
                direction.coerceAtLeast(-Character.codePointCount(before, 0, at)))
            direction > 0 && at < before.length -> Character.offsetByCodePoints(before, at,
                direction.coerceAtMost(Character.codePointCount(before, at, before.length)))
            else -> at
        }
    }

}
