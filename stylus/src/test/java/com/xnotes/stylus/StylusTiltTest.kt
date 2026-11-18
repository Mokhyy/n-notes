package com.xnotes.stylus

import org.junit.Assert.assertEquals
import org.junit.Test

class StylusTiltTest {

    @Test fun uprightAndAbsentAxesAreTheSame() {
        assertEquals(0.0, StylusTilt.normalized(0.0), 1e-12)
        assertEquals(0.0, StylusTilt.normalized(-0.4), 1e-12) // a bad/negative reading never tilts
    }

    @Test fun halfAPerpendicularIsHalfLaid() {
        assertEquals(0.5, StylusTilt.normalized(Math.PI / 4.0), 1e-12)
        assertEquals(1.0, StylusTilt.normalized(Math.PI / 2.0), 1e-12)
        assertEquals(1.0, StylusTilt.normalized(3.0), 1e-12) // past flat: clamped
    }

    @Test fun widthIsIndependentOfPressureAndClamped() {
        assertEquals(1.0, StylusTilt.widthFactor(0.0, 1.0), 1e-12)
        assertEquals(1.0, StylusTilt.widthFactor(1.0, 0.0), 1e-12)
        assertEquals(2.0, StylusTilt.widthFactor(1.0, 1.0), 1e-12)
        assertEquals(1.5, StylusTilt.widthFactor(0.5, 1.0), 1e-12)
        assertEquals(2.0, StylusTilt.widthFactor(4.0, 9.0), 1e-12) // both inputs clamped
    }

    @Test fun bakedWidthKeepsTheExistingFactor() {
        assertEquals(0.8, StylusTilt.bakedWidthFactor(0.0, 1.0, 0.8), 1e-12) // upright keeps its base
        assertEquals(1.2, StylusTilt.bakedWidthFactor(1.0, 1.0, 0.6), 1e-12)
        assertEquals(1.5, StylusTilt.bakedWidthFactor(1.0, 0.5), 1e-12) // base defaults to 1
        assertEquals(0.9, StylusTilt.bakedWidthFactor(0.0, 0.5, 0.9), 1e-12)
    }
}
