package com.productiveos.launcher.util

import java.text.DecimalFormat
import java.util.Stack
import java.util.regex.Pattern

sealed class OmniSearchResult {
    data class MathResult(
        val expression: String,
        val resultFormatted: String,
        val copyText: String
    ) : OmniSearchResult()

    data class WebSearch(
        val query: String,
        val label: String
    ) : OmniSearchResult()

    data class MapsSearch(
        val query: String,
        val label: String
    ) : OmniSearchResult()

    data class TimerCommand(
        val minutes: Int,
        val label: String
    ) : OmniSearchResult()
}

object OmniSearchEvaluator {

    private val decimalFormat = DecimalFormat("#,##0.##")

    fun evaluate(rawInput: String): OmniSearchResult? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        // 1. Bang command: ? search query
        if (trimmed.startsWith("?")) {
            val query = trimmed.removePrefix("?").trim()
            if (query.isNotEmpty()) {
                return OmniSearchResult.WebSearch(
                    query = query,
                    label = "search web: \"$query\""
                )
            }
        }

        // 2. Bang command: @ maps location
        if (trimmed.startsWith("@")) {
            val query = trimmed.removePrefix("@").trim()
            if (query.isNotEmpty()) {
                return OmniSearchResult.MapsSearch(
                    query = query,
                    label = "search maps: \"$query\""
                )
            }
        }

        // 3. Bang command: t [minutes] or timer [minutes]
        val timerPattern = Pattern.compile("^(?:t|timer)\\s*(\\d+)(?:m|min)?$", Pattern.CASE_INSENSITIVE)
        val timerMatcher = timerPattern.matcher(trimmed)
        if (timerMatcher.matches()) {
            val minutes = timerMatcher.group(1)?.toIntOrNull()
            if (minutes != null && minutes in 1..1440) {
                return OmniSearchResult.TimerCommand(
                    minutes = minutes,
                    label = "start $minutes-min timer · tap to launch"
                )
            }
        }

        // 4. Inline Math evaluation
        if (containsMathOperators(trimmed)) {
            val mathRes = evaluateMathExpression(trimmed)
            if (mathRes != null) {
                val formatted = decimalFormat.format(mathRes)
                return OmniSearchResult.MathResult(
                    expression = trimmed,
                    resultFormatted = "= $formatted · tap to copy",
                    copyText = formatted
                )
            }
        }

        return null
    }

    private fun containsMathOperators(text: String): Boolean {
        if (!text.any { it.isDigit() }) return false
        val mathChars = setOf('+', '-', '*', '/', '%', '^')
        return text.any { it in mathChars }
    }

    private fun evaluateMathExpression(expression: String): Double? {
        return runCatching {
            var sanitized = expression
                .replace(" ", "")
                .replace("x", "*")
                .replace("X", "*")
                .replace("÷", "/")

            // Handle percentage: e.g. 50% * 200 or 15%
            sanitized = sanitized.replace(Regex("(\\d+(?:\\.\\d+)?)%")) { match ->
                val num = match.groupValues[1].toDoubleOrNull() ?: return@replace match.value
                (num / 100.0).toString()
            }

            if (!sanitized.matches(Regex("^[0-9+\\-*/^.()]+$"))) {
                return null
            }

            val tokens = tokenize(sanitized)
            val rpn = shuntingYard(tokens)
            evaluateRpn(rpn)
        }.getOrNull()
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    tokens.add(sb.toString())
                    continue
                }
                c == '-' -> {
                    // Check if unary minus
                    val isUnary = tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/", "^", "(")
                    if (isUnary) {
                        val sb = StringBuilder("-")
                        i++
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                            sb.append(expr[i])
                            i++
                        }
                        tokens.add(sb.toString())
                        continue
                    } else {
                        tokens.add("-")
                    }
                }
                c in "+*/^()" -> {
                    tokens.add(c.toString())
                }
            }
            i++
        }
        return tokens
    }

    private fun precedence(op: String): Int = when (op) {
        "+", "-" -> 1
        "*", "/" -> 2
        "^" -> 3
        else -> 0
    }

    private fun shuntingYard(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val ops = Stack<String>()

        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> output.add(token)
                token == "(" -> ops.push(token)
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.peek() != "(") {
                        output.add(ops.pop())
                    }
                    if (ops.isNotEmpty()) ops.pop() // Discard '('
                }
                else -> {
                    while (ops.isNotEmpty() && precedence(ops.peek()) >= precedence(token)) {
                        output.add(ops.pop())
                    }
                    ops.push(token)
                }
            }
        }

        while (ops.isNotEmpty()) {
            output.add(ops.pop())
        }
        return output
    }

    private fun evaluateRpn(rpn: List<String>): Double {
        val stack = Stack<Double>()
        for (token in rpn) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                stack.push(num)
            } else if (stack.size >= 2) {
                val b = stack.pop()
                val a = stack.pop()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0.0) a / b else Double.NaN
                    "^" -> Math.pow(a, b)
                    else -> 0.0
                }
                stack.push(res)
            }
        }
        val finalVal = if (stack.isNotEmpty()) stack.pop() else 0.0
        return if (finalVal.isInfinite() || finalVal.isNaN()) 0.0 else finalVal
    }
}
