package tribixbite.cleverkeys

/** Pure policy for the hold preview and keyboard-wide reversible drag. */
object BackspaceGesture {
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

    fun step(before: String, cursor: Int, direction: Int): Int {
        val at = cursor.coerceIn(0, before.length)
        return when {
            direction < 0 && at > 0 -> Character.offsetByCodePoints(before, at, -1)
            direction > 0 && at < before.length -> Character.offsetByCodePoints(before, at, 1)
            else -> at
        }
    }

    fun repeatDelay(x: Float, width: Float, direction: Int): Long {
        if (!x.isFinite() || !width.isFinite() || width <= 0f || direction == 0) return 200L
        val position = (x / width).coerceIn(0f, 1f)
        val edge = if (direction < 0) 1f - position else position
        return (200f - 170f * edge * edge).toLong().coerceIn(30L, 200L)
    }

    class Drag(private val originX: Float, private val threshold: Float = 15f) {
        var direction = 0
            private set
        private var motionReference = originX
        fun move(x: Float): Boolean {
            if (!x.isFinite()) return false
            val delta = x - motionReference
            if (direction == 0 && x - originX <= -threshold) {
                direction = -1
                motionReference = x
                return true
            }
            if (direction != 0 && kotlin.math.abs(delta) >= threshold) {
                val next = if (delta < 0f) -1 else 1
                motionReference = x
                val changed = next != direction
                direction = next
                return changed
            }
            return false
        }
    }
}
