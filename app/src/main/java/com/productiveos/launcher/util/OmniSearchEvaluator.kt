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

    data class QuantumClipCommand(
        val filterQuery: String,
        val label: String
    ) : OmniSearchResult()

    data class FocusAudioCommand(
        val mode: FocusSoundMode?, // null means stop
        val label: String
    ) : OmniSearchResult()

    data class MicroLogCapture(
        val thought: String,
        val label: String
    ) : OmniSearchResult()

    data class OpenMicroLogVault(
        val label: String = "open micro-log journal (inbox.md) →"
    ) : OmniSearchResult()

    data class OpenBurnerNote(
        val label: String = "open 24h burner scratchpad →"
    ) : OmniSearchResult()
}

object OmniSearchEvaluator {

    private val decimalFormat = DecimalFormat("#,##0.##")

    fun evaluate(rawInput: String): OmniSearchResult? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()

        // 1. Micro-App 1: Quantum Clip (`c/` or `clips`)
        if (lower.startsWith("c/") || lower == "clips" || lower == "clip") {
            val subQuery = if (lower.startsWith("c/")) trimmed.substring(2).trim() else ""
            val label = if (subQuery.isEmpty()) {
                "quantum clip · open clipboard vault →"
            } else {
                "quantum clip · filter \"$subQuery\" →"
            }
            return OmniSearchResult.QuantumClipCommand(filterQuery = subQuery, label = label)
        }

        // 2. Micro-App 2: Focus Anchor (`focus rain`, `focus cafe`, `focus alpha`, `focus stop`)
        if (lower.startsWith("focus")) {
            val arg = lower.removePrefix("focus").trim()
            return when {
                arg == "stop" || arg == "off" -> OmniSearchResult.FocusAudioCommand(
                    mode = null,
                    label = "♪ stop focus anchor audio"
                )
                arg.contains("cafe") || arg.contains("brown") -> OmniSearchResult.FocusAudioCommand(
                    mode = FocusSoundMode.CAFE_BROWN,
                    label = "♪ play focus anchor: warm brownian noise"
                )
                arg.contains("alpha") || arg.contains("binaural") || arg.contains("flow") -> OmniSearchResult.FocusAudioCommand(
                    mode = FocusSoundMode.BINAURAL_ALPHA,
                    label = "♪ play focus anchor: alpha binaural flow"
                )
                else -> OmniSearchResult.FocusAudioCommand(
                    mode = FocusSoundMode.RAIN,
                    label = "♪ play focus anchor: rainfall acoustic stream"
                )
            }
        }

        // 3. Micro-App 3: Micro-Log (`log [thought]` or `logs` / `log/`)
        if (lower == "logs" || lower == "log/" || lower == "journal") {
            return OmniSearchResult.OpenMicroLogVault()
        }
        if (lower.startsWith("log ")) {
            val thought = trimmed.substring(4).trim()
            if (thought.isNotEmpty()) {
                return OmniSearchResult.MicroLogCapture(
                    thought = thought,
                    label = "✓ save to micro-log: \"$thought\""
                )
            }
        }

        // 4. Micro-App 4: Burner Notes (`burner` or `scratch`)
        if (lower == "burner" || lower == "scratch" || lower == "scratchpad") {
            return OmniSearchResult.OpenBurnerNote()
        }

        // 5. Bang command: ? search query
        if (trimmed.startsWith("?")) {
            val query = trimmed.removePrefix("?").trim()
            if (query.isNotEmpty()) {
                return OmniSearchResult.WebSearch(
                    query = query,
                    label = "search web: \"$query\""
                )
            }
        }

        // 6. Bang command: @ maps location
        if (trimmed.startsWith("@")) {
            val query = trimmed.removePrefix("@").trim()
            if (query.isNotEmpty()) {
                return OmniSearchResult.MapsSearch(
                    query = query,
                    label = "search maps: \"$query\""
                )
            }
        }

        // 7. Bang command: t [minutes] or timer [minutes]
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

        // 8. Inline Math evaluation
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
                    if (ops.isNotEmpty()) ops.pop()
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
