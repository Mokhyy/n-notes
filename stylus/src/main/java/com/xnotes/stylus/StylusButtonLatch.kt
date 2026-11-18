package com.xnotes.stylus

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration

/**
 * Tells which pen route a side button is on, so the editor and the canvas agree about whether a
 * pen's button is held, whichever stream reported it.
 *
 * The button may arrive in three ways, and a single pen can use more than one:
 *  1. as a bit in a touch event's buttonState (the S Pen and most digitizer pens);
 *  2. as a standard stylus keycode (Bluetooth/USI pens);
 *  3. as a vendor keycode for firmware without a standard mapping (see [StylusKeys]).
 *
 * The button masks and keycodes come from [StylusKeys] rather than copies, so every surface
 * cannot drift apart on which buttons count.
 */
class StylusButtonLatch {

    private var motionHeld = false

    /** True while a side button is held, from whichever stream reported it. */
    val held: Boolean
        get() = keyHeld || motionHeld

    /**
     * Latch the button off the generic-motion (hover) stream. Returns true when a stylus event
     * leaves the button up, so a caller can end a hovering gesture.
     */
    fun onGenericMotion(e: MotionEvent): Boolean =
        e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS && onStylusMotion(e.buttonState)

    fun onStylusMotion(buttonState: Int): Boolean {
        motionHeld = (buttonState and StylusKeys.BUTTON_MASK) != 0
        return !held
    }

    /**
     * Latch the button off a key event. Returns true when [keyCode] was a stylus side button, so
     * the host consumes it instead of letting it fall through as a normal key.
     */
    fun onKey(keyCode: Int, down: Boolean): Boolean {
        if (!isStylusButtonKey(keyCode)) return false
        keyHeld = down
        return true
    }

    /** Forget what the hover stream said; a held key belongs to the pen and its key-up clears it. */
    fun reset() {
        motionHeld = false
    }

    /** Whether [e] is a stylus contact with the side button down, on either route. */
    fun heldFor(e: MotionEvent): Boolean =
        e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS && heldWith(e.buttonState)

    fun heldWith(buttonState: Int): Boolean =
        (buttonState and StylusKeys.BUTTON_MASK) != 0 || held

    companion object {
        /** One pen, so one key state for every latch: its key-up can reach another pane than its key-down. */
        private var keyHeld = false

        /** The same pen's button clicks, kept once for the same reason; see [ButtonClick]. */
        val clicks = ButtonClick { ViewConfiguration.getLongPressTimeout().toLong() }

        /** Let go of a held key whose key-up may never arrive, as when the window loses focus. */
        fun releaseKey() {
            keyHeld = false
            clicks.cancel()
        }

        fun isStylusButtonKey(keyCode: Int): Boolean =
            StylusKeys.isStylusButtonKey(keyCode)
    }
}

/**
 * Tells a click of a side button from a hold, for a button whose hardware reports both when it
 * is pressed and released in quick succession. A quick press with the pen off the screen is a
 * click; a press held past the long-press timeout is a hold, and a touch while it is down makes
 * it a hold too, because the pen is being used rather than clicked.
 *
 * The hold itself acts at once (see the callers); this only decides afterwards whether the
 * release was also a click, so a press that turns into a hold still runs the tap action once.
 */
class ButtonClick(private val timeoutMs: () -> Long) {

    private var pressedAt = NONE
    private var touched = false
    private var touching = false

    /** The button went down at [at] (uptime ms). */
    fun onPress(at: Long) {
        pressedAt = at
        touched = touching
    }

    /** The button came up at [at]; true when the press was a click. */
    fun onRelease(at: Long): Boolean {
        val click = mayStillClick(at)
        pressedAt = NONE
        return click
    }

    /** Drop the press in progress, so its release cannot click. */
    fun cancel() {
        pressedAt = NONE
    }

    /** Whether the press in progress may still turn out a click at [at], so a hold must not act on it yet. */
    fun mayStillClick(at: Long): Boolean = pressedAt != NONE && !touched && at - pressedAt < timeoutMs()

    /** Follow the pen tip on and off the screen, from every touch the window gets. */
    fun onTouch(e: MotionEvent) = onTouch(e.actionMasked, e.getToolType(e.actionIndex))

    fun onTouch(action: Int, toolType: Int) {
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
                touching = true
                touched = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> if (toolType == MotionEvent.TOOL_TYPE_STYLUS) touching = false
            MotionEvent.ACTION_CANCEL -> touching = false
        }
    }

    private companion object {
        const val NONE = -1L
    }
}
