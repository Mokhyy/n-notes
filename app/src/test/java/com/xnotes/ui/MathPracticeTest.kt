package com.xnotes.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathPracticeTest {
    @Test fun sameMathIgnoresBracesAndSpaces() {
        assertTrue(sameMath("x^2", "x^{2}"))
        assertTrue(sameMath("\\frac {a} {b}", "\\frac{a}{b}"))
        assertFalse(sameMath("x^3", "x^{2}"))
    }
}
