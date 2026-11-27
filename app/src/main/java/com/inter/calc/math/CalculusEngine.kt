package com.inter.calc.math

import kotlin.math.E
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

object CalculusEngine {
    fun differentiate(expr: Expr): Expr = diffRaw(expr).simplify()

    private fun diffRaw(e: Expr): Expr = when (e) {
        is Expr.Const -> Expr.Const(0.0)
        is Expr.Var -> Expr.Const(1.0)
        is Expr.Neg -> Expr.Neg(diffRaw(e.inner))
        is Expr.Add -> Expr.Add(diffRaw(e.left), diffRaw(e.right))
        is Expr.Sub -> Expr.Sub(diffRaw(e.left), diffRaw(e.right))
        is Expr.Mul -> Expr.Add(Expr.Mul(diffRaw(e.left), e.right), Expr.Mul(e.left, diffRaw(e.right)))
        is Expr.Div -> Expr.Div(
            Expr.Sub(Expr.Mul(diffRaw(e.left), e.right), Expr.Mul(e.left, diffRaw(e.right))),
            Expr.Pow(e.right, Expr.Const(2.0))
        )
        is Expr.Pow -> {
            val u = e.base
            val v = e.exp
            when {
                !u.containsVar() && !v.containsVar() -> Expr.Const(0.0)
                !v.containsVar() -> Expr.Mul(Expr.Mul(v, Expr.Pow(u, Expr.Sub(v, Expr.Const(1.0)))), diffRaw(u))
                !u.containsVar() -> {
                    val lnBase = if (u is Expr.Const && abs(u.value - E) < 1e-9) Expr.Const(1.0) else Expr.Func("ln", u)
                    Expr.Mul(Expr.Mul(e, lnBase), diffRaw(v))
                }
                else -> Expr.Mul(e, Expr.Add(Expr.Mul(diffRaw(v), Expr.Func("ln", u)), Expr.Div(Expr.Mul(v, diffRaw(u)), u)))
            }
        }
        is Expr.Func -> {
            val u = e.arg
            val du = diffRaw(u)
            val outer: Expr = when (e.name) {
                "sin" -> Expr.Func("cos", u)
                "cos" -> Expr.Neg(Expr.Func("sin", u))
                "tan" -> Expr.Div(Expr.Const(1.0), Expr.Pow(Expr.Func("cos", u), Expr.Const(2.0)))
                "sec" -> Expr.Mul(Expr.Func("sec", u), Expr.Func("tan", u))
                "csc" -> Expr.Neg(Expr.Mul(Expr.Func("csc", u), Expr.Func("cot", u)))
                "cot" -> Expr.Neg(Expr.Div(Expr.Const(1.0), Expr.Pow(Expr.Func("sin", u), Expr.Const(2.0))))
                "asin" -> Expr.Div(Expr.Const(1.0), Expr.Func("sqrt", Expr.Sub(Expr.Const(1.0), Expr.Pow(u, Expr.Const(2.0)))))
                "acos" -> Expr.Neg(Expr.Div(Expr.Const(1.0), Expr.Func("sqrt", Expr.Sub(Expr.Const(1.0), Expr.Pow(u, Expr.Const(2.0))))))
                "atan" -> Expr.Div(Expr.Const(1.0), Expr.Add(Expr.Const(1.0), Expr.Pow(u, Expr.Const(2.0))))
                "sinh" -> Expr.Func("cosh", u)
                "cosh" -> Expr.Func("sinh", u)
                "tanh" -> Expr.Div(Expr.Const(1.0), Expr.Pow(Expr.Func("cosh", u), Expr.Const(2.0)))
                "arsh" -> Expr.Div(Expr.Const(1.0), Expr.Func("sqrt", Expr.Add(Expr.Pow(u, Expr.Const(2.0)), Expr.Const(1.0))))
                "arch" -> Expr.Div(Expr.Const(1.0), Expr.Func("sqrt", Expr.Sub(Expr.Pow(u, Expr.Const(2.0)), Expr.Const(1.0))))
                "arth" -> Expr.Div(Expr.Const(1.0), Expr.Sub(Expr.Const(1.0), Expr.Pow(u, Expr.Const(2.0))))
                "ln" -> Expr.Div(Expr.Const(1.0), u)
                "log" -> Expr.Div(Expr.Const(1.0), Expr.Mul(u, Expr.Const(ln(10.0))))
                "exp" -> Expr.Func("exp", u)
                "sqrt" -> Expr.Div(Expr.Const(1.0), Expr.Mul(Expr.Const(2.0), Expr.Func("sqrt", u)))
                "cbrt" -> Expr.Div(Expr.Const(1.0), Expr.Mul(Expr.Const(3.0), Expr.Pow(u, Expr.Div(Expr.Const(2.0), Expr.Const(3.0)))))
                "abs" -> Expr.Div(u, Expr.Func("abs", u))
                else -> Expr.Const(Double.NaN)
            }
            Expr.Mul(outer, du)
        }
    }

    fun integrate(expr: Expr): Expr? = integrateImpl(expr.simplify(), depth = 0)?.simplify()

    private fun integrateImpl(e: Expr, depth: Int): Expr? {
        if (depth > 3) return null
        if (!e.containsVar()) return Expr.Mul(e, Expr.Var)
        if (e is Expr.Var) return Expr.Div(Expr.Pow(Expr.Var, Expr.Const(2.0)), Expr.Const(2.0))
        if (e is Expr.Neg) return integrateImpl(e.inner, depth)?.let { Expr.Neg(it) }
        if (e is Expr.Add) {
            val il = integrateImpl(e.left, depth)
            val ir = integrateImpl(e.right, depth)
            if (il != null && ir != null) return Expr.Add(il, ir)
        }
        if (e is Expr.Sub) {
            val il = integrateImpl(e.left, depth)
            val ir = integrateImpl(e.right, depth)
            if (il != null && ir != null) return Expr.Sub(il, ir)
        }
        if (e is Expr.Mul) {
            if (!e.left.containsVar()) return integrateImpl(e.right, depth)?.let { Expr.Mul(e.left, it) }
            if (!e.right.containsVar()) return integrateImpl(e.left, depth)?.let { Expr.Mul(e.right, it) }
        }
        if (e is Expr.Div && !e.right.containsVar()) {
            return integrateImpl(e.left, depth)?.let { Expr.Div(it, e.right) }
        }
        integrateDirect(e)?.let { return it }
        expandOnce(e)?.let { expanded ->
            if (expanded != e) integrateImpl(expanded.simplify(), depth + 1)?.let { return it }
        }
        tryUSubstitution(e)?.let { return it }
        tryByParts(e, depth)?.let { return it }
        return null
    }

    private fun integrateDirect(e: Expr): Expr? {
        if (e is Expr.Pow) {
            val linBase = extractLinear(e.base)
            if (linBase != null && !e.exp.containsVar()) {
                val (a, _) = linBase
                val n = e.exp.eval(0.0)
                if (n.isFinite()) {
                    val antideriv = if (abs(n + 1.0) < 1e-9) {
                        Expr.Func("ln", Expr.Func("abs", e.base))
                    } else {
                        Expr.Div(Expr.Pow(e.base, Expr.Const(n + 1.0)), Expr.Const(n + 1.0))
                    }
                    return Expr.Div(antideriv, Expr.Const(a))
                }
            }
            val linExp = extractLinear(e.exp)
            if (!e.base.containsVar() && linExp != null) {
                val (a, _) = linExp
                val baseVal = e.base.eval(0.0)
                if (baseVal > 0.0 && abs(baseVal - 1.0) > 1e-9) {
                    val denom = if (abs(baseVal - E) < 1e-9) Expr.Const(a) else Expr.Mul(Expr.Const(a), Expr.Func("ln", e.base))
                    return Expr.Div(e, denom)
                }
            }
        }
        if (e is Expr.Func) {
            val lin = extractLinear(e.arg)
            if (lin != null) {
                val (a, _) = lin
                val baseAnti = integrateElementaryOfU(e.name, e.arg) ?: return null
                return Expr.Div(baseAnti, Expr.Const(a))
            }
        }
        if (e is Expr.Div && !e.left.containsVar()) {
            val c = e.left
            val denom = e.right
            extractLinear(denom)?.let { (a, _) ->
                return Expr.Mul(Expr.Div(c, Expr.Const(a)), Expr.Func("ln", Expr.Func("abs", denom)))
            }
            if (denom is Expr.Pow && !denom.exp.containsVar()) {
                val n = denom.exp.eval(0.0)
                extractLinear(denom.base)?.let { (a, _) ->
                    val anti = if (abs(n - 1.0) < 1e-9) Expr.Func("ln", Expr.Func("abs", denom.base))
                    else Expr.Div(Expr.Pow(denom.base, Expr.Const(1.0 - n)), Expr.Const(1.0 - n))
                    return Expr.Mul(Expr.Div(c, Expr.Const(a)), anti)
                }
                if (abs(n - 2.0) < 1e-9 && denom.base is Expr.Func) {
                    val lin = extractLinear(denom.base.arg)
                    if (lin != null) {
                        val a = lin.first
                        if (denom.base.name == "cos") return Expr.Mul(Expr.Div(c, Expr.Const(a)), Expr.Func("tan", denom.base.arg))
                        if (denom.base.name == "sin") return Expr.Neg(Expr.Mul(Expr.Div(c, Expr.Const(a)), Expr.Func("cot", denom.base.arg)))
                    }
                }
            }
            if (denom is Expr.Add) {
                val pair = matchConstAndXSq(denom.left, denom.right)
                if (pair != null && pair > 0.0) {
                    val a = sqrt(pair)
                    return Expr.Mul(Expr.Div(c, Expr.Const(a)), Expr.Func("atan", Expr.Div(Expr.Var, Expr.Const(a))))
                }
            }
            if (denom is Expr.Func && denom.name == "sqrt" && denom.arg is Expr.Sub) {
                val sub = denom.arg
                if (!sub.left.containsVar() && isXSquared(sub.right)) {
                    val k = sub.left.eval(0.0)
                    if (k > 0.0) {
                        val a = sqrt(k)
                        return Expr.Mul(c, Expr.Func("asin", Expr.Div(Expr.Var, Expr.Const(a))))
                    }
                }
            }
        }
        return null
    }

    private fun integrateElementaryOfU(funcName: String, u: Expr): Expr? = when (funcName) {
        "sin" -> Expr.Neg(Expr.Func("cos", u))
        "cos" -> Expr.Func("sin", u)
        "tan" -> Expr.Neg(Expr.Func("ln", Expr.Func("abs", Expr.Func("cos", u))))
        "cot" -> Expr.Func("ln", Expr.Func("abs", Expr.Func("sin", u)))
        "sec" -> Expr.Func("ln", Expr.Func("abs", Expr.Add(Expr.Func("sec", u), Expr.Func("tan", u))))
        "csc" -> Expr.Neg(Expr.Func("ln", Expr.Func("abs", Expr.Add(Expr.Func("csc", u), Expr.Func("cot", u)))))
        "exp" -> Expr.Func("exp", u)
        "ln" -> Expr.Sub(Expr.Mul(u, Expr.Func("ln", u)), u)
        "sinh" -> Expr.Func("cosh", u)
        "cosh" -> Expr.Func("sinh", u)
        "tanh" -> Expr.Func("ln", Expr.Func("cosh", u))
        "arsh" -> Expr.Sub(Expr.Mul(u, Expr.Func("arsh", u)), Expr.Func("sqrt", Expr.Add(Expr.Pow(u, Expr.Const(2.0)), Expr.Const(1.0))))
        "arch" -> Expr.Sub(Expr.Mul(u, Expr.Func("arch", u)), Expr.Func("sqrt", Expr.Sub(Expr.Pow(u, Expr.Const(2.0)), Expr.Const(1.0))))
        "arth" -> Expr.Add(Expr.Mul(u, Expr.Func("arth", u)), Expr.Mul(Expr.Const(0.5), Expr.Func("ln", Expr.Func("abs", Expr.Sub(Expr.Const(1.0), Expr.Pow(u, Expr.Const(2.0)))))))
        "sqrt" -> Expr.Mul(Expr.Div(Expr.Const(2.0), Expr.Const(3.0)), Expr.Pow(u, Expr.Const(1.5)))
        "cbrt" -> Expr.Mul(Expr.Div(Expr.Const(3.0), Expr.Const(4.0)), Expr.Pow(u, Expr.Div(Expr.Const(4.0), Expr.Const(3.0))))
        "asin" -> Expr.Add(Expr.Mul(u, Expr.Func("asin", u)), Expr.Func("sqrt", Expr.Sub(Expr.Const(1.0), Expr.Pow(u, Expr.Const(2.0)))))
        "acos" -> Expr.Sub(Expr.Mul(u, Expr.Func("acos", u)), Expr.Func("sqrt", Expr.Sub(Expr.Const(1.0), Expr.Pow(u, Expr.Const(2.0)))))
        "atan" -> Expr.Sub(Expr.Mul(u, Expr.Func("atan", u)), Expr.Mul(Expr.Const(0.5), Expr.Func("ln", Expr.Add(Expr.Const(1.0), Expr.Pow(u, Expr.Const(2.0))))))
        else -> null
    }

    private fun extractLinear(expr: Expr): Pair<Double, Double>? {
        val d = differentiate(expr)
        if (d.containsVar()) return null
        val a = d.eval(0.0)
        if (!a.isFinite() || abs(a) < 1e-9) return null
        val b = expr.eval(0.0)
        if (!b.isFinite()) return null
        return a to b
    }

    private fun isXSquared(expr: Expr): Boolean =
        expr is Expr.Pow && expr.base == Expr.Var && expr.exp is Expr.Const && abs(expr.exp.value - 2.0) < 1e-9

    private fun matchConstAndXSq(a: Expr, b: Expr): Double? = when {
        !a.containsVar() && isXSquared(b) -> a.eval(0.0)
        !b.containsVar() && isXSquared(a) -> b.eval(0.0)
        else -> null
    }

    private fun expandOnce(e: Expr): Expr? = when (e) {
        is Expr.Mul -> when {
            e.left is Expr.Add -> Expr.Add(Expr.Mul(e.left.left, e.right), Expr.Mul(e.left.right, e.right))
            e.left is Expr.Sub -> Expr.Sub(Expr.Mul(e.left.left, e.right), Expr.Mul(e.left.right, e.right))
            e.right is Expr.Add -> Expr.Add(Expr.Mul(e.left, e.right.left), Expr.Mul(e.left, e.right.right))
            e.right is Expr.Sub -> Expr.Sub(Expr.Mul(e.left, e.right.left), Expr.Mul(e.left, e.right.right))
            else -> null
        }
        is Expr.Div -> when {
            e.left is Expr.Add -> Expr.Add(Expr.Div(e.left.left, e.right), Expr.Div(e.left.right, e.right))
            e.left is Expr.Sub -> Expr.Sub(Expr.Div(e.left.left, e.right), Expr.Div(e.left.right, e.right))
            else -> null
        }
        is Expr.Pow -> if (e.exp is Expr.Const && abs(e.exp.value - 2.0) < 1e-9 && (e.base is Expr.Add || e.base is Expr.Sub)) Expr.Mul(e.base, e.base) else null
        else -> null
    }

    private fun tryUSubstitution(e: Expr): Expr? {
        val candidates = mutableSetOf<Expr>()
        collectSubExprs(e, candidates)
        candidates.remove(Expr.Var)
        for (u in candidates) {
            if (!u.containsVar()) continue
            val du = differentiate(u)
            if (du is Expr.Const && du.value.isZero()) continue
            constantRatio(e, Expr.Div(du, u))?.let { k ->
                return Expr.Mul(Expr.Const(k), Expr.Func("ln", Expr.Func("abs", u)))
            }
            constantRatio(e, Expr.Mul(u, du))?.let { k ->
                return Expr.Mul(Expr.Const(k / 2.0), Expr.Pow(u, Expr.Const(2.0)))
            }
            for (n in listOf(-3.0, -2.0, 2.0, 3.0, 0.5, -0.5)) {
                constantRatio(e, Expr.Mul(Expr.Pow(u, Expr.Const(n)), du))?.let { k ->
                    return Expr.Mul(Expr.Const(k / (n + 1.0)), Expr.Pow(u, Expr.Const(n + 1.0)))
                }
            }
            for (fn in listOf("sin", "cos", "exp", "sinh", "cosh")) {
                constantRatio(e, Expr.Mul(Expr.Func(fn, u), du))?.let { k ->
                    val anti = integrateElementaryOfU(fn, u) ?: return@let
                    return Expr.Mul(Expr.Const(k), anti)
                }
            }
            constantRatio(e, Expr.Mul(Expr.Pow(Expr.Const(E), u), du))?.let { k ->
                return Expr.Mul(Expr.Const(k), Expr.Pow(Expr.Const(E), u))
            }
        }
        return null
    }

    private fun collectSubExprs(e: Expr, out: MutableSet<Expr>) {
        out.add(e)
        when (e) {
            is Expr.Neg -> collectSubExprs(e.inner, out)
            is Expr.Add -> { collectSubExprs(e.left, out); collectSubExprs(e.right, out) }
            is Expr.Sub -> { collectSubExprs(e.left, out); collectSubExprs(e.right, out) }
            is Expr.Mul -> { collectSubExprs(e.left, out); collectSubExprs(e.right, out) }
            is Expr.Div -> { collectSubExprs(e.left, out); collectSubExprs(e.right, out) }
            is Expr.Pow -> { collectSubExprs(e.base, out); collectSubExprs(e.exp, out) }
            is Expr.Func -> collectSubExprs(e.arg, out)
            else -> Unit
        }
    }

    private fun constantRatio(num: Expr, den: Expr): Double? {
        val samples = listOf(0.37, 0.73, 1.19, 1.83, 2.41)
        var ratio: Double? = null
        var validCount = 0
        for (x in samples) {
            val nv = num.eval(x)
            val dv = den.eval(x)
            if (!nv.isFinite() || !dv.isFinite() || abs(dv) < 1e-8) continue
            val r = nv / dv
            if (!r.isFinite() || abs(r) < 1e-9) return null
            if (ratio == null) ratio = r
            else if (abs(r - ratio) > 1e-5 * (1.0 + abs(ratio))) return null
            validCount++
        }
        return if (validCount >= 3) ratio else null
    }

    private fun tryByParts(e: Expr, depth: Int): Expr? {
        if (e !is Expr.Mul) return null
        val pairs = listOf(e.left to e.right, e.right to e.left)
        for ((uCand, dvCand) in pairs) {
            val isPolySmall = uCand == Expr.Var ||
                (uCand is Expr.Pow && uCand.base == Expr.Var && uCand.exp is Expr.Const &&
                    (abs(uCand.exp.value - 2.0) < 1e-9 || abs(uCand.exp.value - 3.0) < 1e-9))
            if (isPolySmall && isTrigOrExp(dvCand)) {
                val v = integrateDirect(dvCand) ?: continue
                val du = differentiate(uCand)
                val rem = integrateImpl(Expr.Mul(v, du).simplify(), depth + 1) ?: continue
                return Expr.Sub(Expr.Mul(uCand, v), rem)
            }
            if (uCand is Expr.Func && uCand.name == "ln" && uCand.arg == Expr.Var) {
                val v = integrateDirect(dvCand) ?: if (dvCand == Expr.Var) Expr.Div(Expr.Pow(Expr.Var, Expr.Const(2.0)), Expr.Const(2.0)) else null
                if (v != null) {
                    val du = differentiate(uCand)
                    val rem = integrateImpl(Expr.Mul(v, du).simplify(), depth + 1) ?: continue
                    return Expr.Sub(Expr.Mul(uCand, v), rem)
                }
            }
            val expA = matchExpLinear(uCand)
            val trigB = matchSinCosLinear(dvCand)
            if (expA != null && trigB != null) {
                val a = expA
                val (fn, b) = trigB
                val denom = Expr.Const(a * a + b * b)
                val inner = if (fn == "sin") {
                    Expr.Sub(
                        Expr.Mul(Expr.Const(a), Expr.Func("sin", Expr.Mul(Expr.Const(b), Expr.Var))),
                        Expr.Mul(Expr.Const(b), Expr.Func("cos", Expr.Mul(Expr.Const(b), Expr.Var)))
                    )
                } else {
                    Expr.Add(
                        Expr.Mul(Expr.Const(a), Expr.Func("cos", Expr.Mul(Expr.Const(b), Expr.Var))),
                        Expr.Mul(Expr.Const(b), Expr.Func("sin", Expr.Mul(Expr.Const(b), Expr.Var)))
                    )
                }
                return Expr.Div(Expr.Mul(uCand, inner), denom)
            }
        }
        return null
    }

    private fun isTrigOrExp(e: Expr): Boolean = when (e) {
        is Expr.Func -> e.name in setOf("sin", "cos", "exp", "sinh", "cosh")
        is Expr.Pow -> e.base is Expr.Const && abs(e.base.value - E) < 1e-9
        else -> false
    }

    private fun matchExpLinear(e: Expr): Double? = when {
        e is Expr.Func && e.name == "exp" -> extractLinear(e.arg)?.takeIf { abs(it.second) < 1e-9 }?.first
        e is Expr.Pow && e.base is Expr.Const && abs(e.base.value - E) < 1e-9 -> extractLinear(e.exp)?.takeIf { abs(it.second) < 1e-9 }?.first
        else -> null
    }

    private fun matchSinCosLinear(e: Expr): Pair<String, Double>? {
        if (e is Expr.Func && (e.name == "sin" || e.name == "cos")) {
            val lin = extractLinear(e.arg) ?: return null
            if (abs(lin.second) < 1e-9) return e.name to lin.first
        }
        return null
    }
}
