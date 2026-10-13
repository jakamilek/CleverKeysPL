package tribixbite.cleverkeys

import kotlin.math.abs
import kotlin.math.min

/** Space and Backspace share distance accumulation and finger-speed acceleration. */
internal class SliderMotion(
    x: Float,
    y: Float,
    stepPx: Float,
    smoothing: Float,
    maxSpeed: Float,
    private val directionX: Int = 1,
    private val directionY: Int = 0,
    private val verticalMultiplier: Float = 1f
) {
    private val step = stepPx.takeIf { it.isFinite() && it > 0f } ?: 30f
    private val blend = smoothing.takeIf { it.isFinite() }?.coerceIn(0.1f, 0.95f) ?: 0.6f
    private val maximum = maxSpeed.takeIf { it.isFinite() }?.coerceIn(1f, 10f) ?: 6f
    private var lastX = x
    private var lastY = y
    private var lastMs: Long? = null
    private var distance = 0f
    private var speed = 1f

    fun move(x: Float, y: Float, nowMs: Long): Int {
        if (!x.isFinite() || !y.isFinite()) return 0
        val dx = x - lastX
        val dy = y - lastY
        val travelled = abs(dx) + abs(dy)
        if (!travelled.isFinite() || travelled == 0f) return 0
        // Preserve the space slider's initial distance and fractional remainder.
        if (lastMs == null && travelled < step) return 0
        distance += (dx * speed * directionX + dy * speed * verticalMultiplier * directionY) / step
        if (!distance.isFinite()) { distance = 0f; return 0 }
        val elapsed = lastMs?.let { (nowMs - it).coerceAtLeast(1L) } ?: 1L
        val instant = min(maximum, travelled / elapsed + 1f)
        speed += (instant - speed) * blend
        lastMs = nowMs
        lastX = x
        lastY = y
        val whole = distance.coerceIn(-256f, 256f).toInt()
        distance %= 1f
        // Bound work for malformed coordinates; excess must not leak into a later stationary event.
        return whole
    }
}
