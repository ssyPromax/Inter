package com.inter.calc.math

import kotlin.math.abs

data class CurvePoint(val x: Double, val y: Double)

/**
 * 高精度自适应采样器：
 * 在定义域边界（如 arth(x) 在 x=±1、ln(x) 在 x=0、arch(x) 在 x=1）与陡峭渐近线附近
 * 自动进行递归二分加密采样，确保曲线平滑延伸至画布边缘而不被提前截断。
 */
object NumericIntegrator {

    fun sampleFunction(expr: Expr, xMin: Double, xMax: Double, baseSteps: Int = 520): List<CurvePoint> {
        val evalFn: (Double) -> Double = { x ->
            val y = expr.eval(x)
            if (y.isFinite()) y else Double.NaN
        }
        return sampleAdaptive(evalFn, xMin, xMax, baseSteps)
    }

    fun sampleDerivative(
        original: Expr,
        symbolicDerivative: Expr?,
        xMin: Double,
        xMax: Double,
        baseSteps: Int = 520,
    ): List<CurvePoint> {
        val h = 1e-5
        val evalFn: (Double) -> Double = { x ->
            val symY = symbolicDerivative?.eval(x)
            if (symY != null && symY.isFinite()) {
                symY
            } else {
                val f1 = original.eval(x + h)
                val f2 = original.eval(x - h)
                if (f1.isFinite() && f2.isFinite()) (f1 - f2) / (2.0 * h) else Double.NaN
            }
        }
        return sampleAdaptive(evalFn, xMin, xMax, baseSteps)
    }

    fun sampleIntegral(
        original: Expr,
        symbolicIntegral: Expr?,
        xMin: Double,
        xMax: Double,
        baseSteps: Int = 520,
    ): List<CurvePoint> {
        if (symbolicIntegral != null) {
            val symPts = sampleFunction(symbolicIntegral, xMin, xMax, baseSteps)
            if (symPts.any { it.y.isFinite() }) return symPts
        }

        // 无初等原函数时：高密度辛普森累计数值积分
        val steps = baseSteps.coerceAtLeast(520)
        val xs = DoubleArray(steps + 1) { i -> xMin + i * (xMax - xMin) / steps }
        val ys = DoubleArray(steps + 1) { Double.NaN }
        val anchorIdx = chooseAnchorIndex(original, xs)
        if (anchorIdx < 0) return xs.map { CurvePoint(it, Double.NaN) }

        ys[anchorIdx] = 0.0
        var acc = 0.0
        for (i in anchorIdx until steps) {
            val seg = simpsonSegment(original, xs[i], xs[i + 1])
            if (seg.isFinite()) {
                acc += seg
                ys[i + 1] = acc
            } else {
                acc = 0.0
                ys[i + 1] = Double.NaN
            }
        }
        acc = 0.0
        for (i in anchorIdx downTo 1) {
            val seg = simpsonSegment(original, xs[i], xs[i - 1])
            if (seg.isFinite()) {
                acc += seg
                ys[i - 1] = acc
            } else {
                acc = 0.0
                ys[i - 1] = Double.NaN
            }
        }
        return List(steps + 1) { i -> CurvePoint(xs[i], ys[i]) }
    }

    private fun sampleAdaptive(
        evalFn: (Double) -> Double,
        xMin: Double,
        xMax: Double,
        baseSteps: Int,
    ): List<CurvePoint> {
        val out = ArrayList<CurvePoint>(baseSteps * 2)
        val dx = (xMax - xMin) / baseSteps
        val ySpanApprox = (xMax - xMin) * 0.85

        var prevX = xMin
        var prevY = evalFn(prevX)
        out.add(CurvePoint(prevX, prevY))

        for (i in 1..baseSteps) {
            val curX = xMin + i * dx
            val curY = evalFn(curX)
            subdivideIfNeeded(evalFn, prevX, prevY, curX, curY, ySpanApprox, depth = 0, out = out)
            out.add(CurvePoint(curX, curY))
            prevX = curX
            prevY = curY
        }
        return out
    }

    private fun subdivideIfNeeded(
        evalFn: (Double) -> Double,
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        ySpanApprox: Double,
        depth: Int,
        out: MutableList<CurvePoint>,
    ) {
        if (depth >= 8) return
        val f1 = y1.isFinite()
        val f2 = y2.isFinite()
        if (!f1 && !f2) return

        // 1) 一侧有定义、另一侧无定义（例如 arth(x) 逼近 ±1，ln(x) 逼近 0）-> 二分逼近边界
        val isBoundary = f1 != f2
        // 2) 两侧都有定义，但斜率/纵坐标跳变剧烈且在可见尺度附近 -> 加密平滑曲线
        val isSteep = f1 && f2 &&
            (abs(y1) < ySpanApprox * 4.0 || abs(y2) < ySpanApprox * 4.0) &&
            abs(y2 - y1) > ySpanApprox * 0.08

        if (!isBoundary && !isSteep) return
        if (abs(x2 - x1) < 1e-6) return

        val midX = 0.5 * (x1 + x2)
        val midY = evalFn(midX)
        subdivideIfNeeded(evalFn, x1, y1, midX, midY, ySpanApprox, depth + 1, out)
        out.add(CurvePoint(midX, midY))
        subdivideIfNeeded(evalFn, midX, midY, x2, y2, ySpanApprox, depth + 1, out)
    }

    private fun chooseAnchorIndex(expr: Expr, xs: DoubleArray): Int {
        var bestIdx = -1
        var bestDist = Double.MAX_VALUE
        for (i in xs.indices) {
            val v = expr.eval(xs[i])
            if (v.isFinite() && abs(v) < 1e4) {
                val dist = abs(xs[i])
                if (dist < bestDist) {
                    bestDist = dist
                    bestIdx = i
                }
            }
        }
        return bestIdx
    }

    private fun simpsonSegment(expr: Expr, a: Double, b: Double): Double {
        val m = 0.5 * (a + b)
        val fa = expr.eval(a)
        val fm = expr.eval(m)
        val fb = expr.eval(b)
        if (!fa.isFinite() || !fm.isFinite() || !fb.isFinite()) return Double.NaN
        if (abs(fa) > 1e5 || abs(fm) > 1e5 || abs(fb) > 1e5) return Double.NaN
        return (b - a) / 6.0 * (fa + 4.0 * fm + fb)
    }
}
