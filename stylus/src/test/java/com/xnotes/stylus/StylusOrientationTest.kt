package com.xnotes.stylus

import org.junit.Assert.assertEquals
import org.junit.Test

class StylusOrientationTest {

    @Test fun absentAndFullTurnsAgree() {
        assertEquals(0.0, StylusOrientation.normalized(0.0), 1e-12)
        assertEquals(0.0, StylusOrientation.normalized(Math.PI * 2.0), 1e-12) // a full turn is the start again
        assertEquals(0.5, StylusOrientation.normalized(Math.PI), 1e-12)
    }

    @Test fun negativeAndPastFullAnglesWrap() {
        assertEquals(0.75, StylusOrientation.normalized(-Math.PI / 2.0), 1e-12)
        assertEquals(0.25, StylusOrientation.normalized(Math.PI * 2.5), 1e-12)
    }

    @Test fun aFlatNibIsBroadAcrossTheStrokeAndThinAlongIt() {
        // Heading east: a nib edge pointing east is edge-on, pointing north crosses the stroke.
        assertEquals(0.08, StylusOrientation.widthFactor(0.0, 0.0, 1.0), 1e-12)
        assertEquals(2.0, StylusOrientation.widthFactor(0.25, 0.0, 1.0), 1e-12)
        assertEquals(1.0, StylusOrientation.widthFactor(0.25, 0.0, 0.0), 1e-12)
        assertEquals(0.08, StylusOrientation.widthFactor(0.5, Math.PI, 1.0), 1e-12) // edge-on again
    }

    @Test fun bakedFactorsFollowTheSameLaw() {
        val xs = doubleArrayOf(0.0, 10.0, 20.0)
        val ys = doubleArrayOf(0.0, 0.0, 0.0)
        val across = StylusOrientation.bakedWidthFactors(xs, ys, doubleArrayOf(0.25, 0.25, 0.25), 1.0)
        val along = StylusOrientation.bakedWidthFactors(xs, ys, doubleArrayOf(0.0, 0.0, 0.0), 1.0)
        assertEquals(2.0, across[1], 1e-12)
        assertEquals(0.08, along[1], 1e-12)
    }
}
