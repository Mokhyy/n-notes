package com.xnotes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
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

/** Floating bar of saved tools: tap arms, the menu saves, reorders and removes.
 *  The handle drags it at once, the rest after a long press; it snaps home or docks upright on a side edge. */
@Composable
fun BoxScope.Pencilcase(
    items: List<PencilcaseItem>,
    canSave: Boolean,
    onSave: () -> Unit,
    onPick: (PencilcaseItem) -> Unit,
    onRemove: (PencilcaseItem) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
) {
    val palette = LocalPalette.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var dock by remember { mutableStateOf(Dock.NONE) }
    var vertical by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(Mode.USE) }
    val current = when {
        mode == Mode.REMOVE && items.isEmpty() -> Mode.USE
        mode == Mode.REORDER && items.size < 2 -> Mode.USE
        else -> mode
    }
    LaunchedEffect(current) { mode = current }

    // Reorder drag, all in root coordinates.
    var grabbed by remember { mutableStateOf<Int?>(null) }
    var grab by remember { mutableStateOf(Offset.Zero) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val bounds = remember { mutableStateMapOf<Int, Rect>() }
    val latestItems by rememberUpdatedState(items)
    val latestMove by rememberUpdatedState(onMove)

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

    fun startMove() { offset = shown(); dock = Dock.NONE }

    fun settle(snap: Float) {
        when {
            offset.getDistance() < snap -> { offset = Offset.Zero; vertical = false }
            offset.x < snap -> { dock = Dock.LEFT; vertical = true }
            offset.x + size.width > area.width - snap -> { dock = Dock.RIGHT; vertical = true }
            else -> vertical = false
        }
    }

    /** Where the dragged item lands in the list without it. */
    fun target(from: Int): Int = latestItems.indices.count { i ->
        val b = bounds[i] ?: return@count false
        i != from && (if (vertical) b.center.y < finger.y else b.center.x < finger.x)
    }

    val caretAt = grabbed?.let { g -> target(g).let { if (it < g) it else it + 1 } }

    Box(
        Modifier.matchParentSize().padding(LocalToolbarCover.current).padding(12.dp)
            .onSizeChanged { area = it }
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        Surface(
            Modifier
                .align(Alignment.TopStart)
                .offset { shown().round() }
                .onSizeChanged { size = it }
                .pointerInput(current == Mode.REORDER) {
                    if (current == Mode.REORDER) return@pointerInput // a long press there picks up an item instead
                    val snap = 48.dp.toPx()
                    detectDragGesturesAfterLongPress(
                        onDragStart = { startMove() },
                        onDragEnd = { settle(snap) },
                        onDragCancel = { settle(snap) },
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
            val handle: @Composable () -> Unit = {
                Icon(
                    XnotesIcons.grip,
                    contentDescription = stringResource(R.string.pencilcase_move),
                    tint = palette.textDim.toComposeColor(),
                    modifier = Modifier
                        .pointerInput(Unit) {
                            val snap = 48.dp.toPx()
                            detectDragGestures(
                                onDragStart = { startMove() },
                                onDragEnd = { settle(snap) },
                                onDragCancel = { settle(snap) },
                            ) { change, drag ->
                                change.consume()
                                offset = shown(offset + drag)
                            }
                        }
                        .padding(8.dp)
                        .size(20.dp)
                        .rotate(if (vertical) 90f else 0f),
                )
            }
            val buttons: @Composable () -> Unit = {
                items.forEachIndexed { i, item ->
                    if (caretAt == i) Caret(vertical)
                    val label = stringResource(item.tool.labelRes)
                    Box(
                        Modifier
                            .alpha(if (grabbed == i) 0.15f else 1f)
                            .onGloballyPositioned { bounds[i] = it.boundsInRoot() }
                            .then(
                                if (current != Mode.REORDER) Modifier
                                else Modifier.pointerInput(item, i) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { at ->
                                            grabbed = i
                                            grab = at
                                            finger = (bounds[i]?.topLeft ?: Offset.Zero) + at
                                        },
                                        onDragEnd = {
                                            val to = target(i)
                                            grabbed = null
                                            if (to != i) latestMove(i, to)
                                        },
                                        onDragCancel = { grabbed = null },
                                    ) { change, drag ->
                                        change.consume()
                                        finger += drag
                                    }
                                },
                            ),
                    ) {
                        CaseButton(
                            pencilcaseIcon(item.tool),
                            if (current == Mode.REMOVE) stringResource(R.string.pencilcase_remove, label) else label,
                            tint = (if (item.tool == Tool.ERASER) palette.text else item.shownColor).toComposeColor(),
                            enabled = current != Mode.REORDER,
                        ) { if (current == Mode.REMOVE) onRemove(item) else onPick(item) }
                        if (current == Mode.REMOVE) {
                            Icon(
                                XnotesIcons.close,
                                contentDescription = null,
                                tint = palette.text.toComposeColor(),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .size(12.dp)
                                    .background(palette.surface.toComposeColor(), CircleShape)
                                    .border(1.dp, palette.border.toComposeColor(), CircleShape)
                                    .padding(2.dp),
                            )
                        }
                    }
                }
                if (caretAt == items.size) Caret(vertical)
            }
            val menu: @Composable () -> Unit = {
                Box {
                    if (current == Mode.USE) {
                        CaseButton(XnotesIcons.more, stringResource(R.string.more), palette.textDim.toComposeColor()) { menuOpen = true }
                    } else {
                        CaseButton(XnotesIcons.check, stringResource(R.string.done), palette.accent.toComposeColor()) { mode = Mode.USE }
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        MenuRow(XnotesIcons.plus, stringResource(R.string.pencilcase_save), canSave) { menuOpen = false; onSave() }
                        MenuRow(XnotesIcons.sort, stringResource(R.string.pencilcase_reorder), items.size > 1) { menuOpen = false; mode = Mode.REORDER }
                        MenuRow(XnotesIcons.trash, stringResource(R.string.pencilcase_remove_items), items.isNotEmpty()) { menuOpen = false; mode = Mode.REMOVE }
                    }
                }
            }
            val scroll = rememberScrollState()
            if (vertical) {
                Column(Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    handle()
                    Rule(vertical)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) { buttons() }
                    Rule(vertical)
                    menu()
                }
            } else {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    handle()
                    Rule(vertical)
                    Row(Modifier.weight(1f, fill = false).horizontalScroll(scroll), verticalAlignment = Alignment.CenterVertically) { buttons() }
                    Rule(vertical)
                    menu()
                }
            }
        }
        grabbed?.let { g ->
            val item = items.getOrNull(g) ?: return@let
            Box(Modifier.align(Alignment.TopStart).offset { (finger - grab - origin).round() }) {
                ToolbarDragGhost(pencilcaseIcon(item.tool), stringResource(item.tool.labelRes))
            }
        }
    }
}

private enum class Dock { NONE, LEFT, RIGHT }

private enum class Mode { USE, REMOVE, REORDER }

@Composable
private fun Rule(vertical: Boolean) {
    Box(
        Modifier
            .padding(4.dp)
            .then(if (vertical) Modifier.height(1.dp).width(20.dp) else Modifier.width(1.dp).height(20.dp))
            .background(LocalPalette.current.border.toComposeColor()),
    )
}

@Composable
private fun Caret(vertical: Boolean) {
    Box(
        Modifier
            .then(if (vertical) Modifier.height(2.dp).width(28.dp) else Modifier.width(2.dp).height(28.dp))
            .background(LocalPalette.current.accent.toComposeColor()),
    )
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        enabled = enabled,
        onClick = onClick,
    )
}

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
