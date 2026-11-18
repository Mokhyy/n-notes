package com.xnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.settings.Preferences
import com.xnotes.stylus.StylusDeviceInfo
import com.xnotes.stylus.StylusDeviceMatch
import com.xnotes.stylus.StylusDiscovery
import com.xnotes.stylus.StylusProfile
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/**
 * The dedicated pen settings page: one general section every pen inherits, then a page per pen
 * with its identity, detected capabilities and overrides. All pen behaviour stays in the `:stylus`
 * module; this file only binds those models to the existing settings controls.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StylusPensSection(prefs: Preferences, update: (Preferences) -> Unit, editor: Editor) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf(emptyList<StylusDeviceInfo>()) }
    fun refresh() {
        devices = StylusDiscovery.scan(context)
    }
    LaunchedEffect(Unit) { refresh() }

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var labOpen by rememberSaveable { mutableStateOf(false) }
    var labProfileId by rememberSaveable { mutableStateOf<String?>(null) }
    if (labOpen) {
        StylusLabPage(
            editor = editor,
            profile = prefs.stylusProfiles.firstOrNull { it.id == labProfileId },
            devices = devices,
            onBack = { labOpen = false; labProfileId = null },
        )
        return
    }
    val selected = prefs.stylusProfiles.firstOrNull { it.id == selectedId }
    if (selected != null) {
        StylusPenDetail(prefs, update, selected, devices, onBack = { selectedId = null }, onLab = {
            labProfileId = selected.id
            labOpen = true
        })
    } else {
        StylusGeneralSection(prefs, update, devices, ::refresh, onOpen = { selectedId = it }, onLab = {
            labProfileId = null
            labOpen = true
        })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StylusGeneralSection(
    prefs: Preferences,
    update: (Preferences) -> Unit,
    devices: List<StylusDeviceInfo>,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    onLab: () -> Unit,
) {
    val palette = LocalPalette.current
    SectionTitle(stringResource(R.string.pref_stylus_general))
    Text(
        stringResource(R.string.pref_stylus_general_help),
        color = palette.textDim.toComposeColor(),
        fontSize = 12.sp,
    )
    TextButton(onClick = onLab) { Text(stringResource(R.string.pref_stylus_lab_test_any), fontSize = 13.sp) }
    FieldLabel(stringResource(R.string.pref_pen_button_hold))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        penButtonOptions.forEach { (id, label) ->
            Chip(stringResource(label), prefs.penButtonTool == id) { update(prefs.copy(penButtonTool = id)) }
        }
    }
    if (prefs.penButtonTool == "eraser" || prefs.penButtonTool == "pan") {
        CheckRow(stringResource(R.string.pref_pen_button_hover), prefs.penButtonHover) {
            update(prefs.copy(penButtonHover = it))
        }
    }
    CheckRow(stringResource(R.string.pref_stylus_hover_preview), prefs.stylusHoverPreview) {
        update(prefs.copy(stylusHoverPreview = it))
    }
    if (prefs.stylusHoverPreview) {
        CheckRow(stringResource(R.string.pref_stylus_hover_distance), prefs.stylusHoverDistance) {
            update(prefs.copy(stylusHoverDistance = it))
        }
    }
    CheckRow(stringResource(R.string.pref_stylus_store_tilt), prefs.stylusStoreTiltAngle) {
        update(prefs.copy(stylusStoreTiltAngle = it))
    }
    CheckRow(stringResource(R.string.pref_stylus_store_orientation), prefs.stylusStoreOrientation) {
        update(prefs.copy(stylusStoreOrientation = it))
    }
    FieldLabel(stringResource(R.string.pref_stylus_pressure_curve))
    StylusCurveEditor(
        prefs.stylusPressureCurve,
        inputLabel = stringResource(R.string.curve_axis_pressure),
    ) { update(prefs.copy(stylusPressureCurve = it)) }
    FieldLabel(stringResource(R.string.pref_stylus_tilt_curve))
    StylusCurveEditor(
        prefs.stylusTiltCurve,
        inputLabel = stringResource(R.string.curve_axis_tilt),
    ) { update(prefs.copy(stylusTiltCurve = it)) }
    val tapOptions = tapGestureOptions.map { (id, res) -> id to stringResource(res) }
    FieldLabel(stringResource(R.string.pref_stylus_double_tap))
    OptionDropdown(tapOptions, prefs.stylusDoubleTap) { update(prefs.copy(stylusDoubleTap = it)) }
    FieldLabel(stringResource(R.string.pref_stylus_button_tap))
    OptionDropdown(tapOptions, prefs.stylusButtonTap) { update(prefs.copy(stylusButtonTap = it)) }

    Spacer(Modifier.height(10.dp))
    SectionTitle(stringResource(R.string.pref_stylus_pens))
    prefs.stylusProfiles.forEach { profile ->
        val connected = devices.firstOrNull { profile.match.matches(it.device) }
        StylusPenRow(
            profile = profile,
            connected = connected,
            active = prefs.stylusActiveProfile == profile.id,
            onClick = { onOpen(profile.id) },
        )
    }

    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(stringResource(R.string.pref_stylus_detected))
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onRefresh) { Text(stringResource(R.string.pref_stylus_refresh), fontSize = 12.sp) }
    }
    val unknown = devices.filter { info -> prefs.stylusProfiles.none { it.match.matches(info.device) } }
    if (unknown.isEmpty()) {
        Text(stringResource(R.string.pref_stylus_no_detected), color = palette.textDim.toComposeColor(), fontSize = 12.sp)
    } else {
        unknown.forEach { info ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(info.device.name, color = palette.text.toComposeColor(), fontSize = 14.sp)
                Text(info.summary(), color = palette.textDim.toComposeColor(), fontSize = 11.sp)
                TextButton(onClick = {
                    val profile = profileFor(info, newProfileId(prefs))
                    update(prefs.copy(stylusProfiles = prefs.stylusProfiles + profile))
                    onOpen(profile.id)
                }) {
                    Text(stringResource(R.string.pref_stylus_add, info.device.name), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StylusPenRow(
    profile: StylusProfile,
    connected: StylusDeviceInfo?,
    active: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(palette.surface.toComposeColor())
            .border(1.dp, palette.border.toComposeColor(), MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                profile.name,
                color = palette.text.toComposeColor(),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            if (active) {
                Spacer(Modifier.width(8.dp))
                Badge(stringResource(R.string.pref_stylus_use_for_drawing))
            }
        }
        if (profile.notes.isNotBlank()) {
            Text(profile.notes, color = palette.textDim.toComposeColor(), fontSize = 11.sp)
        }
        Text(
            connected?.summary() ?: stringResource(R.string.pref_stylus_not_connected),
            color = palette.textDim.toComposeColor(),
            fontSize = 11.sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StylusPenDetail(
    prefs: Preferences,
    update: (Preferences) -> Unit,
    profile: StylusProfile,
    devices: List<StylusDeviceInfo>,
    onBack: () -> Unit,
    onLab: () -> Unit,
) {
    val palette = LocalPalette.current
    val connected = devices.firstOrNull { profile.match.matches(it.device) }
    // On a tablet whose digitizer is shared, pinning a Bluetooth profile is how a pen is chosen;
    // show that digitizer's capabilities so the page still tells the truth about the hardware.
    val caps = connected ?: devices.firstOrNull { it.isDigitizer && prefs.stylusActiveProfile == profile.id }
    fun save(next: StylusProfile) {
        update(prefs.copy(stylusProfiles = prefs.stylusProfiles.map { if (it.id == next.id) next else it }))
    }

    TextButton(onClick = onBack) { Text("‹ " + stringResource(R.string.pref_stylus_back), fontSize = 13.sp) }
    TextButton(onClick = onLab) { Text(stringResource(R.string.pref_stylus_lab_test), fontSize = 13.sp) }
    OutlinedTextField(
        value = profile.name,
        onValueChange = { save(profile.copy(name = it)) },
        label = { Text(stringResource(R.string.pref_stylus_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = profile.notes,
        onValueChange = { save(profile.copy(notes = it)) },
        label = { Text(stringResource(R.string.pref_stylus_notes)) },
        placeholder = { Text(stringResource(R.string.pref_stylus_notes_hint)) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )

    SectionTitle(stringResource(R.string.pref_stylus_identity))
    Text(
        "${profile.name} · ${if (connected != null) stringResource(R.string.pref_stylus_connected, connected.device.name) else stringResource(R.string.pref_stylus_not_connected)}",
        color = palette.textDim.toComposeColor(),
        fontSize = 12.sp,
    )
    connected?.let {
        Text(
            "vendor 0x${it.device.vendorId.toString(16)} · product 0x${it.device.productId.toString(16)} · ${it.device.descriptor}",
            color = palette.textDim.toComposeColor(),
            fontSize = 11.sp,
        )
    }

    SectionTitle(stringResource(R.string.pref_stylus_capabilities))
    if (caps == null) {
        Text(stringResource(R.string.pref_stylus_no_caps), color = palette.textDim.toComposeColor(), fontSize = 12.sp)
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (caps.hasPressure) Badge("Pressure · ${caps.pressureMax.toInt()}")
            if (caps.hasTilt) Badge("Tilt")
            if (caps.hasOrientation) Badge("Orientation")
            if (caps.hasDistance) Badge(if (caps.distanceContinuous) "Hover distance" else "Binary hover distance")
            if (caps.hasButtons) Badge("Buttons")
        }
    }

    CheckRow(stringResource(R.string.pref_stylus_use_for_drawing), prefs.stylusActiveProfile == profile.id) {
        update(prefs.copy(stylusActiveProfile = if (it) profile.id else null))
    }
    CheckRow(stringResource(R.string.pref_stylus_enabled), profile.enabled) {
        save(profile.copy(enabled = it))
    }

    Spacer(Modifier.height(8.dp))
    SectionTitle(stringResource(R.string.pref_stylus_override))
    FieldLabel(stringResource(R.string.pref_pen_button_hold))
    CheckRow(stringResource(R.string.pref_stylus_override), profile.penButtonTool != null) {
        save(profile.copy(penButtonTool = if (it) prefs.penButtonTool else null))
    }
    if (profile.penButtonTool != null) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            penButtonOptions.forEach { (id, label) ->
                Chip(stringResource(label), profile.penButtonTool == id) { save(profile.copy(penButtonTool = id)) }
            }
        }
    }
    val effectiveTool = profile.penButtonTool ?: prefs.penButtonTool
    if (effectiveTool == "eraser" || effectiveTool == "pan") {
        BoolOverride(
            label = stringResource(R.string.pref_pen_button_hover),
            value = profile.penButtonHover,
            inherited = prefs.penButtonHover,
        ) { save(profile.copy(penButtonHover = it)) }
    }
    BoolOverride(
        label = stringResource(R.string.pref_stylus_hover_preview),
        value = profile.hoverPreview,
        inherited = prefs.stylusHoverPreview,
    ) { save(profile.copy(hoverPreview = it)) }
    val effectivePreview = profile.hoverPreview ?: prefs.stylusHoverPreview
    if (effectivePreview) {
        BoolOverride(
            label = stringResource(R.string.pref_stylus_hover_distance),
            value = profile.hoverDistancePreview,
            inherited = prefs.stylusHoverDistance,
        ) { save(profile.copy(hoverDistancePreview = it)) }
    }

    ActionOverride(stringResource(R.string.pref_stylus_double_tap), profile.doubleTapAction) {
        save(profile.copy(doubleTapAction = it))
    }
    ActionOverride(stringResource(R.string.pref_stylus_button_tap), profile.buttonTapAction) {
        save(profile.copy(buttonTapAction = it))
    }
    ActionOverride(stringResource(R.string.pref_stylus_page_up), profile.button1TapAction) {
        save(profile.copy(button1TapAction = it))
    }
    ActionOverride(stringResource(R.string.pref_stylus_page_down), profile.button2TapAction) {
        save(profile.copy(button2TapAction = it))
    }

    Spacer(Modifier.height(10.dp))
    TextButton(onClick = { save(profile.withGeneralOverrides()) }) {
        Text(stringResource(R.string.pref_stylus_reset), fontSize = 13.sp)
    }
    TextButton(onClick = {
        val copy = profile.copy(
            id = newProfileId(prefs),
            name = profile.name + " copy",
        )
        update(prefs.copy(stylusProfiles = prefs.stylusProfiles + copy))
        onBack()
    }) { Text(stringResource(R.string.pref_stylus_duplicate), fontSize = 13.sp) }
    TextButton(onClick = {
        update(
            prefs.copy(
                stylusProfiles = prefs.stylusProfiles.filter { it.id != profile.id },
                stylusActiveProfile = prefs.stylusActiveProfile?.takeIf { it != profile.id },
            ),
        )
        onBack()
    }) { Text(stringResource(R.string.pref_stylus_delete), fontSize = 13.sp, color = palette.accent.toComposeColor()) }
}

@Composable
private fun BoolOverride(label: String, value: Boolean?, inherited: Boolean, onChange: (Boolean?) -> Unit) {
    CheckRow("$label · ${stringResource(R.string.pref_stylus_override)}", value != null) {
        onChange(if (it) inherited else null)
    }
    if (value != null) CheckRow(label, value) { onChange(it) }
}

@Composable
private fun ActionOverride(label: String, value: String?, onChange: (String?) -> Unit) {
    FieldLabel(label)
    val options = buildList {
        add("" to stringResource(R.string.pref_stylus_inherit))
        addAll(tapGestureOptions.map { (id, res) -> id to stringResource(res) })
    }
    OptionDropdown(options, value ?: "") { onChange(it.ifEmpty { null }) }
}

@Composable
private fun Badge(text: String) {
    val palette = LocalPalette.current
    Text(
        text,
        color = palette.textDim.toComposeColor(),
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

private fun profileFor(info: StylusDeviceInfo, id: String): StylusProfile {
    // A Bluetooth pen is best identified by vendor/product (its HID, mouse and keyboard nodes
    // share those); a digitizer has no stable public unique id, so its descriptor is the identity.
    val external = info.isBluetoothHid && info.device.vendorId != 0 && info.device.productId != 0
    val descriptor = info.device.descriptor.takeIf { !external && it.isNotBlank() }
    return StylusProfile(
        id = id,
        name = info.device.name.ifBlank { "Pen" },
        match = StylusDeviceMatch(
            vendorId = info.device.vendorId.takeIf { external },
            productId = info.device.productId.takeIf { external },
            nameContains = info.device.name.takeIf { it.isNotBlank() && descriptor == null && !external },
            descriptor = descriptor,
        ),
        notes = info.summary(),
    )
}

private fun newProfileId(prefs: Preferences): String {
    var i = 1
    while (prefs.stylusProfiles.any { it.id == "pen-$i" }) i++
    return "pen-$i"
}

/** Drop every per-pen override, leaving the profile's identity, notes and enabled state. */
private fun StylusProfile.withGeneralOverrides(): StylusProfile = copy(
    penButtonTool = null,
    penButtonHover = null,
    doubleTapAction = null,
    buttonTapAction = null,
    button1TapAction = null,
    button2TapAction = null,
    hoverPreview = null,
    hoverDistancePreview = null,
    heldKeycodes = null,
    clickOnlyKeycodes = null,
    pageKeyKeycodes = null,
    clickableHeldKeycodes = null,
    doubleTapKeycode = null,
    barrelDoubleTapWindowMs = null,
)
