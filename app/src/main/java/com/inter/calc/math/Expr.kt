package com.inter.calc.math

import kotlin.math.*

sealed class Expr {
    data class Const(val value: Double) : Expr()
    data object Var : Expr()
    data class Neg(val inner: Expr) : Expr()
    data class Add(val left: Expr, val right: Expr) : Expr()
    data class Sub(val left: Expr, val right: Expr) : Expr()
    data class Mul(val left: Expr, val right: Expr) : Expr()
    data class Div(val left: Expr, val right: Expr) : Expr()
    data class Pow(val base: Expr, val exp: Expr) : Expr()
    data class Func(val name: String, val arg: Expr) : Expr()

    fun eval(x: Double): Double = when (this) {
        is Const -> value
        is Var -> x
        is Neg -> -inner.eval(x)
        is Add -> left.eval(x) + right.eval(x)
        is Sub -> left.eval(x) - right.eval(x)
        is Mul -> left.eval(x) * right.eval(x)
        is Div -> {
            val d = right.eval(x)
            if (abs(d) < 1e-12) Double.NaN else left.eval(x) / d
        }
        is Pow -> {
            val b = base.eval(x)
            val e = exp.eval(x)
            when {
                b.isNaN() || e.isNaN() -> Double.NaN
                b < 0.0 && abs(e - round(e)) > 1e-9 -> Double.NaN
                else -> b.pow(e)
            }
        }
        is Func -> {
            val v = arg.eval(x)
            if (v.isNaN()) Double.NaN else when (name) {
                "sin" -> sin(v)
                "cos" -> cos(v)
                "tan" -> if (abs(cos(v)) < 1e-9) Double.NaN else tan(v)
                "sec" -> if (abs(cos(v)) < 1e-9) Double.NaN else 1.0 / cos(v)
                "csc" -> if (abs(sin(v)) < 1e-9) Double.NaN else 1.0 / sin(v)
                "cot" -> if (abs(sin(v)) < 1e-9) Double.NaN else cos(v) / sin(v)
                "asin" -> if (v in -1.0..1.0) asin(v) else Double.NaN
                "acos" -> if (v in -1.0..1.0) acos(v) else Double.NaN
                "atan" -> atan(v)
                "sinh" -> sinh(v)
                "cosh" -> cosh(v)
                "tanh" -> tanh(v)
                "arsh" -> asinh(v)
                "arch" -> if (v >= 1.0) acosh(v) else Double.NaN
                "arth" -> if (v > -1.0 && v < 1.0) atanh(v) else Double.NaN
                "ln" -> if (v > 0.0) ln(v) else Double.NaN
                "log" -> if (v > 0.0) log10(v) else Double.NaN
                "exp" -> exp(v)
                "sqrt" -> if (v >= 0.0) sqrt(v) else Double.NaN
                "cbrt" -> cbrt(v)
                "abs" -> abs(v)
                else -> Double.NaN
            }
        }
    }

    fun containsVar(): Boolean = when (this) {
        is Const -> false
        is Var -> true
        is Neg -> inner.containsVar()
        is Add -> left.containsVar() || right.containsVar()
        is Sub -> left.containsVar() || right.containsVar()
        is Mul -> left.containsVar() || right.containsVar()
        is Div -> left.containsVar() || right.containsVar()
        is Pow -> base.containsVar() || exp.containsVar()
        is Func -> arg.containsVar()
    }

    fun simplify(): Expr {
        var cur = simplifyOnce()
        repeat(4) {
            val next = cur.simplifyOnce()
            if (next == cur) return cur
            cur = next
        }
        return cur
    }

    internal fun simplifyOnce(): Expr = when (this) {
        is Const -> if (value == -0.0) Const(0.0) else this
        is Var -> this
        is Neg -> {
            val s = inner.simplifyOnce()
            when (s) {
                is Const -> Const(if (s.value == 0.0) 0.0 else -s.value)
                is Neg -> s.inner
                is Mul -> if (s.left is Const) Mul(Const(-s.left.value), s.right).simplifyOnce() else Neg(s)
                else -> Neg(s)
            }
        }
        is Add -> {
            val l = left.simplifyOnce()
            val r = right.simplifyOnce()
            when {
                l is Const && r is Const -> Const(l.value + r.value)
                l is Const && l.value.isZero() -> r
                r is Const && r.value.isZero() -> l
                r is Neg -> Sub(l, r.inner).simplifyOnce()
                r is Const && r.value < 0 -> Sub(l, Const(-r.value))
                l == r -> Mul(Const(2.0), l).simplifyOnce()
                else -> Add(l, r)
            }
        }
        is Sub -> {
            val l = left.simplifyOnce()
            val r = right.simplifyOnce()
            when {
                l is Const && r is Const -> Const(l.value - r.value)
                r is Const && r.value.isZero() -> l
                l is Const && l.value.isZero() -> Neg(r).simplifyOnce()
                r is Neg -> Add(l, r.inner).simplifyOnce()
                r is Const && r.value < 0 -> Add(l, Const(-r.value))
                l == r -> Const(0.0)
                else -> Sub(l, r)
            }
        }
        is Mul -> {
            val l = left.simplifyOnce()
            val r = right.simplifyOnce()
            when {
                l is Const && r is Const -> Const(l.value * r.value)
                (l is Const && l.value.isZero()) || (r is Const && r.value.isZero()) -> Const(0.0)
                l is Const && l.value.isOne() -> r
                r is Const && r.value.isOne() -> l
                l is Const && l.value.isNegOne() -> Neg(r).simplifyOnce()
                r is Const && r.value.isNegOne() -> Neg(l).simplifyOnce()
                r is Const && l !is Const -> Mul(r, l).simplifyOnce()
                l is Const && r is Mul && r.left is Const -> Mul(Const(l.value * r.left.value), r.right).simplifyOnce()
                l is Neg && r is Neg -> Mul(l.inner, r.inner).simplifyOnce()
                l is Neg -> Neg(Mul(l.inner, r).simplifyOnce()).simplifyOnce()
                r is Neg -> Neg(Mul(l, r.inner).simplifyOnce()).simplifyOnce()
                l is Const && r is Div && r.left is Const -> Div(Const(l.value * r.left.value), r.right).simplifyOnce()
                l == Var && r == Var -> Pow(Var, Const(2.0))
                l is Pow && l.base == Var && r == Var -> Pow(Var, Add(l.exp, Const(1.0)).simplifyOnce())
                l == Var && r is Pow && r.base == Var -> Pow(Var, Add(r.exp, Const(1.0)).simplifyOnce())
                else -> Mul(l, r)
            }
        }
        is Div -> {
            val l = left.simplifyOnce()
            val r = right.simplifyOnce()
            when {
                l is Const && l.value.isZero() -> Const(0.0)
                r is Const && r.value.isOne() -> l
                r is Const && r.value.isNegOne() -> Neg(l).simplifyOnce()
                l is Const && r is Const && !r.value.isZero() -> {
                    val q = l.value / r.value
                    if (abs(q - round(q)) < 1e-9 || abs(q * 4 - round(q * 4)) < 1e-9) Const(q) else Div(l, r)
                }
                l == r -> Const(1.0)
                l is Neg && r is Neg -> Div(l.inner, r.inner).simplifyOnce()
                l is Neg -> Neg(Div(l.inner, r).simplifyOnce()).simplifyOnce()
                r is Neg -> Neg(Div(l, r.inner).simplifyOnce()).simplifyOnce()
                l is Mul && l.left is Const && r is Const && !r.value.isZero() -> {
                    val ratio = l.left.value / r.value
                    if (abs(ratio - round(ratio)) < 1e-9) Mul(Const(round(ratio)), l.right).simplifyOnce() else Div(l, r)
                }
                else -> Div(l, r)
            }
        }
        is Pow -> {
            val b = base.simplifyOnce()
            val e = exp.simplifyOnce()
            when {
                e is Const && e.value.isZero() -> Const(1.0)
                e is Const && e.value.isOne() -> b
                b is Const && b.value.isZero() -> Const(0.0)
                b is Const && b.value.isOne() -> Const(1.0)
                b is Const && e is Const -> {
                    val res = b.value.pow(e.value)
                    if (res.isFinite() && abs(res - round(res)) < 1e-9) Const(round(res)) else Pow(b, e)
                }
                b is Pow -> Pow(b.base, Mul(b.exp, e).simplifyOnce()).simplifyOnce()
                else -> Pow(b, e)
            }
        }
        is Func -> {
            val a = arg.simplifyOnce()
            when {
                name == "exp" && a is Const && a.value.isZero() -> Const(1.0)
                name == "ln" && a is Const && abs(a.value - E) < 1e-9 -> Const(1.0)
                name == "ln" && a is Const && a.value.isOne() -> Const(0.0)
                name == "ln" && a is Func && a.name == "exp" -> a.arg
                name == "exp" && a is Func && a.name == "ln" -> a.arg
                else -> Func(name, a)
            }
        }
    }

    fun format(): String = formatPrec(0)

    private fun formatPrec(parentPrec: Int): String = when (this) {
        is Const -> formatNumber(value)
        is Var -> "x"
        is Neg -> {
            val s = inner.formatPrec(3)
            val res = if (inner is Add || inner is Sub) "-($s)" else "-$s"
            if (parentPrec > 3) "($res)" else res
        }
        is Add -> {
            val res = "${left.formatPrec(1)} + ${right.formatPrec(1)}"
            if (parentPrec > 1) "($res)" else res
        }
        is Sub -> {
            val res = "${left.formatPrec(1)} - ${right.formatPrec(2)}"
            if (parentPrec > 1) "($res)" else res
        }
        is Mul -> {
            val lStr = left.formatPrec(2)
            val rStr = right.formatPrec(2)
            val omitStar = (left is Const && (right is Var || right is Func || right is Pow)) ||
                (left is Var && right is Func)
            val res = if (omitStar) "$lStr$rStr" else "$lStr·$rStr"
            if (parentPrec > 2) "($res)" else res
        }
        is Div -> {
            val lStr = left.formatPrec(3)
            val rStr = right.formatPrec(4)
            val res = "$lStr / $rStr"
            if (parentPrec > 2) "($res)" else res
        }
        is Pow -> {
            val bStr = if (base is Const && abs(base.value - E) < 1e-9) "e" else base.formatPrec(5)
            val eStr = exp.formatPrec(4)
            val res = "$bStr^$eStr"
            if (parentPrec > 4) "($res)" else res
        }
        is Func -> when (name) {
            "abs" -> "|${arg.formatPrec(0)}|"
            "exp" -> "e^(${arg.formatPrec(0)})"
            "sqrt" -> "√(${arg.formatPrec(0)})"
            else -> "$name(${arg.formatPrec(0)})"
        }
    }

    /**
     * Converts this expression AST into publication-grade LaTeX for KaTeX rendering.
     */
    fun toLatex(): String = toLatexPrec(0)

    private fun toLatexPrec(parentPrec: Int): String = when (this) {
        is Const -> formatLatexNumber(value)
        is Var -> "x"
        is Neg -> {
            val s = inner.toLatexPrec(3)
            val res = if (inner is Add || inner is Sub) "-\\left($s\\right)" else "-$s"
            if (parentPrec > 3) "\\left($res\\right)" else res
        }
        is Add -> {
            val res = "${left.toLatexPrec(1)} + ${right.toLatexPrec(1)}"
            if (parentPrec > 1) "\\left($res\\right)" else res
        }
        is Sub -> {
            val res = "${left.toLatexPrec(1)} - ${right.toLatexPrec(2)}"
            if (parentPrec > 1) "\\left($res\\right)" else res
        }
        is Mul -> {
            val lStr = left.toLatexPrec(2)
            val rStr = right.toLatexPrec(2)
            val omitDot = (left is Const && (right is Var || right is Func || right is Pow)) ||
                (left is Var && right is Func) ||
                (left is Pow && right is Func)
            val res = if (omitDot) "$lStr $rStr" else "$lStr \\cdot $rStr"
            if (parentPrec > 2) "\\left($res\\right)" else res
        }
        is Div -> "\\frac{${left.toLatexPrec(0)}}{${right.toLatexPrec(0)}}"
        is Pow -> {
            val bStr = if (base is Const && abs(base.value - E) < 1e-9) {
                "e"
            } else if (base is Var || base is Const) {
                base.toLatexPrec(5)
            } else {
                "\\left(${base.toLatexPrec(0)}\\right)"
            }
            val eStr = exp.toLatexPrec(0)
            val res = "{$bStr}^{$eStr}"
            if (parentPrec > 4) "\\left($res\\right)" else res
        }
        is Func -> {
            val a = arg.toLatexPrec(0)
            when (name) {
                "abs" -> "\\left|$a\\right|"
                "exp" -> "e^{$a}"
                "sqrt" -> "\\sqrt{$a}"
                "cbrt" -> "\\sqrt[3]{$a}"
                "sin", "cos", "tan", "sec", "csc", "cot",
                "sinh", "cosh", "tanh", "ln", "log" -> "\\$name\\left($a\\right)"
                "asin" -> "\\arcsin\\left($a\\right)"
                "acos" -> "\\arccos\\left($a\\right)"
                "atan" -> "\\arctan\\left($a\\right)"
                "arsh", "arch", "arth" -> "\\operatorname{$name}\\left($a\\right)"
                else -> "\\operatorname{$name}\\left($a\\right)"
            }
        }
    }

    companion object {
        fun formatNumber(v: Double): String {
            if (abs(v - PI) < 1e-9) return "π"
            if (abs(v - E) < 1e-9) return "e"
            val r = round(v)
            if (abs(v - r) < 1e-9) return r.toLong().toString()
            return "%.4f".format(v).trimEnd('0').trimEnd('.')
        }

        fun formatLatexNumber(v: Double): String {
            if (abs(v - PI) < 1e-9) return "\\pi"
            if (abs(v - E) < 1e-9) return "e"
            val r = round(v)
            if (abs(v - r) < 1e-9) return r.toLong().toString()
            return "%.4f".format(v).trimEnd('0').trimEnd('.')
        }
    }
}

internal fun Double.isZero(): Boolean = abs(this) < 1e-9
internal fun Double.isOne(): Boolean = abs(this - 1.0) < 1e-9
internal fun Double.isNegOne(): Boolean = abs(this + 1.0) < 1e-9
