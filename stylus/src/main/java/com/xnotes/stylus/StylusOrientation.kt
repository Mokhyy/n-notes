package com.xnotes.stylus

/**
 * What a pen's orientation means for ink.
 *
 * `MotionEvent.AXIS_ORIENTATION` is the angle of the pen around its own axis in radians: which way
 * a chisel or flat nib faces. A full turn is [TWO_PI], so storage uses a 0..1 fraction of the turn.
 *
 * The renderer sweeps a round disc, so this cannot make a truly elliptical nib. Instead it applies
 * the chisel rule: the ink is widest when the nib edge crosses the direction of travel and thinnest
 * when it runs along it. That is the visible part of a rotated flat nib, and it needs no renderer
 * change beyond the half-width it already draws.
 */
object StylusOrientation {

    private val TWO_PI = Math.PI * 2.0

    /** Never let a nib turned fully edge-on erase the stroke: a hairline stays visible. */
    private const val MIN_FACTOR = 0.08

    /** A raw orientation angle to a clockwise 0..1 turn; negative and past-full readings wrap. */
    fun normalized(radians: Double): Double = ((radians % TWO_PI) + TWO_PI) % TWO_PI / TWO_PI

    fun normalized(radians: Float): Double = normalized(radians.toDouble())

    /**
     * Chisel width factor for a nib turned to [turn] (`0..1`) travelling along [headingRadians].
     *
     * The nib edge is a line at `turn · 2π`. Its projection across the direction of travel is
     * `|sin(nib - heading)|`: 1 when the edge crosses the path (broad), 0 when it lies along it
     * (edge-on). [strength] in `0..1` blends that range around 1, so 0 is an ordinary round nib and
     * 1 spans a hairline to twice the base width. The factor never goes below [MIN_FACTOR].
     */
    fun widthFactor(turn: Double, headingRadians: Double, strength: Double): Double {
        if (strength <= 0.0) return 1.0
        val nib = (((turn % 1.0) + 1.0) % 1.0) * TWO_PI
        val across = kotlin.math.abs(kotlin.math.sin(nib - headingRadians))
        return (1.0 + strength.coerceIn(0.0, 1.0) * (2.0 * across - 1.0)).coerceAtLeast(MIN_FACTOR)
    }

    /**
     * Per-sample chisel factors for a stroke saved without its orientation channel. Headings come
     * from a centred chord, which is what [com.xnotes.core.stroke.StrokeEngine] uses for tangents,
     * so a baked stroke looks like the one that was drawn.
     */
    fun bakedWidthFactors(xs: DoubleArray, ys: DoubleArray, turns: DoubleArray, strength: Double): DoubleArray {
        val n = minOf(xs.size, ys.size, turns.size)
        if (strength <= 0.0 || n == 0) return DoubleArray(n) { 1.0 }
        val out = DoubleArray(n)
        for (i in 0 until n) {
            val j0 = if (i > 0) i - 1 else 0
            val j1 = if (i + 1 < n) i + 1 else n - 1
            val dx = xs[j1] - xs[j0]
            val dy = ys[j1] - ys[j0]
            val heading = if (dx == 0.0 && dy == 0.0) 0.0 else kotlin.math.atan2(dy, dx)
            out[i] = widthFactor(turns[i], heading, strength)
        }
        return out
    }
}
