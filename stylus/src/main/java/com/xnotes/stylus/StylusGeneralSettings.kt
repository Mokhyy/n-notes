package com.xnotes.stylus

/**
 * The general section: settings that apply to every pen. A [StylusProfile] overrides individual
 * fields for one pen, so changing a general value moves every pen that has not overridden it.
 */
data class StylusGeneralSettings(
    /** Tool a held side button activates: a tool id, or "none". */
    val penButtonTool: String = "eraser",
    /** Whether the held side-button tool also acts while hovering (no contact needed). */
    val penButtonHover: Boolean = false,
    /** Action mapped to a barrel double-tap. */
    val doubleTapAction: String = "none",
    /** Action mapped to a momentary side-button click. */
    val buttonTapAction: String = "none",
    /** Action mapped to the first page-key button. */
    val button1TapAction: String = "none",
    /** Action mapped to the second page-key button. */
    val button2TapAction: String = "none",
    /** Keycodes any pen may report as held side buttons. */
    val heldKeycodes: Set<Int> = StylusKeys.DEFAULT_HELD,
    /** Keycodes any pen may report as momentary side-button clicks. */
    val clickOnlyKeycodes: Set<Int> = StylusKeys.DEFAULT_CLICK_ONLY,
    /** Keycodes used by pens whose two buttons arrive as the plain page keys. */
    val pageKeyKeycodes: Set<Int> = StylusKeys.PAGE_KEYS,
    /** Held keycodes whose hardware also reports a quick click. */
    val clickableHeldKeycodes: Set<Int> = StylusKeys.CLICKABLE_HELD,
    /** Vendor keycode a barrel double-tap arrives on. */
    val doubleTapKeycode: Int = StylusKeys.BARREL_DOUBLE_TAP,
    /** Longest gap between the two presses of a barrel double-tap, in ms. */
    val barrelDoubleTapWindowMs: Long = 600L,
)
