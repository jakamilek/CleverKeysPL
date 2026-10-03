package tribixbite.cleverkeys

/** Session-only eligibility. Editor mutations must not turn an ordinary Shift into an edit. */
internal class CursorWordCapitalization {
    var eligiblePosition: Int? = null
        private set
    private var expectedPosition: Int? = null
    private var awaitingUnknownMutation = false
    private var awaitingMutationAcknowledgement = false
    private var repeatEdit = false
    private var knownCaseMutation = false

    fun disarm() {
        eligiblePosition = null
        repeatEdit = false
    }

    fun reset() {
        eligiblePosition = null
        expectedPosition = null
        awaitingUnknownMutation = false
        awaitingMutationAcknowledgement = false
        repeatEdit = false
        knownCaseMutation = false
    }

    fun mutation(position: Int, retainEdit: Boolean = false) {
        eligiblePosition = if (retainEdit && position >= 0) position else null
        expectedPosition = position.takeIf { it >= 0 }
        awaitingUnknownMutation = position < 0
        awaitingMutationAcknowledgement = true
        repeatEdit = retainEdit
        knownCaseMutation = retainEdit && position >= 0
    }

    fun selection(oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int) {
        if (newStart < 0 || newStart != newEnd) {
            eligiblePosition = null
            return
        }
        if (newStart == expectedPosition) {
            awaitingMutationAcknowledgement = false
            eligiblePosition = if (repeatEdit) newStart else null
            return
        }
        if (awaitingUnknownMutation) {
            awaitingUnknownMutation = false
            awaitingMutationAcknowledgement = false
            eligiblePosition = null
            return
        }
        // Some editors expose the pre-commit cursor during the immediate read.
        // Its first movement acknowledgement must still leave ordinary Shift alone.
        // A case edit restores a known caret in a batch. Editors can omit its unchanged
        // final-selection callback; the next USER move must not be swallowed as an ack.
        if (awaitingMutationAcknowledgement && !knownCaseMutation &&
            oldStart == expectedPosition && oldStart == oldEnd) {
            awaitingMutationAcknowledgement = false
            expectedPosition = null
            eligiblePosition = null
            return
        }
        if (oldStart != newStart || oldEnd != newEnd) {
            expectedPosition = null
            awaitingMutationAcknowledgement = false
            repeatEdit = false
            knownCaseMutation = false
            eligiblePosition = newStart
        }
    }

    data class Edit(val start: Int, val original: String, val replacement: String, val cursor: Int)

    companion object {
        const val CONTEXT_LIMIT = 128
        private val WORD = Regex("[\\p{L}][\\p{L}\\p{M}]*(?:['’‑-][\\p{L}][\\p{L}\\p{M}]*)*")

        /** Changes one Unicode code point only; all other letters/marks retain their spelling. */
        fun plan(before: String, after: String, cursor: Int): Edit? {
            if (cursor < before.length || before.length > CONTEXT_LIMIT ||
                after.length > CONTEXT_LIMIT) return null
            val text = before + after
            val caret = before.length
            var start = caret
            var end = caret
            while (start > 0 && wordPart(Character.codePointBefore(text, start))) {
                start -= Character.charCount(Character.codePointBefore(text, start))
            }
            while (end < text.length && wordPart(Character.codePointAt(text, end))) {
                end += Character.charCount(Character.codePointAt(text, end))
            }
            if (start == end || (start == 0 && before.length >= CONTEXT_LIMIT) ||
                (end == text.length && after.length >= CONTEXT_LIMIT)) return null
            val word = text.substring(start, end)
            if (!WORD.matches(word)) return null
            // Do not edit identifiers, paths, emails or dotted names in a normal text field.
            if (technicalNeighbour(text, start - 1, -1) || technicalNeighbour(text, end, 1)) return null
            val first = Character.codePointAt(word, 0)
            val replacement = if (Character.isUpperCase(first) || Character.isTitleCase(first)) {
                Character.toLowerCase(first)
            } else {
                Character.toTitleCase(first)
            }
            if (first == replacement) return null
            val originalText = String(Character.toChars(first))
            val replacementText = String(Character.toChars(replacement))
            // Keep cursor offsets unambiguous; skip Unicode mappings changing UTF-16 width.
            if (originalText.length != replacementText.length) return null
            return Edit(cursor - caret + start, originalText, replacementText, cursor)
        }

        private fun wordPart(cp: Int): Boolean = Character.isLetter(cp) ||
            Character.getType(cp) in setOf(Character.NON_SPACING_MARK.toInt(),
                Character.COMBINING_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt()) ||
            cp == '\''.code || cp == '’'.code || cp == '-'.code || cp == '‑'.code

        private fun technicalNeighbour(text: String, index: Int, direction: Int): Boolean {
            val c = text.getOrNull(index) ?: return false
            return c.isLetterOrDigit() || c in "_@/\\:#" ||
                (c == '.' && text.getOrNull(index + direction)?.isLetterOrDigit() == true)
        }
    }
}
