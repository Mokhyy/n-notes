package com.xnotes.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.xnotes.R
import com.xnotes.stylus.StylusDeviceInfo
import com.xnotes.stylus.StylusLab
import com.xnotes.stylus.StylusProfile
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/**
 * The on-device stylus test: a small drawing surface and the guided steps from [StylusLab]. The
 * page only forwards events and shows the snapshot; all step rules, formatting and the report are
 * in the `:stylus` module.
 */
@Composable
internal fun StylusLabPage(
    editor: Editor,
    profile: StylusProfile?,
    devices: List<StylusDeviceInfo>,
    onBack: () -> Unit,
) {
    val palette = LocalPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val lab = remember { StylusLab() }
    var snapshot by remember { mutableStateOf(lab.snapshot()) }
    var running by remember { mutableStateOf(false) }
    val surface = remember { LabSurface(context) }

    val info = remember(profile, devices) {
        profile?.let { p -> devices.firstOrNull { p.match.matches(it.device) } }
            ?: devices.firstOrNull { it.isDigitizer }
            ?: devices.firstOrNull()
    }

    fun refresh() {
        snapshot = lab.snapshot()
    }

    fun start() {
        lab.start(info, labHeader(context, profile))
        running = true
        refresh()
        surface.requestFocus()
    }

    fun close() {
        lab.stop()
        editor.stylusKeyTap = null
        running = false
        onBack()
    }

    DisposableEffect(running) {
        editor.stylusKeyTap = if (running) {
            { e ->
                val consumed = lab.onKey(e)
                if (consumed) refresh()
                consumed
            }
        } else {
            null
        }
        onDispose { editor.stylusKeyTap = null }
    }

    LaunchedEffect(running) {
        if (running) surface.requestFocus()
    }

    TextButton(onClick = ::close) { Text("‹ " + stringResource(R.string.pref_stylus_back), fontSize = 13.sp) }
    Text(
        stringResource(R.string.pref_stylus_lab),
        color = palette.text.toComposeColor(),
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    )
    Text(
        profile?.name ?: info?.device?.name ?: stringResource(R.string.pref_stylus_lab_no_pen),
        color = palette.textDim.toComposeColor(),
        fontSize = 12.sp,
    )
    info?.let {
        Text(it.summary(), color = palette.textDim.toComposeColor(), fontSize = 11.sp)
    }

    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { if (running) { lab.stop(); running = false; refresh() } else start() }) {
            Text(
                stringResource(if (running) R.string.pref_stylus_lab_stop else R.string.pref_stylus_lab_start),
                fontSize = 13.sp,
            )
        }
        if (snapshot.step == StylusLab.Step.BUTTONS) {
            TextButton(onClick = { lab.completeCurrent(); refresh() }) {
                Text(stringResource(R.string.pref_stylus_lab_done), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { copyReport(context, lab.report()) }) {
            Text(stringResource(R.string.pref_stylus_lab_copy), fontSize = 13.sp)
        }
        TextButton(onClick = { shareReport(context, lab.report()) }) {
            Text(stringResource(R.string.pref_stylus_lab_share), fontSize = 13.sp)
        }
    }

    AndroidView(
        factory = { surface },
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(top = 4.dp)
            .border(1.dp, palette.border.toComposeColor()),
        update = { view ->
            view.onPenMotion = { e, hover ->
                if (running) {
                    if (hover) lab.onHover(e) else lab.onTouch(e)
                    refresh()
                }
                running
            }
            view.onPenGeneric = { e ->
                if (running) {
                    lab.onGenericMotion(e)
                    refresh()
                }
                running
            }
            if (running) view.requestFocus()        },
    )

    Spacer(Modifier.height(8.dp))
    snapshot.step?.let { step ->
        Text(step.title, color = palette.text.toComposeColor(), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(step.hint, color = palette.textDim.toComposeColor(), fontSize = 11.sp)
    }
    for (s in lab.steps) {
        val done = s in snapshot.completed
        val current = s == snapshot.step
        Text(
            (if (done) "[x] " else if (current) "[>] " else "[ ] ") + s.title,
            color = if (current) palette.accent.toComposeColor() else palette.textDim.toComposeColor(),
            fontSize = 11.sp,
        )
    }

    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.pref_stylus_lab_log), color = palette.text.toComposeColor(), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .background(palette.surface.toComposeColor())
            .border(1.dp, palette.border.toComposeColor())
            .verticalScroll(rememberScrollState())
            .padding(6.dp),
    ) {
        Text(
            snapshot.lines.takeLast(80).joinToString("\n").ifEmpty { stringResource(R.string.pref_stylus_lab_idle) },
            color = palette.textDim.toComposeColor(),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
    }
}

private fun labHeader(context: Context, profile: StylusProfile?): List<String> {
    val version = runCatching {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        pi.versionName.orEmpty()
    }.getOrDefault("")
    return listOf(
        "app=${context.packageName} $version",
        "android=${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
        "device=${Build.MANUFACTURER} ${Build.MODEL}",
        "profile=${profile?.name ?: "none"}",
    )
}

private fun copyReport(context: Context, report: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Stylus lab", report))
}

private fun shareReport(context: Context, report: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, report)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.pref_stylus_lab_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * A bare canvas that gives the lab raw stylus events. It consumes touches while a session runs so
 * the surrounding settings scroll cannot steal a vertical stroke.
 */
private class LabSurface(context: Context) : View(context) {

    var onPenMotion: ((MotionEvent, Boolean) -> Boolean)? = null
    var onPenGeneric: ((MotionEvent) -> Boolean)? = null

    private val grid = Paint().apply {
        color = Color.argb(28, 128, 128, 128)
        strokeWidth = 1f
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 128, 128, 128)
        textSize = 13f * resources.displayMetrics.density
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        return onPenMotion?.invoke(event, false) ?: super.onTouchEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val handled = onPenMotion?.invoke(event, true) ?: false
        return handled || super.onHoverEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean =
        onPenGeneric?.invoke(event) ?: super.onGenericMotionEvent(event)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val step = 24f * resources.displayMetrics.density
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), grid)
            x += step
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, grid)
            y += step
        }
        canvas.drawText("Test area", 14f * resources.displayMetrics.density, 22f * resources.displayMetrics.density, label)
    }
}
