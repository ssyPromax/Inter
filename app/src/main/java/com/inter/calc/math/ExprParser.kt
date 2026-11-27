package com.inter.calc.math

import kotlin.math.E
import kotlin.math.PI

class ParseException(message: String) : RuntimeException(message)

object ExprParser {
    private sealed class Token {
        data class Num(val value: Double) : Token()
        data class Ident(val name: String) : Token()
        data class Op(val ch: Char) : Token()
        data object LParen : Token()
        data object RParen : Token()
        data object Bar : Token()
    }

    private val knownFuncs = setOf(
        "sin", "cos", "tan", "sec", "csc", "cot",
        "asin", "acos", "atan", "arcsin", "arccos", "arctan",
        "sinh", "cosh", "tanh",
        "arsh", "arch", "arth", "asinh", "acosh", "atanh",
        "ln", "log", "exp", "sqrt", "cbrt", "abs"
    )

    fun parse(rawInput: String): Expr {
        val cleaned = normalize(rawInput)
        if (cleaned.isEmpty()) throw ParseException("请输入函数表达式")
        val rawTokens = tokenize(cleaned)
        val tokens = insertImplicitMultiplication(rawTokens)
        val parser = Parser(tokens)
        val expr = parser.parseExpression()
        if (!parser.isAtEnd()) throw ParseException("表达式末尾存在多余字符")
        return expr.simplify()
    }

    private fun normalize(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                '×', '·' -> sb.append('*')
                '÷' -> sb.append('/')
                '−', '–' -> sb.append('-')
                '（' -> sb.append('(')
                '）' -> sb.append(')')
                'π' -> sb.append("pi")
                '√' -> sb.append("sqrt")
                '²' -> sb.append("^2")
                '³' -> sb.append("^3")
                ' ', '\t', '\n' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun tokenize(s: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            when {
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var dotCount = 0
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
                        if (s[i] == '.') dotCount++
                        i++
                    }
                    if (dotCount > 1) throw ParseException("无效的数字格式")
                    val numStr = s.substring(start, i)
                    val value = numStr.toDoubleOrNull() ?: throw ParseException("无法解析数字: $numStr")
                    tokens.add(Token.Num(value))
                }
                ch.isLetter() -> {
                    val start = i
                    while (i < s.length && s[i].isLetter()) i++
                    val word = s.substring(start, i).lowercase()
                    if (word in knownFuncs || word == "pi" || word == "e" || word == "x") {
                        tokens.add(Token.Ident(word))
                    } else {
                        var pos = 0
                        while (pos < word.length) {
                            val matched = knownFuncs.filter { word.startsWith(it, pos) }.maxByOrNull { it.length }
                            if (matched != null) {
                                tokens.add(Token.Ident(matched))
                                pos += matched.length
                            } else if (word.startsWith("pi", pos)) {
                                tokens.add(Token.Ident("pi"))
                                pos += 2
                            } else if (word[pos] == 'x' || word[pos] == 'e') {
                                tokens.add(Token.Ident(word[pos].toString()))
                                pos += 1
                            } else {
                                throw ParseException("无法识别的符号: ${word.substring(pos)}")
                            }
                        }
                    }
                }
                ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '^' -> {
                    tokens.add(Token.Op(ch)); i++
                }
                ch == '(' -> { tokens.add(Token.LParen); i++ }
                ch == ')' -> { tokens.add(Token.RParen); i++ }
                ch == '|' -> { tokens.add(Token.Bar); i++ }
                else -> throw ParseException("不支持的字符: $ch")
            }
        }
        return tokens
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens
        val result = mutableListOf<Token>()
        for (idx in tokens.indices) {
            val cur = tokens[idx]
            if (result.isNotEmpty()) {
                val prev = result.last()
                val prevIsValue = prev is Token.Num || prev is Token.RParen || (prev is Token.Ident && prev.name !in knownFuncs)
                val curStartsValue = cur is Token.Num || cur is Token.LParen || cur is Token.Ident
                if (prevIsValue && curStartsValue) result.add(Token.Op('*'))
            }
            result.add(cur)
        }
        return result
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0
        fun isAtEnd(): Boolean = pos >= tokens.size
        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun advance(): Token = tokens[pos++]

        fun parseExpression(): Expr {
            var left = parseTerm()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
                    advance()
                    val right = parseTerm()
                    left = if (t.ch == '+') Expr.Add(left, right) else Expr.Sub(left, right)
                } else break
            }
            return left
        }

        private fun parseTerm(): Expr {
            var left = parseUnary()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.ch == '*' || t.ch == '/')) {
                    advance()
                    val right = parseUnary()
                    left = if (t.ch == '*') Expr.Mul(left, right) else Expr.Div(left, right)
                } else break
            }
            return left
        }

        private fun parseUnary(): Expr {
            val t = peek()
            if (t is Token.Op && t.ch == '+') { advance(); return parseUnary() }
            if (t is Token.Op && t.ch == '-') { advance(); return Expr.Neg(parseUnary()) }
            return parsePower()
        }

        private fun parsePower(): Expr {
            val base = parsePrimary()
            val t = peek()
            if (t is Token.Op && t.ch == '^') {
                advance()
                return Expr.Pow(base, parseUnary())
            }
            return base
        }

        private fun parsePrimary(): Expr {
            val t = peek() ?: throw ParseException("表达式不完整")
            return when (t) {
                is Token.Num -> { advance(); Expr.Const(t.value) }
                is Token.Ident -> {
                    advance()
                    when (t.name) {
                        "x" -> Expr.Var
                        "pi" -> Expr.Const(PI)
                        "e" -> Expr.Const(E)
                        else -> {
                            val canonical = when (t.name) {
                                "arcsin" -> "asin"
                                "arccos" -> "acos"
                                "arctan" -> "atan"
                                "asinh" -> "arsh"
                                "acosh" -> "arch"
                                "atanh" -> "arth"
                                else -> t.name
                            }
                            if (canonical !in knownFuncs) throw ParseException("未知函数: ${t.name}")
                            val arg = if (peek() is Token.LParen) {
                                advance()
                                val inside = parseExpression()
                                // 省略右括号时自动补全，大幅提升输入效率
                                if (peek() is Token.RParen) {
                                    advance()
                                }
                                inside
                            } else {
                                parseUnary()
                            }
                            Expr.Func(canonical, arg)
                        }
                    }
                }
                is Token.LParen -> {
                    advance()
                    val inside = parseExpression()
                    // 省略右括号时自动补全
                    if (peek() is Token.RParen) {
                        advance()
                    }
                    inside
                }
                is Token.Bar -> {
                    advance()
                    val inside = parseExpression()
                    // 省略右侧绝对值符号时自动补全
                    if (peek() is Token.Bar) {
                        advance()
                    }
                    Expr.Func("abs", inside)
                }
                else -> throw ParseException("意外的符号")
            }
        }
    }
}
