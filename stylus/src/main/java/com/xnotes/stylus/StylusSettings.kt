package com.xnotes.stylus

import android.view.KeyEvent

/**
 * What one pen's buttons do after its profile is merged over the general settings. Every field is
 * resolved, so callers never have to know whether a value came from the pen or the general set.
 */
data class EffectiveStylusSettings(
    /** The matched profile's id, or null when the general settings are in force. */
    val profileId: String?,
    val penButtonTool: String,
    val penButtonHover: Boolean,
    val doubleTapAction: String,
    val buttonTapAction: String,
    val button1TapAction: String,
    val button2TapAction: String,
    val heldKeycodes: Set<Int>,
    val clickOnlyKeycodes: Set<Int>,
    val pageKeyKeycodes: Set<Int>,
    val clickableHeldKeycodes: Set<Int>,
    val doubleTapKeycode: Int,
    val barrelDoubleTapWindowMs: Long,
    /** Whether the app draws the hovering pen's cursor. */
    val hoverPreview: Boolean,
    /** Whether that cursor fades and shrinks with the pen's hover distance. */
    val hoverDistancePreview: Boolean,
) {
    /** Whether [keyCode] is a held side button on this pen. */
    fun isHeldButton(keyCode: Int): Boolean = keyCode in heldKeycodes

    /** Whether [keyCode] is a momentary click or page-key button on this pen. */
    fun isTapButton(keyCode: Int): Boolean =
        keyCode in clickOnlyKeycodes || keyCode in pageKeyKeycodes

    /** The action a tap on [keyCode] runs: the page-key actions for the page keys, else the button action. */
    fun tapActionFor(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_PAGE_UP -> button1TapAction
        KeyEvent.KEYCODE_PAGE_DOWN -> button2TapAction
        else -> buttonTapAction
    }
}

/**
 * The stylus module's settings: one general section for every pen and a list of per-pen profiles.
 * Profiles are matched against the connected pen; an explicit [activeProfileId] pins one instead.
 */
data class StylusSettings(
    val general: StylusGeneralSettings = StylusGeneralSettings(),
    val profiles: List<StylusProfile> = StylusPresets.profiles,
    /** Force one profile instead of matching the connected pen, or null to match. */
    val activeProfileId: String? = null,
) {
    /** The profile that governs [device]: the pinned one, else the first enabled match. */
    fun profileFor(device: StylusDevice?): StylusProfile? {
        activeProfileId?.let { id ->
            profiles.firstOrNull { it.id == id && it.enabled }?.let { return it }
        }
        if (device == null) return null
        return profiles.firstOrNull { it.matches(device) }
    }

    /** The general settings with [device]'s profile folded over them. */
    fun effectiveFor(device: StylusDevice?): EffectiveStylusSettings {
        val p = profileFor(device)
        return EffectiveStylusSettings(
            profileId = p?.id,
            penButtonTool = p?.penButtonTool ?: general.penButtonTool,
            penButtonHover = p?.penButtonHover ?: general.penButtonHover,
            doubleTapAction = p?.doubleTapAction ?: general.doubleTapAction,
            buttonTapAction = p?.buttonTapAction ?: general.buttonTapAction,
            button1TapAction = p?.button1TapAction ?: general.button1TapAction,
            button2TapAction = p?.button2TapAction ?: general.button2TapAction,
            heldKeycodes = general.heldKeycodes + (p?.heldKeycodes ?: emptySet()),
            clickOnlyKeycodes = general.clickOnlyKeycodes + (p?.clickOnlyKeycodes ?: emptySet()),
            pageKeyKeycodes = general.pageKeyKeycodes + (p?.pageKeyKeycodes ?: emptySet()),
            clickableHeldKeycodes = general.clickableHeldKeycodes + (p?.clickableHeldKeycodes ?: emptySet()),
            doubleTapKeycode = p?.doubleTapKeycode ?: general.doubleTapKeycode,
            barrelDoubleTapWindowMs = p?.barrelDoubleTapWindowMs ?: general.barrelDoubleTapWindowMs,
            hoverPreview = p?.hoverPreview ?: general.hoverPreview,
            hoverDistancePreview = p?.hoverDistancePreview ?: general.hoverDistancePreview,
        )
    }
}
