package com.xnotes.platform

import android.content.Context
import com.xnotes.stylus.StylusCapabilities

/** Device input-capability probes, used to pick sensible first-run defaults. */
object DeviceCapabilities {

    /** Whether the device exposes a genuine stylus/pen digitizer; see [StylusCapabilities.hasStylus]. */
    fun hasStylus(context: Context): Boolean = StylusCapabilities.hasStylus(context)
}
