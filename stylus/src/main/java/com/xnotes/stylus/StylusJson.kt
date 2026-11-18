package com.xnotes.stylus

import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistence for the per-pen section. The general section stays in the app's existing flat
 * preference keys for backwards compatibility; profiles are new, so they get their own array.
 *
 * Forgiving on read: a missing, malformed or partially written entry falls back to the presets,
 * because a broken pen profile must never leave the user without working buttons.
 */
object StylusJson {

    fun profilesToJson(profiles: List<StylusProfile>): JSONArray =
        JSONArray().apply { profiles.forEach { put(profileToJson(it)) } }

    fun profilesFromJson(array: JSONArray?): List<StylusProfile> {
        if (array == null) return StylusPresets.profiles
        val saved = (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let { profileFromJson(it) } }
        return if (saved.isEmpty()) StylusPresets.profiles else saved
    }

    private fun profileToJson(p: StylusProfile): JSONObject = JSONObject()
        .put("id", p.id)
        .put("name", p.name)
        .put("notes", p.notes)
        .put("enabled", p.enabled)
        .put("match", JSONObject()
            .apply { p.match.vendorId?.let { put("vendor_id", it) } }
            .apply { p.match.productId?.let { put("product_id", it) } }
            .apply { p.match.nameContains?.let { put("name_contains", it) } }
            .apply { if (p.match.bluetoothOnly) put("bluetooth_only", true) }
            .apply { p.match.descriptor?.let { put("descriptor", it) } })
        .apply { p.penButtonTool?.let { put("pen_button_tool", it) } }
        .apply { p.penButtonHover?.let { put("pen_button_hover", it) } }
        .apply { p.doubleTapAction?.let { put("double_tap_action", it) } }
        .apply { p.buttonTapAction?.let { put("button_tap_action", it) } }
        .apply { p.button1TapAction?.let { put("button1_tap_action", it) } }
        .apply { p.button2TapAction?.let { put("button2_tap_action", it) } }
        .apply { p.heldKeycodes?.let { put("held_keycodes", keysToJson(it)) } }
        .apply { p.clickOnlyKeycodes?.let { put("click_only_keycodes", keysToJson(it)) } }
        .apply { p.pageKeyKeycodes?.let { put("page_key_keycodes", keysToJson(it)) } }
        .apply { p.clickableHeldKeycodes?.let { put("clickable_held_keycodes", keysToJson(it)) } }
        .apply { p.doubleTapKeycode?.let { put("double_tap_keycode", it) } }
        .apply { p.barrelDoubleTapWindowMs?.let { put("barrel_double_tap_window_ms", it) } }
        .apply { p.hoverPreview?.let { put("hover_preview", it) } }
        .apply { p.hoverDistancePreview?.let { put("hover_distance_preview", it) } }

    private fun profileFromJson(o: JSONObject): StylusProfile? {
        val id = o.optString("id", "").ifEmpty { return null }
        val match = o.optJSONObject("match")
        return StylusProfile(
            id = id,
            name = o.optString("name", id),
            notes = o.optString("notes", ""),
            match = StylusDeviceMatch(
                vendorId = match?.optIntOrNull("vendor_id"),
                productId = match?.optIntOrNull("product_id"),
                nameContains = match?.optString("name_contains", "")?.ifEmpty { null },
                bluetoothOnly = match?.optBoolean("bluetooth_only", false) ?: false,
                descriptor = match?.optString("descriptor", "")?.ifEmpty { null },
            ),
            enabled = o.optBoolean("enabled", true),
            penButtonTool = o.optString("pen_button_tool", "").ifEmpty { null },
            penButtonHover = o.optBooleanOrNull("pen_button_hover"),
            doubleTapAction = o.optString("double_tap_action", "").ifEmpty { null },
            buttonTapAction = o.optString("button_tap_action", "").ifEmpty { null },
            button1TapAction = o.optString("button1_tap_action", "").ifEmpty { null },
            button2TapAction = o.optString("button2_tap_action", "").ifEmpty { null },
            heldKeycodes = o.optJSONArray("held_keycodes")?.let { keysFromJson(it) },
            clickOnlyKeycodes = o.optJSONArray("click_only_keycodes")?.let { keysFromJson(it) },
            pageKeyKeycodes = o.optJSONArray("page_key_keycodes")?.let { keysFromJson(it) },
            clickableHeldKeycodes = o.optJSONArray("clickable_held_keycodes")?.let { keysFromJson(it) },
            doubleTapKeycode = o.optIntOrNull("double_tap_keycode"),
            barrelDoubleTapWindowMs = o.optLongOrNull("barrel_double_tap_window_ms"),
            hoverPreview = o.optBooleanOrNull("hover_preview"),
            hoverDistancePreview = o.optBooleanOrNull("hover_distance_preview"),
        )
    }

    private fun keysToJson(keys: Set<Int>): JSONArray = JSONArray().apply { keys.sorted().forEach { put(it) } }

    private fun keysFromJson(a: JSONArray): Set<Int> =
        (0 until a.length()).map { a.optInt(it) }.toSet()

    private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key)) optInt(key) else null
    private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key)) optLong(key) else null
    private fun JSONObject.optBooleanOrNull(key: String): Boolean? = if (has(key)) optBoolean(key) else null
}
