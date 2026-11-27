package com.inter.calc.math

/**
 * 将输入框中的实时原始文本（可能处于输入中途，如 `(1)/(`、`sin(x`、`x^(`）
 * 转换为带绿色光标竖线的标准 LaTeX，供输入框以 KaTeX 实时呈现。
 */
object InputLatexFormatter {
    private const val CARET_TOKEN = "@@CARET@@"

    fun toLiveLatex(raw: String, cursorPos: Int, caretColorHex: String = "#3F8F4F"): String {
        val pos = cursorPos.coerceIn(0, raw.length)
        val withCaret = raw.substring(0, pos) + CARET_TOKEN + raw.substring(pos)
        val converted = convertSegment(withCaret)
        val caretLatex = "\\color{$caretColorHex}{\\rule[-0.2em]{1.6pt}{1.15em}}"
        return converted.replace(CARET_TOKEN, caretLatex)
    }

    private fun convertSegment(input: String): String {
        if (input.isEmpty()) return ""
        val s = input
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("²", "^2")
            .replace("³", "^3")

        // 先递归处理顶层或括号内的分数 `/`、幂 `^`、根号 `√(`、函数名
        return parseAddSub(s)
    }

    private fun parseAddSub(s: String): String {
        val parts = splitTopLevel(s, setOf('+', '-'))
        if (parts.size == 1) return parseMulDiv(s)
        val sb = StringBuilder()
        for ((op, chunk) in parts) {
            if (op != null) sb.append(if (op == '+') " + " else " - ")
            sb.append(parseMulDiv(chunk))
        }
        return sb.toString()
    }

    private fun parseMulDiv(s: String): String {
        val slashIdx = findTopLevelChar(s, '/')
        if (slashIdx >= 0) {
            val leftRaw = s.substring(0, slashIdx)
            val rightRaw = s.substring(slashIdx + 1)
            val num = stripOuterParens(leftRaw).let { if (it.isEmpty()) "\\square" else parseAddSub(it) }
            val den = stripOuterParens(rightRaw).let { if (it.isEmpty()) "\\square" else parseAddSub(it) }
            return "\\frac{$num}{$den}"
        }
        val mulParts = splitTopLevel(s, setOf('*'))
        if (mulParts.size > 1) {
            return mulParts.joinToString(" \\cdot ") { (_, c) -> parsePow(c) }
        }
        return parsePow(s)
    }

    private fun parsePow(s: String): String {
        val caretIdx = findTopLevelChar(s, '^')
        if (caretIdx >= 0) {
            val baseRaw = s.substring(0, caretIdx)
            val expRaw = s.substring(caretIdx + 1)
            val base = parseAtom(baseRaw)
            val exp = stripOuterParens(expRaw).let { if (it.isEmpty()) "\\square" else parseAddSub(it) }
            return "{$base}^{$exp}"
        }
        return parseAtom(s)
    }

    private fun parseAtom(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return ""

        // 整段被括号包裹
        if (s.startsWith("(") && findMatchingParen(s, 0) == s.lastIndex) {
            val inner = s.substring(1, s.lastIndex)
            return if (inner.isEmpty()) "\\left(\\square\\right)" else "\\left(${parseAddSub(inner)}\\right)"
        }

        // 处理函数调用与根号，如 √(...)、sin(...)、arsh(...)、cbrt(...)
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (s.startsWith(CARET_TOKEN, i)) {
                sb.append(CARET_TOKEN)
                i += CARET_TOKEN.length
                continue
            }
            if (s[i] == '√') {
                if (i + 1 < s.length && s[i + 1] == '(') {
                    val close = findMatchingParen(s, i + 1)
                    val inner = if (close > i + 1) s.substring(i + 2, close) else s.substring(i + 2)
                    sb.append("\\sqrt{${if (inner.isEmpty()) "\\square" else parseAddSub(inner)}}")
                    i = if (close > i + 1) close + 1 else s.length
                } else {
                    sb.append("\\sqrt{\\square}")
                    i++
                }
                continue
            }
            if (s[i] == 'π') {
                sb.append("\\pi")
                i++
                continue
            }
            if (s[i] == '(') {
                val close = findMatchingParen(s, i)
                if (close > i) {
                    val inner = s.substring(i + 1, close)
                    sb.append("\\left(${parseAddSub(inner)}\\right)")
                    i = close + 1
                } else {
                    val inner = s.substring(i + 1)
                    sb.append("\\left(${parseAddSub(inner)}\\right.")
                    i = s.length
                }
                continue
            }
            if (s[i].isLetter()) {
                val start = i
                while (i < s.length && s[i].isLetter()) i++
                val word = s.substring(start, i)
                val lower = word.lowercase()
                when (lower) {
                    "sqrt" -> {
                        if (i < s.length && s[i] == '(') {
                            val close = findMatchingParen(s, i)
                            val inner = if (close > i) s.substring(i + 1, close) else s.substring(i + 1)
                            sb.append("\\sqrt{${if (inner.isEmpty()) "\\square" else parseAddSub(inner)}}")
                            i = if (close > i) close + 1 else s.length
                        } else sb.append("\\sqrt{}")
                    }
                    "cbrt" -> {
                        if (i < s.length && s[i] == '(') {
                            val close = findMatchingParen(s, i)
                            val inner = if (close > i) s.substring(i + 1, close) else s.substring(i + 1)
                            sb.append("\\sqrt[3]{${if (inner.isEmpty()) "\\square" else parseAddSub(inner)}}")
                            i = if (close > i) close + 1 else s.length
                        } else sb.append("\\sqrt[3]{}")
                    }
                    "pi" -> sb.append("\\pi")
                    "sin", "cos", "tan", "sec", "csc", "cot", "sinh", "cosh", "tanh", "ln", "log" -> sb.append("\\$lower")
                    "asin", "arcsin" -> sb.append("\\arcsin")
                    "acos", "arccos" -> sb.append("\\arccos")
                    "atan", "arctan" -> sb.append("\\arctan")
                    "arsh", "arch", "arth" -> sb.append("\\operatorname{$lower}")
                    else -> sb.append(word)
                }
                continue
            }
            sb.append(s[i])
            i++
        }
        return sb.toString()
    }

    private fun stripOuterParens(raw: String): String {
        val s = raw.trim()
        if (s.startsWith("(") && findMatchingParen(s, 0) == s.lastIndex) {
            return s.substring(1, s.lastIndex)
        }
        return s
    }

    private fun findMatchingParen(s: String, openIdx: Int): Int {
        var depth = 0
        for (i in openIdx until s.length) {
            when (s[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun findTopLevelChar(s: String, target: Char): Int {
        var depth = 0
        var i = 0
        while (i < s.length) {
            if (s.startsWith(CARET_TOKEN, i)) {
                i += CARET_TOKEN.length
                continue
            }
            when (s[i]) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
                target -> if (depth == 0) return i
            }
            i++
        }
        return -1
    }

    private fun splitTopLevel(s: String, ops: Set<Char>): List<Pair<Char?, String>> {
        val result = mutableListOf<Pair<Char?, String>>()
        var depth = 0
        var lastStart = 0
        var pendingOp: Char? = null
        var i = 0
        while (i < s.length) {
            if (s.startsWith(CARET_TOKEN, i)) {
                i += CARET_TOKEN.length
                continue
            }
            val ch = s[i]
            when {
                ch == '(' -> depth++
                ch == ')' -> if (depth > 0) depth--
                depth == 0 && ch in ops && i > lastStart -> {
                    result.add(pendingOp to s.substring(lastStart, i))
                    pendingOp = ch
                    lastStart = i + 1
                }
            }
            i++
        }
        result.add(pendingOp to s.substring(lastStart))
        return result
    }
}
