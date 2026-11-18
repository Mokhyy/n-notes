package com.xnotes.stylus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusCurveTest {

    @Test fun identityIsTheStraightLine() {
        val c = StylusCurve.IDENTITY
        assertEquals(2, c.points.size)
        for (i in 0..20) {
            val x = i / 20.0
            assertEquals(x, c.valueAt(x), 1e-9)
        }
        assertTrue(c.isIdentity)
    }

    @Test fun pointsAreHitExactlyAndOutsideHoldsTheEnd() {
        val c = StylusCurve(listOf(StylusCurve.Point(0.1, 0.2), StylusCurve.Point(0.4, 0.7), StylusCurve.Point(0.9, 0.8)))
        assertEquals(0.2, c.valueAt(0.1), 1e-12)
        assertEquals(0.7, c.valueAt(0.4), 1e-12)
        assertEquals(0.8, c.valueAt(0.9), 1e-12)
        assertEquals(0.2, c.valueAt(0.0), 1e-12)
        assertEquals(0.8, c.valueAt(1.0), 1e-12)
        assertFalse(c.isIdentity)
    }

    @Test fun pointsCanBeAddedMovedAndMergedAway() {
        val added = StylusCurve.IDENTITY.addPoint(0.5, 0.7)
        assertEquals(3, added.points.size)
        assertEquals(0.5, added.points[1].x, 1e-12)
        assertEquals(0.7, added.valueAt(0.5), 1e-12)

        // A point cannot cross its neighbours, but the endpoints may reach the box edges.
        val moved = added.withPoint(1, 0.95, 0.1)
        assertTrue(moved.points[1].x < moved.points[2].x)
        val end = added.withPoint(2, 0.2, 0.4)
        assertTrue(end.points[2].x > end.points[1].x)

        assertEquals(2, added.removePoint(1).points.size)
        assertEquals(2, StylusCurve.IDENTITY.removePoint(0).points.size) // never below two
    }

    @Test fun presetsShapeTheResponse() {
        assertTrue(StylusCurve.SOFT.valueAt(0.5) > 0.5)
        assertTrue(StylusCurve.FIRM.valueAt(0.5) < 0.5)
        assertTrue(StylusCurve.S_CURVE.valueAt(0.5) > 0.45 && StylusCurve.S_CURVE.valueAt(0.5) < 0.55)
    }
}
