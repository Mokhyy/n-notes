package com.xnotes.stylus

/**
 * What a pen's tilt means for ink width.
 *
 * `MotionEvent.AXIS_TILT` is the nib's angle off perpendicular in radians: 0 upright, about
 * [HALF_PI] laid flat. Everything here is deliberately independent of pressure — pressure shapes
 * the base width, tilt only scales it — so a pen can thin from pressure and broaden from laying it
 * down at the same time, or either one on its own.
 *
 * Kept in this module so the app only passes the axis reading and the tool's strength; the meaning
 * of tilt and the curve stay with the rest of the stylus behaviour.
 */
object StylusTilt {

    private val HALF_PI = Math.PI / 2.0

    /**
     * Normalize a raw tilt reading to `0` upright and `1` flat, clamped, so a pen that reports past
     * perpendicular (or a negative axis) never widens beyond the configured maximum.
     */
    fun normalized(tiltRadians: Double): Double = (tiltRadians / HALF_PI).coerceIn(0.0, 1.0)

    fun normalized(tiltRadians: Float): Double = normalized(tiltRadians.toDouble())

    /**
     * Extra width at [normalizedTilt] with [strength] as the increase at full lay: `1` upright,
     * `1 + strength` flat. Strength is clamped to `[0, 1]`, so the most a pen can add is its own
     * width again. Independent of pressure, which the renderer applies separately.
     */
    fun widthFactor(normalizedTilt: Double, strength: Double): Double =
        1.0 + normalizedTilt.coerceIn(0.0, 1.0) * strength.coerceIn(0.0, 1.0)

    /**
     * The width a sample should keep when its tilt is baked away instead of stored: [base] is the
     * width factor it already had (the speed pen's, or 1), multiplied by this sample's tilt factor.
     * Saving the result in the width channel gives the same ink without an angle channel.
     */
    fun bakedWidthFactor(normalizedTilt: Double, strength: Double, base: Double = 1.0): Double =
        base * widthFactor(normalizedTilt, strength)
}
