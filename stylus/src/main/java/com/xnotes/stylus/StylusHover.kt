package com.xnotes.stylus

import android.view.InputDevice
import android.view.MotionEvent
import kotlin.math.pow

/**
 * The hover-preview response: how a hovering pen's distance becomes cursor size and opacity.
 *
 * All of it lives here so the app only decides *whether* a pen shows the preview and whether its
 * profile uses distance; the curve itself, the per-device normalisation and the binary-axis guard
 * are shared by every surface and stay in the stylus module.
 */
object StylusHover {

    /** Fraction the hover ring shrinks by at the pen's maximum reported distance. */
    const val SHRINK_AT_MAX_DISTANCE = 0.65

    /** Fraction of the hover ring's alpha lost at the pen's maximum reported distance. */
    const val FADE_AT_MAX_DISTANCE = 0.85

    /**
     * Exponent of the distance response. Below 1 it lifts the low end of the range, so the small
     * distance changes a pen makes while hovering close move the ring much more than a linear
     * mapping would: the preview responds where the pen actually is, not only near its stated max.
     */
    const val DISTANCE_RESPONSE = 0.4

    /** Radius of the neutral pointer ring, in dp. */
    const val POINTER_RADIUS_DP = 5.0

    /** Floor for a stroke tool's hover ring, in dp. */
    const val MIN_STROKE_RADIUS_DP = 4.0

    /** Radius of the dot at the centre of a stroke tool's hover ring, in dp. */
    const val DOT_RADIUS_DP = 1.5

    private val maxByDevice = HashMap<Int, Double>()

    /**
     * [MotionEvent.AXIS_DISTANCE] as 0..1 of this device's maximum, cached per pen, then shaped by
     * [DISTANCE_RESPONSE] so a small lift already moves the cursor.
     *
     * Returns 0 when the axis is absent, and also when the device advertises only a binary
     * far/near range (max 1), as the Xiaomi Focus Pen Pro's Novatek panel does: with no gradient
     * to fade across, the cursor stays at full strength rather than sitting permanently "far".
     */
    @Synchronized
    fun distanceFraction(e: MotionEvent): Double {
        val raw = e.getAxisValue(MotionEvent.AXIS_DISTANCE)
        if (raw <= 0f) return 0.0
        val max = maxByDevice.getOrPut(e.deviceId) {
            try {
                InputDevice.getDevice(e.deviceId)?.motionRanges
                    ?.filter { it.axis == MotionEvent.AXIS_DISTANCE && it.max > 0f }
                    ?.maxOfOrNull { it.max.toDouble() } ?: 0.0
            } catch (_: Exception) {
                0.0
            }
        }
        return if (max > 1.0) (raw / max).coerceIn(0.0, 1.0).pow(DISTANCE_RESPONSE) else 0.0
    }

    /** Size multiplier for a cursor at [distanceFraction] (0 = on the glass, 1 = furthest). */
    fun sizeScale(distanceFraction: Double): Double =
        (1.0 - SHRINK_AT_MAX_DISTANCE * distanceFraction).coerceIn(0.0, 1.0)

    /** Opacity multiplier for a cursor at [distanceFraction]. */
    fun alpha(distanceFraction: Double): Double =
        (1.0 - FADE_AT_MAX_DISTANCE * distanceFraction).coerceIn(0.0, 1.0)
}
