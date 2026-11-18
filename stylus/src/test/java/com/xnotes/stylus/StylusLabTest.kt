package com.xnotes.stylus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusLabTest {

    @Test
    fun aFreshSessionStartsAtTheHoverStep() {
        val lab = StylusLab { 0L }
        assertNull(lab.snapshot().step)

        lab.start(null, listOf("test header"))

        val snap = lab.snapshot()
        assertTrue(snap.running)
        assertTrue(snap.step == StylusLab.Step.HOVER_ENTER)
    }

    @Test
    fun finishingTheButtonsPhaseCompletesTheSessionAndReportsIt() {
        val lab = StylusLab { 0L }
        lab.start(null, listOf("test header"))
        lab.completeCurrent()

        assertNull(lab.snapshot().step)
        val report = lab.report()
        assertTrue(report.contains("test header"))
        assertTrue(report.contains("Hover"))
        assertTrue(report.contains("Buttons"))
    }

    @Test
    fun stoppingEndsTheSession() {
        val lab = StylusLab { 0L }
        lab.start(null)
        lab.stop()

        assertFalse(lab.snapshot().running)
    }
}
