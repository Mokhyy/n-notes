package com.xnotes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.xnotes.R
import com.xnotes.core.geometry.Rect
import com.xnotes.core.model.Stroke
import com.xnotes.core.stroke.Sample
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolConfig
import com.xnotes.platform.MathOcr
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val EXERCISES = listOf(
    "x^{2}",
    "\\frac{a}{b}",
    "\\sqrt{x+1}",
    "a^{2}+b^{2}=c^{2}",
    "\\sum_{i=1}^{n}i",
)

private val TIPS = listOf(
    R.string.math_tip_fractions,
    R.string.math_tip_exponents,
    R.string.math_tip_spacing,
    R.string.math_tip_letters,
    R.string.math_tip_size,
)

/** Braces and spaces don't change what is written, so "x^2" and "x^{2}" count as the same answer. */
internal fun sameMath(a: String, b: String): Boolean {
    fun norm(s: String) = s.filterNot { it.isWhitespace() || it == '{' || it == '}' }
    return norm(a) == norm(b)
}

/** Writing tips plus a pad to practise a few equations against the math recognizer. */
@Composable
fun MathPractice(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val palette = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var index by remember { mutableIntStateOf(0) }
    val lines = remember { mutableStateListOf<SnapshotStateList<Offset>>() }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val target = EXERCISES[index]
    val latexConfig = LatexConfig(fontSize = 22.sp, theme = latexTheme())
    val outlineConfig = LatexConfig(fontSize = 64.sp, theme = LatexTheme.light(palette.text.toComposeColor().copy(alpha = 0.15f), Color.Transparent))

    fun clear() {
        lines.clear()
        result = null
        error = null
    }

    fun check() {
        val points = lines.flatten()
        if (points.isEmpty()) return
        val strokes = lines.map { line ->
            Stroke(Tool.PEN, ToolConfig(), line.map { Sample(it.x.toDouble(), it.y.toDouble(), 1.0) })
        }
        val left = points.minOf { it.x }.toDouble()
        val top = points.minOf { it.y }.toDouble()
        val bounds = Rect(left, top, points.maxOf { it.x } - left, points.maxOf { it.y } - top)
        checking = true
        error = null
        scope.launch {
            try {
                result = withContext(Dispatchers.Default) { MathOcr.recognize(context, strokes, bounds) }
            } catch (e: Throwable) {
                error = context.getString(R.string.ocr_math_failed, e.message)
            }
            checking = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(palette.bg.toComposeColor()).systemBarsPadding()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.math_practice_title), color = palette.text.toComposeColor(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onBack) { Text(stringResource(R.string.close), fontSize = 13.sp) }
                }
                for (tip in TIPS) Text(stringResource(tip), color = palette.textDim.toComposeColor(), fontSize = 13.sp, textAlign = TextAlign.Center)

                Text(
                    stringResource(R.string.math_practice_write, index + 1, EXERCISES.size),
                    color = palette.accent.toComposeColor(), fontSize = 13.sp,
                )
                Latex(latex = target, config = latexConfig)
                val ink = palette.text.toComposeColor()
                Box(
                    Modifier.widthIn(max = 640.dp).fillMaxWidth().height(220.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(palette.surface.toComposeColor())
                        .border(1.dp, palette.border.toComposeColor(), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                // A faint copy of the target to write over; only the user's strokes go to the recognizer.
                Latex(latex = target, config = outlineConfig)
                Canvas(
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                val line = mutableStateListOf(down.position)
                                lines += line
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    line += change.position
                                    change.consume()
                                }
                            }
                        },
                ) {
                    val width = 3.dp.toPx()
                    for (line in lines) {
                        if (line.size == 1) {
                            drawCircle(ink, width / 2, line[0])
                            continue
                        }
                        val path = Path().apply {
                            moveTo(line[0].x, line[0].y)
                            for (i in 1 until line.size) lineTo(line[i].x, line[i].y)
                        }
                        drawPath(path, ink, style = DrawStroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = ::clear, enabled = !checking) { Text(stringResource(R.string.math_practice_clear), fontSize = 13.sp) }
                    TextButton(onClick = ::check, enabled = !checking && lines.isNotEmpty()) { Text(stringResource(R.string.math_practice_check), fontSize = 13.sp) }
                    if (checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { index = (index + 1) % EXERCISES.size; clear() }) {
                        Text(stringResource(R.string.math_practice_next), fontSize = 13.sp)
                    }
                }
                error?.let { Text(it, color = palette.danger.toComposeColor(), fontSize = 13.sp) }
                result?.let { got ->
                    val ok = sameMath(got, target)
                    Text(
                        stringResource(if (ok) R.string.math_practice_correct else R.string.math_practice_wrong),
                        color = if (ok) palette.accent.toComposeColor() else palette.danger.toComposeColor(),
                        fontSize = 13.sp,
                    )
                    if (got.isBlank()) {
                        Text(stringResource(R.string.ocr_no_math), color = palette.textDim.toComposeColor(), fontSize = 13.sp)
                    } else {
                        Latex(latex = got, config = latexConfig)
                        Text(got, color = palette.textDim.toComposeColor(), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
