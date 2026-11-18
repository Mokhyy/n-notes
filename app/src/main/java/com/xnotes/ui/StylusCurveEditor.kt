package com.xnotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.stylus.StylusCurve
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlin.math.hypot

/**
 * The pen response editor: a smooth line through points the user places. Tap the line to add a
 * point, drag a point to move it in both axes, and drag it onto a neighbour to merge it away.
 *
 * The x axis is the normalized pen input (pressure or tilt lay) and the y axis is the strength the
 * ink uses, so the default two-point curve is exactly `y = x`.
 */
@Composable
internal fun StylusCurveEditor(curve: StylusCurve, inputLabel: String, onChange: (StylusCurve) -> Unit) {
    val palette = LocalPalette.current
    val lineColor = palette.accent.toComposeColor()
    val gridColor = palette.border.toComposeColor()
    val pointColor = palette.text.toComposeColor()
    val plateColor = palette.surfaceHi.toComposeColor()
    val labelColor = palette.textDim.toComposeColor()
    var points by remember(curve) { mutableStateOf(curve.points) }
    var dragging by remember { mutableStateOf(-1) }
    // Dragging only touches the local state. Applying preferences touches the canvas and writes
    // every setting to disk, so it is done once when the gesture ends, not on each motion event.
    var dirty by remember { mutableStateOf(false) }
    LaunchedEffect(curve) { points = curve.points }

    fun flush() {
        if (!dirty) return
        dirty = false
        onChange(StylusCurve(points))
    }

    fun commit(next: List<StylusCurve.Point>) {
        points = next
        dirty = false
        onChange(StylusCurve(next))
    }

    fun offsetOf(p: StylusCurve.Point, w: Float, h: Float): Offset =
        Offset(p.x.toFloat() * w, (1f - p.y.toFloat()) * h)

    fun distanceTo(p: StylusCurve.Point, at: Offset, w: Float, h: Float): Float {
        val o = offsetOf(p, w, h)
        return hypot((at.x - o.x).toDouble(), (at.y - o.y).toDouble()).toFloat()
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(18.dp).height(160.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.curve_axis_strength),
                color = labelColor,
                fontSize = 10.sp,
                modifier = Modifier.rotate(-90f),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(plateColor, RoundedCornerShape(10.dp))
                    .border(1.dp, gridColor, RoundedCornerShape(10.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (w <= 0f || h <= 0f) return@detectTapGestures
                            val near = 18.dp.toPx()
                            if (points.any { distanceTo(it, pos, w, h) < near }) return@detectTapGestures
                            val x = (pos.x / w).coerceIn(0f, 1f).toDouble()
                            val next = StylusCurve(points).addPoint(x, StylusCurve(points).valueAt(x))
                            if (next.points.size > points.size) commit(next.points)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                dirty = false
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val near = 22.dp.toPx()
                                dragging = points.indices
                                    .map { it to distanceTo(points[it], pos, w, h) }
                                    .filter { it.second < near }
                                    .minByOrNull { it.second }?.first ?: -1
                            },
                            onDragEnd = { dragging = -1; flush() },
                            onDragCancel = { dragging = -1; flush() },
                            onDrag = { change, _ ->
                                val i = dragging
                                if (i < 0 || i !in points.indices) return@detectDragGestures
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                if (w <= 0f || h <= 0f) return@detectDragGestures
                                // Dragging onto a side point merges this one away.
                                val merge = 16.dp.toPx()
                                val onto = (i - 1)..(i + 1)
                                if (points.size > 2 && onto.any { it != i && it in points.indices && distanceTo(points[it], change.position, w, h) < merge }) {
                                    points = StylusCurve(points).removePoint(i).points
                                    dirty = true
                                    dragging = -1
                                } else {
                                    val x = (change.position.x / w).coerceIn(0f, 1f).toDouble()
                                    val y = (1f - change.position.y / h).coerceIn(0f, 1f).toDouble()
                                    points = StylusCurve(points).withPoint(i, x, y).points
                                    dirty = true
                                }
                                change.consume()
                            },
                        )
                    },
            ) {
                val w = size.width
                val h = size.height
                for (i in 1 until 4) {
                    drawLine(gridColor, Offset(w * i / 4f, 0f), Offset(w * i / 4f, h), 1f)
                    drawLine(gridColor, Offset(0f, h * i / 4f), Offset(w, h * i / 4f), 1f)
                }
                val shape = Path()
                val steps = 96
                val curveNow = StylusCurve(points)
                for (i in 0..steps) {
                    val x = i / steps.toFloat()
                    val y = curveNow.valueAt(x.toDouble()).toFloat()
                    val at = Offset(w * x, h * (1f - y))
                    if (i == 0) shape.moveTo(at.x, at.y) else shape.lineTo(at.x, at.y)
                }
                drawPath(shape, lineColor, style = Stroke(width = 2.5.dp.toPx()))
                points.forEachIndexed { i, value ->
                    val at = offsetOf(value, w, h)
                    drawCircle(plateColor, 8.dp.toPx(), at)
                    drawCircle(if (i == dragging) lineColor else pointColor, if (i == dragging) 6.dp.toPx() else 5.dp.toPx(), at)
                }
            }
            Text(
                inputLabel,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = labelColor,
                fontSize = 10.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { commit(StylusCurve.DEFAULT) }) { Text(stringResource(R.string.curve_linear)) }
                TextButton(onClick = { dragging = -1; commit(StylusCurve.SOFT.points) }) { Text(stringResource(R.string.curve_soft)) }
                TextButton(onClick = { dragging = -1; commit(StylusCurve.FIRM.points) }) { Text(stringResource(R.string.curve_firm)) }
                TextButton(onClick = { dragging = -1; commit(StylusCurve.S_CURVE.points) }) { Text(stringResource(R.string.curve_s_curve)) }
            }
        }
    }
}
