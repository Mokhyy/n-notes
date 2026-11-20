package com.xnotes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xnotes.R
import com.xnotes.core.tools.PencilcaseItem
import com.xnotes.core.tools.Tool
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/** Floating bar of saved tools: tap arms, plus saves the armed tool, bin toggles removing.
 *  Long-press drags it; let go near the top-left corner to go home, near a side edge to dock upright. */
@Composable
fun BoxScope.Pencilcase(
    items: List<PencilcaseItem>,
    canSave: Boolean,
    onSave: () -> Unit,
    onPick: (PencilcaseItem) -> Unit,
    onRemove: (PencilcaseItem) -> Unit,
) {
    val palette = LocalPalette.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var removeMode by remember { mutableStateOf(false) }
    var dock by remember { mutableStateOf(Dock.NONE) }
    var vertical by remember { mutableStateOf(false) }
    val removing = removeMode && items.isNotEmpty()

    fun shown(at: Offset = offset): Offset {
        val maxX = (area.width - size.width).coerceAtLeast(0).toFloat()
        val maxY = (area.height - size.height).coerceAtLeast(0).toFloat()
        val x = when (dock) {
            Dock.LEFT -> 0f
            Dock.RIGHT -> maxX
            Dock.NONE -> at.x.coerceIn(0f, maxX)
        }
        return Offset(x, at.y.coerceIn(0f, maxY))
    }

    Box(Modifier.matchParentSize().padding(LocalToolbarCover.current).padding(12.dp).onSizeChanged { area = it }) {
        Surface(
            Modifier
                .align(Alignment.TopStart)
                .offset { shown().round() }
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    val snap = 48.dp.toPx()
                    fun settle() {
                        when {
                            offset.getDistance() < snap -> { offset = Offset.Zero; vertical = false }
                            offset.x < snap -> { dock = Dock.LEFT; vertical = true }
                            offset.x + size.width > area.width - snap -> { dock = Dock.RIGHT; vertical = true }
                            else -> vertical = false
                        }
                    }
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset = shown(); dock = Dock.NONE },
                        onDragEnd = ::settle,
                        onDragCancel = ::settle,
                    ) { change, drag ->
                        change.consume()
                        offset = shown(offset + drag)
                    }
                },
            shape = MenuDefaults.shape,
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
            border = BorderStroke(1.dp, palette.border.toComposeColor()),
        ) {
            val buttons: @Composable () -> Unit = {
                for (item in items) {
                    val label = stringResource(item.tool.labelRes)
                    CaseButton(
                        if (removing) XnotesIcons.close else pencilcaseIcon(item.tool),
                        if (removing) stringResource(R.string.pencilcase_remove, label) else label,
                        tint = if (removing || item.tool == Tool.ERASER) palette.text.toComposeColor() else item.shownColor.toComposeColor(),
                    ) {
                        if (!removing) onPick(item)
                        else { onRemove(item); if (items.size == 1) removeMode = false }
                    }
                }
                if (!removing) {
                    CaseButton(
                        XnotesIcons.plus,
                        stringResource(R.string.pencilcase_save),
                        tint = (if (canSave) palette.textDim else palette.border).toComposeColor(),
                        enabled = canSave,
                        onClick = onSave,
                    )
                }
                if (items.isNotEmpty()) {
                    CaseButton(
                        XnotesIcons.trash,
                        stringResource(if (removing) R.string.done else R.string.pencilcase_remove_items),
                        tint = (if (removing) palette.accent else palette.textDim).toComposeColor(),
                    ) { removeMode = !removing }
                }
            }
            val scroll = rememberScrollState()
            if (vertical) {
                Column(Modifier.padding(vertical = 4.dp).verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) { buttons() }
            } else {
                Row(Modifier.padding(horizontal = 4.dp).horizontalScroll(scroll), verticalAlignment = Alignment.CenterVertically) { buttons() }
            }
        }
    }
}

private enum class Dock { NONE, LEFT, RIGHT }

@Composable
private fun CaseButton(
    icon: ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier.clip(MaterialTheme.shapes.extraSmall).clickable(enabled = enabled, onClick = onClick).padding(8.dp).size(20.dp),
    )
}

@Composable
private fun pencilcaseIcon(tool: Tool): ImageVector = when (tool) {
    Tool.ERASER -> XnotesIcons.eraser
    Tool.TAPE -> XnotesIcons.tape
    Tool.SHAPE -> XnotesIcons.shape
    else -> ImageVector.vectorResource(
        when (tool) {
            Tool.DASHED -> R.drawable.ic_stroke_dashed
            Tool.CALLIGRAPHY -> R.drawable.ic_stroke_calligraphy
            Tool.SPEED -> R.drawable.ic_stroke_speed
            Tool.TAPER -> R.drawable.ic_stroke_taper
            Tool.HIGHLIGHTER -> R.drawable.ic_stroke_highlighter
            else -> R.drawable.ic_stroke_regular
        },
    )
}
