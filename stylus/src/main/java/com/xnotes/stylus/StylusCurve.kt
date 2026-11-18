package com.xnotes.stylus

/**
 * A pen response curve: how a normalized input — pressure or tilt lay — becomes the strength the
 * ink uses. The user edits it as a smooth line through freely placed points, so it can start as the
 * straight `y = x` and gain any shape they need.
 *
 * Points are kept in increasing x order, at least two. Outside the first and last point the curve
 * holds that endpoint's value, which lets the visible line start or end inside the 0..1 box.
 */
data class StylusCurve(val points: List<Point> = DEFAULT) {

    data class Point(val x: Double, val y: Double)

    init {
        require(points.size >= 2) { "a response curve needs at least two points" }
        require(points.all { it.x in 0.0..1.0 && it.y in 0.0..1.0 }) { "curve points stay inside the box" }
        require(points.zipWithNext().all { (a, b) -> a.x < b.x }) { "curve points are ordered by x" }
    }

    /** The output for [x], interpolated through the points and clamped to `[0, 1]`. */
    fun valueAt(x: Double): Double {
        val first = points.first()
        val last = points.last()
        if (x <= first.x) return first.y.coerceIn(0.0, 1.0)
        if (x >= last.x) return last.y.coerceIn(0.0, 1.0)
        var i = 0
        while (i + 1 < points.size && points[i + 1].x < x) i++
        val p1 = points[i]
        val p2 = points[i + 1]
        val dx = p2.x - p1.x
        if (dx <= 1e-12) return p2.y.coerceIn(0.0, 1.0)
        val u = ((x - p1.x) / dx).coerceIn(0.0, 1.0)
        val m1 = slopeAt(i)
        val m2 = slopeAt(i + 1)
        val u2 = u * u
        val u3 = u2 * u
        val h00 = 2.0 * u3 - 3.0 * u2 + 1.0
        val h10 = u3 - 2.0 * u2 + u
        val h01 = -2.0 * u3 + 3.0 * u2
        val h11 = u3 - u2
        return (h00 * p1.y + h10 * dx * m1 + h01 * p2.y + h11 * dx * m2).coerceIn(0.0, 1.0)
    }

    private fun slopeAt(i: Int): Double = when {
        i == 0 -> (points[1].y - points[0].y) / (points[1].x - points[0].x)
        i == points.lastIndex ->
            (points[i].y - points[i - 1].y) / (points[i].x - points[i - 1].x)
        else ->
            (points[i + 1].y - points[i - 1].y) / (points[i + 1].x - points[i - 1].x)
    }

    /** Move point [index] to ([x], [y]), keeping it between its neighbours. */
    fun withPoint(index: Int, x: Double, y: Double): StylusCurve {
        if (index !in points.indices) return this
        val lo = if (index == 0) 0.0 else points[index - 1].x + MIN_GAP
        val hi = if (index == points.lastIndex) 1.0 else points[index + 1].x - MIN_GAP
        val nx = if (hi < lo) lo else x.coerceIn(lo, hi)
        val next = points.toMutableList()
        next[index] = Point(nx, y.coerceIn(0.0, 1.0))
        return StylusCurve(next)
    }

    /** Add a point at ([x], [y]); a point already sitting there is left alone. */
    fun addPoint(x: Double, y: Double): StylusCurve {
        val at = x.coerceIn(0.0, 1.0)
        if (points.any { kotlin.math.abs(it.x - at) < MIN_GAP }) return this
        val next = (points + Point(at, y.coerceIn(0.0, 1.0))).sortedBy { it.x }
        return if (next.zipWithNext().all { (a, b) -> a.x < b.x }) StylusCurve(next) else this
    }

    /** Remove point [index], unless that would leave fewer than two. */
    fun removePoint(index: Int): StylusCurve =
        if (index in points.indices && points.size > 2) StylusCurve(points.filterIndexed { i, _ -> i != index }) else this

    val isIdentity: Boolean get() = points == DEFAULT

    companion object {
        const val MIN_GAP = 0.001

        val DEFAULT = listOf(Point(0.0, 0.0), Point(1.0, 1.0))
        val IDENTITY = StylusCurve()
        val SOFT = StylusCurve(listOf(Point(0.0, 0.0), Point(0.5, 0.62), Point(1.0, 1.0)))
        val FIRM = StylusCurve(listOf(Point(0.0, 0.0), Point(0.5, 0.30), Point(1.0, 1.0)))
        val S_CURVE = StylusCurve(
            listOf(Point(0.0, 0.0), Point(0.28, 0.10), Point(0.72, 0.90), Point(1.0, 1.0)),
        )
    }
}
