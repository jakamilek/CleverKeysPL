package tribixbite.cleverkeys.ai

import java.util.Locale

/** Independent live-editor context for SI; does not mutate the two-word n-gram tracker. */
internal object HerbertContextWindow {
    const val MAX_UNITS = 4096
    const val MAX_WORDS = 64
    const val DEFAULT_WORDS = 32

    private fun space(cp: Int) = Character.isWhitespace(cp) || Character.isSpaceChar(cp) || cp == 0x85

    /** Diagnostic count using the same boundaries as retain; no editor access. */
    fun countWords(text: String): Int {
        var count = 0
        var inWord = false
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val isSpace = space(cp)
            if (!isSpace && !inWord) count++
            inWord = !isSpace
            i += Character.charCount(cp)
        }
        return count
    }

    /** The caller reads BEFORE inserting a swipe, never from a historical lowercase word list. */
    fun retain(beforeCursor: String, maxWords: Int = DEFAULT_WORDS): String {
        require(maxWords in 1..MAX_WORDS)
        var start = (beforeCursor.length - MAX_UNITS).coerceAtLeast(0)
        if (start < beforeCursor.length && Character.isLowSurrogate(beforeCursor[start])) start++
        // A bounded read may start inside a word; do not pass that invented fragment to the model.
        if (start > 0 && start < beforeCursor.length &&
            !space(beforeCursor.codePointBefore(start)) && !space(beforeCursor.codePointAt(start))) {
            while (start < beforeCursor.length && !space(beforeCursor.codePointAt(start))) {
                start += Character.charCount(beforeCursor.codePointAt(start))
            }
        }
        val starts = ArrayList<Int>()
        var i = start
        var inWord = false
        while (i < beforeCursor.length) {
            val cp = beforeCursor.codePointAt(i)
            val isSpace = space(cp)
            if (!isSpace && !inWord) starts.add(i)
            inWord = !isSpace
            i += Character.charCount(cp)
        }
        if (starts.size > maxWords) start = starts[starts.size - maxWords]
        return beforeCursor.substring(start)
    }
}

/** Captured editor state. Every component must be revalidated before adopting a result. */
internal data class HerbertRequestIdentity(
    val editorSession: Long,
    val textRevision: Long,
    val selectionStart: Int,
    val selectionEnd: Int,
    val requestId: Long,
    val language: String,
    val packIdentity: String,
    val settingsRevision: Long,
    val context: String,
)

/** One lexical key with source-confirmed forms; decoder scores never enter the MLM model. */
internal class HerbertCasePair(key: String, surfaces: List<String>) {
    val key: String = key
    val surfaces: List<String> = java.util.Collections.unmodifiableList(ArrayList(surfaces))

    init {
        require(key.isNotBlank() && key.length <= 96 && key == key.lowercase(Locale.ROOT))
        require(this.surfaces.size == 2 && this.surfaces.distinct().size == 2)
        require(this.surfaces.all { it.lowercase(Locale.ROOT) == key })
        require(key in this.surfaces && key.replaceFirstChar { it.titlecase(Locale.ROOT) } in this.surfaces)
    }

    /** Safe alternative order only. Null means keep the existing geometric/source order. */
    fun ordered(
        scores: Map<String, Float>,
        captured: HerbertRequestIdentity,
        current: HerbertRequestIdentity,
        nowNanos: Long,
        deadlineNanos: Long,
        explicitOrSentenceCapitalization: Boolean,
        ordinaryPolishField: Boolean,
    ): List<String>? {
        if (!ordinaryPolishField || explicitOrSentenceCapitalization || captured != current ||
            captured.language != "pl" || captured.selectionStart != captured.selectionEnd ||
            nowNanos >= deadlineNanos || captured.context.isBlank()) return null
        if (scores.keys != surfaces.toSet() || scores.values.any { !it.isFinite() }) return null
        // Stable ties preserve source order; no hardcoded name/common-word preferences.
        return surfaces.sortedWith(compareByDescending<String> { scores.getValue(it) })
    }
}
