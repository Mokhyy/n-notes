package com.xnotes.stylus

import android.view.InputDevice

/**
 * The identity of a connected pen, as Android reports it on its input events. Profiles match
 * against this rather than against a product name alone, so two pens from one vendor can still
 * be told apart by product id.
 */
data class StylusDevice(
    val id: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val sources: Int,
) {
    /** A paired external pen (Bluetooth/USI) rather than the screen's digitizer. */
    val bluetooth: Boolean get() = (sources and InputDevice.SOURCE_BLUETOOTH_STYLUS) != 0

    companion object {
        /** The pen behind an event's `deviceId`, or null when it is unknown or not available. */
        fun of(id: Int): StylusDevice? = try {
            InputDevice.getDevice(id)?.let { d ->
                StylusDevice(d.id, d.name.orEmpty(), d.vendorId, d.productId, d.sources)
            }
        } catch (_: Exception) {
            null
        }
    }
}
