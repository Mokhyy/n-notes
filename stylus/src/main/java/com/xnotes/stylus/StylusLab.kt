package com.xnotes.stylus

import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import java.util.Locale

/**
 * A guided stylus test session. The user brings a pen near, moves it, taps, draws and presses its
 * buttons on an app-provided surface; every stylus event becomes one sanitised text line. The
 * report is what a maintainer needs to add a new pen preset, without screen recording or note data.
 *
 * The state machine, formatting and report live here; the app only forwards events and renders the
 * snapshot.
 */
class StylusLab(private val clock: () -> Long = { SystemClock.uptimeMillis() }) {

    enum class Step(val title: String, val hint: String) {
        HOVER_ENTER("Hover", "Bring the pen close to the test area without touching it."),
        HOVER_MOVE("Move while hovering", "Move the pen around above the test area."),
        HOVER_EXIT("Leave", "Lift the pen away until the hover ends."),
        TOUCH("Tap", "Touch the test area once with the pen tip."),
        DRAG("Draw", "Draw a short stroke with the pen tip."),
        BUTTONS("Buttons", "Press each pen button once, one at a time, then tap Done."),
    }

    data class Snapshot(
        val running: Boolean = false,
        val step: Step? = null,
        val completed: Set<Step> = emptySet(),
        val seenKeys: List<String> = emptyList(),
        val lines: List<String> = emptyList(),
    )

    val steps: List<Step> get() = Step.entries

    private val lines = ArrayDeque<String>()
    private val completed = linkedSetOf<Step>()
    private val seenKeys = linkedMapOf<Int, String>()
    private var running = false
    private var startedAt = 0L
    private var header = ""
    private var lastHoverLog = 0L
    private var lastMoveLog = 0L
    private var downX = 0f
    private var downY = 0f

    /** The first step not yet completed, or null when the whole session is done. */
    val current: Step? get() = steps.firstOrNull { it !in completed }

    fun start(deviceInfo: StylusDeviceInfo?, headerLines: List<String> = emptyList()) {
        lines.clear()
        completed.clear()
        seenKeys.clear()
        running = true
        startedAt = clock()
        lastHoverLog = 0L
        lastMoveLog = 0L
        header = buildHeader(deviceInfo, headerLines)
        add("session", "started")
    }

    fun stop() {
        if (running) add("session", "stopped")
        running = false
    }

    /** The user says they pressed every button, so the final phase can finish. */
    fun completeCurrent() {
        if (!running) return
        completed += steps
        add("step", "buttons done")
    }

    fun onHover(e: MotionEvent): Boolean {
        if (!running || !isPen(e)) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER -> {
                add("hover-enter", describe(e))
                advanceIf(Step.HOVER_ENTER)
            }
            MotionEvent.ACTION_HOVER_MOVE -> {
                val now = clock()
                if (now - lastHoverLog >= HOVER_LOG_MS) {
                    lastHoverLog = now
                    add("hover-move", describe(e))
                }
                advanceIf(Step.HOVER_MOVE)
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                add("hover-exit", describe(e))
                advanceIf(Step.HOVER_EXIT)
            }
        }
        return true
    }

    fun onTouch(e: MotionEvent): Boolean {
        if (!running || !isPen(e)) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                add("touch-down", describe(e))
                advanceIf(Step.TOUCH)
            }
            MotionEvent.ACTION_MOVE -> {
                val moved = kotlin.math.hypot((e.x - downX).toDouble(), (e.y - downY).toDouble())
                if (moved > DRAG_SLOP_PX) advanceIf(Step.DRAG)
                val now = clock()
                if (now - lastMoveLog >= MOVE_LOG_MS) {
                    lastMoveLog = now
                    add("touch-move", describe(e))
                }
            }
            MotionEvent.ACTION_UP -> add("touch-up", describe(e))
            MotionEvent.ACTION_CANCEL -> add("touch-cancel", describe(e))
        }
        return true
    }

    fun onGenericMotion(e: MotionEvent): Boolean {
        if (!running) return false
        add(
            "motion",
            describe(e) + " buttonState=0x${e.buttonState.toString(16)} actionButton=0x${e.actionButton.toString(16)}",
        )
        if (current == Step.BUTTONS && e.actionButton != 0) {
            seenKeys[e.actionButton] = "BUTTON_${e.actionButton}"
        }
        return true
    }

    /** Any key while the lab runs is captured; pen buttons arrive here as `KeyEvent`s. */
    fun onKey(e: KeyEvent): Boolean {
        if (!running) return false
        val kind = when (e.action) {
            KeyEvent.ACTION_DOWN -> "key-down"
            KeyEvent.ACTION_UP -> "key-up"
            else -> "key-${e.action}"
        }
        add(
            kind,
            "key=${KeyEvent.keyCodeToString(e.keyCode)} code=${e.keyCode} repeat=${e.repeatCount} " +
                deviceName(e.deviceId),
        )
        if (e.action == KeyEvent.ACTION_DOWN) {
            seenKeys[e.keyCode] = KeyEvent.keyCodeToString(e.keyCode)
        }
        return true
    }

    fun snapshot(): Snapshot = Snapshot(
        running = running,
        step = if (running) current else null,
        completed = completed.toSet(),
        seenKeys = seenKeys.map { "${it.value} (${it.key})" },
        lines = lines.toList(),
    )

    /** A plain-text report, including the order of steps and every observed event. */
    fun report(): String = buildString {
        if (header.isNotEmpty()) {
            appendLine(header)
            appendLine()
        }
        appendLine("Steps:")
        steps.forEach { appendLine((if (it in completed) "[x] " else "[ ] ") + it.title) }
        appendLine()
        if (seenKeys.isNotEmpty()) {
            appendLine("Buttons seen:")
            seenKeys.forEach { (code, name) -> appendLine("$name ($code)") }
            appendLine()
        }
        appendLine("Log:")
        lines.forEach { appendLine(it) }
    }

    private fun advanceIf(step: Step) {
        if (step in completed) return
        // A user who starts with the pen already near, or touches before hovering, should not get
        // stuck on an earlier step: performing a later one also closes the ones it presupposes.
        for (s in steps) {
            completed += s
            if (s == step) break
        }
        add("step", "done: ${step.title}")
    }

    private fun add(kind: String, body: String) {
        if (!running && kind != "session") return
        val t = clock() - startedAt
        lines.addLast(String.format(Locale.US, "[%6d ms] %-12s %s", t, kind, body))
        while (lines.size > MAX_LINES) lines.removeFirst()
    }

    private fun describe(e: MotionEvent): String = buildString {
        append("dev=").append(e.deviceId).append(' ').append(deviceName(e.deviceId))
        append(" tool=").append(toolName(e.getToolType(0)))
        append(" x=").append(fmt(e.x)).append(" y=").append(fmt(e.y))
        append(" pressure=").append(fmt(e.getPressure(0)))
        append(" tilt=").append(fmt(e.getAxisValue(MotionEvent.AXIS_TILT)))
        append(" orientation=").append(fmt(e.getAxisValue(MotionEvent.AXIS_ORIENTATION)))
        append(" distance=").append(fmt(e.getAxisValue(MotionEvent.AXIS_DISTANCE)))
        append(" buttons=0x").append(e.buttonState.toString(16))
    }

    private fun buildHeader(deviceInfo: StylusDeviceInfo?, headerLines: List<String>): String = buildString {
        appendLine("Stylus lab report")
        headerLines.forEach { appendLine(it) }
        if (deviceInfo != null) {
            val d = deviceInfo.device
            appendLine("pen: ${d.name}")
            appendLine("vendor=0x${d.vendorId.toString(16)} product=0x${d.productId.toString(16)} descriptor=${d.descriptor}")
            appendLine("source=${if (deviceInfo.isDigitizer) "digitizer" else "bluetooth/hid"}")
            appendLine(
                "axes: pressure=${if (deviceInfo.hasPressure) deviceInfo.pressureMax.toInt().toString() else "none"}" +
                    " tilt=${if (deviceInfo.hasTilt) "yes" else "no"}" +
                    " orientation=${if (deviceInfo.hasOrientation) "yes" else "no"}" +
                    " distance=${if (deviceInfo.hasDistance) if (deviceInfo.distanceContinuous) "continuous" else "binary" else "none"}" +
                    " buttons=${if (deviceInfo.hasButtons) "yes" else "no"}",
            )
        }
    }

    private fun isPen(e: MotionEvent): Boolean {
        val tool = e.getToolType(0)
        return tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER
    }

    private fun deviceName(id: Int): String = StylusDevice.of(id)?.name ?: "device $id"

    private fun toolName(tool: Int): String = when (tool) {
        MotionEvent.TOOL_TYPE_STYLUS -> "stylus"
        MotionEvent.TOOL_TYPE_ERASER -> "eraser"
        MotionEvent.TOOL_TYPE_FINGER -> "finger"
        MotionEvent.TOOL_TYPE_MOUSE -> "mouse"
        else -> "tool-$tool"
    }

    private fun fmt(v: Float): String = String.format(Locale.US, "%.2f", v)

    private companion object {
        const val MAX_LINES = 400
        const val HOVER_LOG_MS = 120L
        const val MOVE_LOG_MS = 80L
        const val DRAG_SLOP_PX = 16.0
    }
}
