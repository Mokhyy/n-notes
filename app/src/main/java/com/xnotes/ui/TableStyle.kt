package com.xnotes.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xnotes.R
import com.xnotes.core.model.BoxStyle
import com.xnotes.core.model.Rgba
import com.xnotes.core.model.TableItem
import com.xnotes.core.model.TableLook
import com.xnotes.core.tools.ToolConversions

private const val MIN_WIDTH = 1f
private const val MAX_WIDTH = 12f
private const val FIRST_BORDER = 2.0
private const val FIRST_CORNER = 8.0

/** The cards the table tool and a table's Change style both show: [colour], fill, border, corners, then [counts], thickness, dashes and glow. */
internal fun TableLook.groups(
    ink: Rgba,
    recents: List<Rgba>,
    colour: MenuGroup?,
    counts: List<MenuGroup>,
    onChange: (TableLook, preview: Boolean) -> Unit,
): List<List<MenuGroup>> {
    val look = this
    fun set(next: TableLook) = onChange(next, false)
    fun drag(next: TableLook) = onChange(next, true)
    val settle = { onChange(look, false) }
    val frame = buildList<MenuGroup> {
        if (colour != null) add(colour)
        add {
            MenuSwitch(stringResource(R.string.caption_fill), box.fill != null) { on -> set(copy(box = box.copy(fill = if (on) ink.copy(a = 64) else null))) }
            box.fill?.let { fill ->
                MenuRow(stringResource(R.string.caption_fill_colour)) {
                    ColorPickerDot(fill, custom = true, onPick = { set(copy(box = box.copy(fill = it))) }, dismissOnPick = false) { d, p ->
                        ColorPickerPopup(fill, recents, d, p)
                    }
                }
            }
        }
        add {
            MenuSwitch(stringResource(R.string.box_border), box.outlined) { on ->
                set(copy(box = if (on) box.copy(border = box.border ?: ink, borderWidth = box.borderWidth.takeIf { it > 0.0 } ?: FIRST_BORDER) else box.copy(border = null)))
            }
            if (box.outlined) {
                val border = box.border ?: ink
                MenuRow(stringResource(R.string.caption_colour)) {
                    ColorPickerDot(border, custom = true, onPick = { set(copy(box = box.copy(border = it))) }, dismissOnPick = false) { d, p ->
                        ColorPickerPopup(border, recents, d, p)
                    }
                }
                MenuSlider(
                    stringResource(R.string.caption_thickness), box.borderWidth.toFloat(), 0.5f..BoxStyle.MAX_BORDER.toFloat(),
                    step = 0.5f, child = true, onChangeFinished = settle,
                ) { drag(copy(box = box.copy(borderWidth = it.toDouble()))) }
            }
        }
        add {
            MenuSwitch(stringResource(R.string.box_corners), box.corner > 0.0) { on -> set(copy(box = box.copy(corner = if (on) FIRST_CORNER else 0.0))) }
            if (box.corner > 0.0) {
                MenuSlider(
                    stringResource(R.string.box_corners), box.corner.toFloat(), 1f..BoxStyle.MAX_CORNER.toFloat(),
                    step = 1f, child = true, onChangeFinished = settle,
                ) { drag(copy(box = box.copy(corner = it.toDouble()))) }
            }
        }
    }
    val size = counts + listOf<MenuGroup> {
        MenuSlider(
            stringResource(R.string.caption_thickness), width.toFloat(), MIN_WIDTH..MAX_WIDTH,
            step = ToolConversions.WIDTH_STEP.toFloat(), onChangeFinished = settle,
        ) { drag(copy(width = it.toDouble())) }
    }
    val effects = listOf<MenuGroup>(
        {
            MenuSwitch(stringResource(R.string.caption_dashed), lines.dashed) { set(copy(lines = lines.copy(dashed = it))) }
            if (lines.dashed) {
                MenuSlider(stringResource(R.string.caption_dash), lines.dashLength.toFloat(), 2f..40f, child = true, onChangeFinished = settle) {
                    drag(copy(lines = lines.copy(dashLength = it.toDouble())))
                }
                MenuSlider(stringResource(R.string.caption_gap), lines.dashGap.toFloat(), 2f..40f, child = true, onChangeFinished = settle) {
                    drag(copy(lines = lines.copy(dashGap = it.toDouble())))
                }
            }
        },
        {
            MenuSwitch(stringResource(R.string.caption_neon), lines.neon) { set(copy(lines = lines.copy(neon = it))) }
            if (lines.neon) {
                MenuSlider(
                    stringResource(R.string.caption_glow_intensity), ToolConversions.neonStrengthToIntensity(lines.neonStrength).toFloat(), 0f..100f,
                    child = true, onChangeFinished = settle,
                ) { drag(copy(lines = lines.copy(neonStrength = ToolConversions.intensityToNeonStrength(it.toDouble())))) }
            }
        },
    )
    return listOf(frame, size, effects)
}

/** A table's column or row count, stepped. */
internal fun tableCount(label: Int, n: Int, onChange: (Int) -> Unit): MenuGroup = {
    MenuRow(stringResource(label)) {
        MenuStepper(
            "$n", stringResource(R.string.count_fewer), stringResource(R.string.count_more), Modifier.width(116.dp),
            canLess = n > 1, canMore = n < TableItem.MAX_LINES,
            onLess = { onChange(n - 1) }, onMore = { onChange(n + 1) },
        )
    }
}
