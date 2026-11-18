package com.xnotes.stylus

/**
 * The pens the app knows from the stylus-diagnostics reports. Each entry records only what makes
 * that pen different; everything else it inherits from [StylusGeneralSettings]. Users can edit or
 * disable these profiles and add their own in Preferences.
 */
object StylusPresets {

    val profiles: List<StylusProfile> = listOf(
        StylusProfile(
            id = "samsung-s-pen",
            name = "Samsung S Pen",
            match = StylusDeviceMatch(nameContains = "S Pen"),
        ),
        StylusProfile(
            id = "huawei-m-pencil",
            name = "Huawei M-Pencil",
            match = StylusDeviceMatch(nameContains = "M-Pencil"),
            doubleTapKeycode = StylusKeys.BARREL_DOUBLE_TAP,
            barrelDoubleTapWindowMs = 600L,
        ),
        StylusProfile(
            id = "honor-magic-pencil",
            name = "HONOR Magic-Pencil",
            match = StylusDeviceMatch(nameContains = "Magic-Pencil"),
            clickOnlyKeycodes = setOf(333),
        ),
        StylusProfile(
            id = "lenovo-tab-pen",
            name = "Lenovo Tab Pen Plus",
            match = StylusDeviceMatch(nameContains = "Tab Pen"),
            clickOnlyKeycodes = setOf(600, 601, 602, 603, 604),
        ),
        StylusProfile(
            id = "redmi-smart-pen",
            name = "Redmi Smart Pen",
            match = StylusDeviceMatch(nameContains = "Smart Pen"),
            pageKeyKeycodes = StylusKeys.PAGE_KEYS,
        ),
        StylusProfile(
            id = "oneplus-stylo",
            name = "OnePlus Pad Go 2 Stylo",
            match = StylusDeviceMatch(nameContains = "Stylo"),
            heldKeycodes = setOf(StylusKeys.VENDOR_HELD),
            clickableHeldKeycodes = setOf(StylusKeys.VENDOR_HELD),
        ),
        StylusProfile(
            id = "xiaomi-focus-pen",
            name = "Xiaomi Focus Pen Pro",
            match = StylusDeviceMatch(nameContains = "Focus Pen"),
            heldKeycodes = setOf(StylusKeys.FOCUS_PEN_PRO_PINCH),
            clickableHeldKeycodes = setOf(StylusKeys.FOCUS_PEN_PRO_PINCH),
        ),
    )

    fun byId(id: String): StylusProfile? = profiles.firstOrNull { it.id == id }
}
