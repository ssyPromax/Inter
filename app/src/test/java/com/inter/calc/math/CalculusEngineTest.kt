package com.inter.calc.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CalculusEngineTest {
    @Test
    fun testPolynomialDerivativeAndIntegral() {
        val f = ExprParser.parse("3x^2 + 2x - 1")
        val df = CalculusEngine.differentiate(f)
        val F = CalculusEngine.integrate(f)
        assertNotNull(F)
        assertEquals(14.0, df.eval(2.0), 1e-6)
        val x = 1.7
        val h = 1e-6
        assertEquals(f.eval(x), (F!!.eval(x + h) - F.eval(x - h)) / (2 * h), 1e-4)
    }

    @Test
    fun testIntegrationByPartsXSinX() {
        val f = ExprParser.parse("x*sin(x)")
        val F = CalculusEngine.integrate(f)
        assertNotNull(F)
        val x = 1.2
        val h = 1e-6
        assertEquals(f.eval(x), (F!!.eval(x + h) - F.eval(x - h)) / (2 * h), 1e-4)
    }

    @Test
    fun testMissingRightParenAutoClose() {
        val f1 = ExprParser.parse("sin(x")
        val f2 = ExprParser.parse("ln(x^2+1")
        val f3 = ExprParser.parse("(x+1)/(x-2")
        assertEquals(kotlin.math.sin(1.0), f1.eval(1.0), 1e-6)
        assertEquals(kotlin.math.ln(5.0), f2.eval(2.0), 1e-6)
        assertEquals(4.0, f3.eval(3.0), 1e-6)
    }

    @Test
    fun testInverseHyperbolicFunctions() {
        val h = 1e-6
        for ((exprStr, x) in listOf("arsh(x)" to 1.2, "arch(x)" to 2.0, "arth(x)" to 0.4)) {
            val f = ExprParser.parse(exprStr)
            val df = CalculusEngine.differentiate(f)
            val F = CalculusEngine.integrate(f)
            assertNotNull(F)
            val numDf = (f.eval(x + h) - f.eval(x - h)) / (2 * h)
            assertEquals(numDf, df.eval(x), 1e-4)
            val numF = (F!!.eval(x + h) - F.eval(x - h)) / (2 * h)
            assertEquals(f.eval(x), numF, 1e-4)
        }
    }

    @Test
    fun testLatexOutput() {
        val f = ExprParser.parse("1/(1+x^2)")
        assertTrue(f.toLatex().contains("\\frac"))
        val g = ExprParser.parse("arsh(x")
        assertTrue(g.toLatex().contains("\\operatorname{arsh}"))
    }

    @Test
    fun testNumericFallbackNonElementary() {
        val f = ExprParser.parse("e^(-x^2)")
        val pts = NumericIntegrator.sampleIntegral(f, null, -2.0, 2.0, 200)
        val atOne = pts.minBy { abs(it.x - 1.0) }
        assertEquals(0.7468, atOne.y, 1e-2)
    }
}
