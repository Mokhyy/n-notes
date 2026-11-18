package com.xnotes.stylus

/** Which connected pens a [StylusProfile] applies to. Every non-null field must match. */
data class StylusDeviceMatch(
    val vendorId: Int? = null,
    val productId: Int? = null,
    val nameContains: String? = null,
    /** Restrict the profile to a paired external pen (Bluetooth/USI) rather than a digitizer. */
    val bluetoothOnly: Boolean = false,
    /** Exact Android input-device descriptor, which survives across reconnects. */
    val descriptor: String? = null,
) {
    /** True when at least one identifying field is set, so a profile never matches every pen. */
    val isSpecific: Boolean
        get() = vendorId != null || productId != null || nameContains != null || bluetoothOnly ||
            descriptor != null

    fun matches(device: StylusDevice): Boolean {
        if (!isSpecific) return false
        if (vendorId != null && vendorId != device.vendorId) return false
        if (productId != null && productId != device.productId) return false
        if (nameContains != null && !device.name.contains(nameContains, ignoreCase = true)) return false
        if (bluetoothOnly && !device.bluetooth) return false
        if (descriptor != null && !descriptor.equals(device.descriptor, ignoreCase = true)) return false
        return true
    }
}

/**
 * The dedicated section for one pen: everything that may differ between styluses. A null field
 * inherits the same field from [StylusGeneralSettings], so a profile only records what makes that
 * pen special.
 *
 * [id] is stable and used as the persisted key; [name] is what the settings UI shows.
 */
data class StylusProfile(
    val id: String,
    val name: String,
    val match: StylusDeviceMatch = StylusDeviceMatch(),
    /** A disabled profile is skipped when matching, but its values survive. */
    val enabled: Boolean = true,
    /** Tool a held side button activates on this pen: a tool id, or "none". */
    val penButtonTool: String? = null,
    /** Whether the held side-button tool also acts while hovering. */
    val penButtonHover: Boolean? = null,
    /** Action mapped to this pen's barrel double-tap. */
    val doubleTapAction: String? = null,
    /** Action mapped to this pen's momentary side-button click. */
    val buttonTapAction: String? = null,
    /** Action mapped to this pen's first page-key button. */
    val button1TapAction: String? = null,
    /** Action mapped to this pen's second page-key button. */
    val button2TapAction: String? = null,
    /** Extra held-side-button keycodes this pen sends, added to the general set. */
    val heldKeycodes: Set<Int>? = null,
    /** Extra momentary side-button click keycodes this pen sends, added to the general set. */
    val clickOnlyKeycodes: Set<Int>? = null,
    /** Extra page-key keycodes this pen sends, added to the general set. */
    val pageKeyKeycodes: Set<Int>? = null,
    /** Extra held keycodes whose hardware also reports a quick click, added to the general set. */
    val clickableHeldKeycodes: Set<Int>? = null,
    /** Vendor keycode this pen uses for a barrel double-tap, when it is not the general one. */
    val doubleTapKeycode: Int? = null,
    /** Longest gap between the two presses of a barrel double-tap on this pen, in ms. */
    val barrelDoubleTapWindowMs: Long? = null,
    /** Whether this pen shows the app-drawn hover cursor; null inherits the general setting. */
    val hoverPreview: Boolean? = null,
    /** Whether this pen fades its hover cursor with distance; null inherits the general setting. */
    val hoverDistancePreview: Boolean? = null,
    /**
     * Free-form description shown on the pen's settings page: what this pen supports and how it
     * behaves. New profiles start from the detected capabilities, and the user can edit them.
     */
    val notes: String = "",
) {
    fun matches(device: StylusDevice): Boolean = enabled && match.matches(device)
}
