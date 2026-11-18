package com.xnotes.stylus

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * One connected pen or digitizer and what it actually reports. The capabilities are read from the
 * device's motion ranges and key map, not guessed from its name, so the per-pen page can show the
 * truth about a pen even when the ROM never draws its pointer.
 */
data class StylusDeviceInfo(
    val device: StylusDevice,
    val hasPressure: Boolean,
    val pressureMax: Float,
    val hasTilt: Boolean,
    val hasOrientation: Boolean,
    val hasDistance: Boolean,
    val distanceMax: Float,
    val hasButtons: Boolean,
    val isDigitizer: Boolean,
    val isBluetoothHid: Boolean,
) {
    /** A continuous distance axis has real values to fade with; a binary one only says near/far. */
    val distanceContinuous: Boolean get() = distanceMax > 1f

    fun summary(): String = buildList {
        add(if (isDigitizer) "Stylus digitizer" else "Bluetooth pen")
        add(if (hasPressure) "Pressure" else "No pressure")
        if (hasTilt) add("Tilt")
        if (hasOrientation) add("Orientation")
        if (hasDistance) add(if (distanceContinuous) "Hover distance" else "Binary hover distance")
        if (hasButtons) add("Buttons")
    }.joinToString(" · ")

    companion object {
        private val BUTTON_KEYS = intArrayOf(
            KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY,
            KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY,
            KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY,
            KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_F13,
            KeyEvent.KEYCODE_F14,
            KeyEvent.KEYCODE_F15,
            KeyEvent.KEYCODE_F16,
            KeyEvent.KEYCODE_F17,
            KeyEvent.KEYCODE_F18,
            KeyEvent.KEYCODE_F19,
        )

        fun of(device: InputDevice, snapshot: StylusDevice = StylusDevice.of(device)): StylusDeviceInfo {
            val ranges = device.motionRanges
            fun range(axis: Int) = ranges.firstOrNull { it.axis == axis && it.max > 0f }
            return StylusDeviceInfo(
                device = snapshot,
                hasPressure = range(MotionEvent.AXIS_PRESSURE) != null,
                pressureMax = range(MotionEvent.AXIS_PRESSURE)?.max ?: 0f,
                hasTilt = range(MotionEvent.AXIS_TILT) != null,
                hasOrientation = range(MotionEvent.AXIS_ORIENTATION) != null,
                hasDistance = range(MotionEvent.AXIS_DISTANCE) != null,
                distanceMax = range(MotionEvent.AXIS_DISTANCE)?.max ?: 0f,
                hasButtons = runCatching { device.hasKeys(*BUTTON_KEYS).any { it } }.getOrDefault(false),
                isDigitizer = device.supportsSource(InputDevice.SOURCE_STYLUS),
                isBluetoothHid = snapshot.bluetooth ||
                    (!device.supportsSource(InputDevice.SOURCE_STYLUS) && device.isExternal),
            )
        }

        /** Combine two interfaces of the same pen (its HID, mouse and keyboard nodes). */
        fun richer(a: StylusDeviceInfo, b: StylusDeviceInfo): StylusDeviceInfo {
            val digitizer = if (b.isDigitizer && !a.isDigitizer) b else a
            // A digitizer's node is the one that draws; otherwise keep the shortest name, which is
            // the pen itself rather than its "... Mouse" or "... Keyboard" HID node.
            val representative = when {
                b.isDigitizer && !a.isDigitizer -> b.device
                a.isDigitizer && !b.isDigitizer -> a.device
                b.device.name.length < a.device.name.length -> b.device
                else -> a.device
            }
            return digitizer.copy(
                device = representative,
                hasPressure = a.hasPressure || b.hasPressure,
                pressureMax = maxOf(a.pressureMax, b.pressureMax),
                hasTilt = a.hasTilt || b.hasTilt,
                hasOrientation = a.hasOrientation || b.hasOrientation,
                hasDistance = a.hasDistance || b.hasDistance,
                distanceMax = maxOf(a.distanceMax, b.distanceMax),
                hasButtons = a.hasButtons || b.hasButtons,
                isDigitizer = a.isDigitizer || b.isDigitizer,
                isBluetoothHid = a.isBluetoothHid || b.isBluetoothHid,
            )
        }
    }
}

/**
 * Enumerates the pens Android currently knows about. A pen can expose several input nodes (its
 * Bluetooth HID, mouse and keyboard interfaces), so nodes sharing a unique id or descriptor are
 * collapsed. A Bluetooth pen may not advertise a STYLUS source at all on some ROMs, so an external
 * device whose name says pen/stylus/pencil is listed too.
 */
object StylusDiscovery {

    fun scan(context: Context): List<StylusDeviceInfo> {
        val im = context.getSystemService(Context.INPUT_SERVICE) as? InputManager ?: return emptyList()
        val grouped = LinkedHashMap<String, StylusDeviceInfo>()
        for (id in im.inputDeviceIds) {
            val device = runCatching { im.getInputDevice(id) }.getOrNull() ?: continue
            if (!isPenLike(device)) continue
            val snapshot = StylusDevice.of(device)
            val info = StylusDeviceInfo.of(device, snapshot)
            // The Bluetooth HID, mouse and keyboard nodes share no public unique id, so group
            // external nodes by vendor/product; digitizers are distinct hardware and keep their
            // own descriptor.
            val key = when {
                device.isExternal && snapshot.vendorId != 0 && snapshot.productId != 0 ->
                    "hid:${snapshot.vendorId}:${snapshot.productId}"
                snapshot.descriptor.isNotBlank() -> snapshot.descriptor
                else -> "id:$id"
            }
            val existing = grouped[key]
            grouped[key] = if (existing == null) info else StylusDeviceInfo.richer(existing, info)
        }
        return grouped.values.sortedBy { it.device.name.lowercase() }
    }

    private fun isPenLike(device: InputDevice): Boolean {
        if (device.supportsSource(InputDevice.SOURCE_STYLUS)) return true
        if (device.supportsSource(InputDevice.SOURCE_BLUETOOTH_STYLUS)) return true
        if (!device.isExternal) return false
        val name = device.name.orEmpty().lowercase()
        return name.contains("pen") || name.contains("stylus") || name.contains("pencil")
    }
}
