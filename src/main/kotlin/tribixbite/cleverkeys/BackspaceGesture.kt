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

    /** Percentages scale the v8 rate. Fast mode is based on travel, not absolute finger position. */
    fun repeatDelay(x: Float, width: Float, direction: Int, travel: Float,
        screenWidth: Float, options: EditBehaviorOptions): Long {
        val fast = screenWidth.isFinite() && screenWidth > 0f && travel.isFinite() &&
            travel > screenWidth * options.accelPercent.coerceIn(EditBehaviorRanges.BACKSPACE_ACCEL_PERCENT) / 100f
        val percent = if (fast) options.fastPercent.coerceIn(EditBehaviorRanges.BACKSPACE_FAST_PERCENT)
            else options.speedPercent.coerceIn(EditBehaviorRanges.BACKSPACE_SPEED_PERCENT)
        return (repeatDelay(x, width, direction) * 100f / percent).toLong().coerceAtLeast(10L)
    }

    /**
     * A small reversal brakes the current repeat; a separate deliberate move resumes.
     * References follow the active extreme, so touch jitter cannot drift the brake point.
     */
    class Drag(
        private val originX: Float,
        private val threshold: Float = 15f,
        private val pauseThreshold: Float = 3f,
        private val pauseEnabled: Boolean = true
    ) {
        var direction = 0
            private set
        private var activated = false
        private var travelOrigin = originX
        private var lastDirection = -1

        fun travel(x: Float): Float = if (direction == 0) 0f
            else ((x - travelOrigin) * direction).coerceAtLeast(0f)
        private var motionReference = originX
        private var extremeX = originX

        fun move(x: Float): Boolean {
            if (!x.isFinite()) return false
            if (direction == 0) {
                val delta = x - motionReference
                // Initially only a left drag extends the word preview. After braking,
                // either direction can resume, relative to the new pause position.
                if (!activated && delta > -threshold) return false
                if (kotlin.math.abs(delta) < threshold) return false
                direction = if (delta < 0f) -1 else 1
                if (activated && direction != lastDirection) travelOrigin = motionReference
                lastDirection = direction
                activated = true
                extremeX = x
                return true
            }
            val advance = (x - extremeX) * direction
            if (advance <= -pauseThreshold) {
                motionReference = x
                if (pauseEnabled) direction = 0
                else {
                    direction = -direction
                    lastDirection = direction
                    travelOrigin = extremeX
                    extremeX = x
                }
                return true
            }
            if (advance > 0f) extremeX = x
            return false
        }
    }
}
