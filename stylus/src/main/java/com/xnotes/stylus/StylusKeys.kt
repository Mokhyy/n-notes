package com.xnotes.stylus

import android.view.KeyEvent
import android.view.MotionEvent

/**
 * How the wide range of Android pens report their side buttons.
 *
 * A pen may reach us by any of three routes, and different hardware mixes them:
 *  - button bits in a [MotionEvent]'s buttonState (most digitizer pens, e.g. the S Pen);
 *  - standard stylus button keycodes (Bluetooth/USI pens);
 *  - vendor keycodes for firmware with no standard mapping (Huawei, HONOR, Lenovo, Redmi,
 *    OnePlus, Xiaomi).
 *
 * The sets below are the general defaults. A [StylusProfile] may replace any of them for one
 * class of pen without touching every other pen's behaviour.
 */
object StylusKeys {

    /** Button bits that mean "a stylus side button is down" in a MotionEvent's buttonState. */
    val BUTTON_MASK: Int = MotionEvent.BUTTON_STYLUS_PRIMARY or
        MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY

    /** Standard side-button keycodes every Bluetooth/USI pen may send. */
    val STANDARD_HELD: Set<Int> = setOf(
        KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY,
        KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY,
        KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY,
        KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL,
    )

    /** OnePlus Pad Go 2 Stylo reports a held side button as F21. */
    val VENDOR_HELD: Int = KeyEvent.KEYCODE_F21

    /** Xiaomi Focus Pen Pro has no button and sends its pinch as BUTTON_7. */
    val FOCUS_PEN_PRO_PINCH: Int = KeyEvent.KEYCODE_BUTTON_7

    /** Huawei M-Pencil's barrel double-tap arrives as this vendor keycode. */
    const val BARREL_DOUBLE_TAP: Int = 718

    /** Momentary-click vendor keycodes: HONOR Magic-Pencil 4s (333), Lenovo Tab Pen Plus (600–604). */
    val VENDOR_CLICK_ONLY: Set<Int> = setOf(333, 600, 601, 602, 603, 604)

    /** Redmi Smart Pen's two buttons arrive as the plain page keys. */
    val PAGE_KEYS: Set<Int> = setOf(KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_PAGE_DOWN)

    /** Held buttons whose hardware also reports a quick click, so click and hold can both work. */
    val CLICKABLE_HELD: Set<Int> = setOf(KeyEvent.KEYCODE_F21, KeyEvent.KEYCODE_BUTTON_7)

    /** The held keys a generic pen may send. */
    val DEFAULT_HELD: Set<Int> = STANDARD_HELD + VENDOR_HELD + FOCUS_PEN_PRO_PINCH

    /** The click/tap keys a generic pen may send. */
    val DEFAULT_CLICK_ONLY: Set<Int> = VENDOR_CLICK_ONLY + PAGE_KEYS

    /** Whether [keyCode] is a side button any known pen sends as a held key. */
    fun isStylusButtonKey(keyCode: Int): Boolean = keyCode in DEFAULT_HELD
}
