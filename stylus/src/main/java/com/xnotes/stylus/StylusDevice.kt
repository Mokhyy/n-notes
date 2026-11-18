package com.xnotes.stylus

import android.view.InputDevice

/**
 * The identity of a connected pen, as Android reports it on its input events. Profiles match
 * against this rather than against a product name alone, so two pens from one vendor can still
 * be told apart by product id or unique id.
 */
data class StylusDevice(
    val id: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val sources: Int,
    val descriptor: String = "",
) {
    /** A paired external pen (Bluetooth/USI) rather than the screen's digitizer. */
    val bluetooth: Boolean get() = (sources and InputDevice.SOURCE_BLUETOOTH_STYLUS) != 0

    /** Whether Android reports this device as a stylus digitizer. */
    val digitizer: Boolean get() = (sources and InputDevice.SOURCE_STYLUS) != 0

    companion object {
        /** The pen behind an event's `deviceId`, or null when it is unknown or not available. */
        fun of(id: Int): StylusDevice? = try {
            InputDevice.getDevice(id)?.let { of(it) }
        } catch (_: Exception) {
            null
        }

        /** A snapshot of [device]'s identity. */
        fun of(device: InputDevice): StylusDevice = StylusDevice(
            id = device.id,
            name = device.name.orEmpty(),
            vendorId = device.vendorId,
            productId = device.productId,
            sources = device.sources,
            descriptor = device.descriptor.orEmpty(),
        )
    }
}
