package com.lecturebrief.ui.util

/**
 * Pure-Kotlin pre-processor that converts inline (`$...$`, `\(...\)`) and display
 * (`$$...$$`, `\[...\]`) LaTeX math expressions into readable Unicode mathematical notation.
 */
object LatexMathFormatter {

    private val DISPLAY_DOLLAR_REGEX = Regex("""\$\$(.+?)\$\$""", RegexOption.DOT_MATCHES_ALL)
    private val DISPLAY_BRACKET_REGEX = Regex("""\\\[(.+?)\\\]""", RegexOption.DOT_MATCHES_ALL)
    private val INLINE_PAREN_REGEX = Regex("""\\\((.+?)\\\)""", RegexOption.DOT_MATCHES_ALL)
    private val INLINE_DOLLAR_REGEX = Regex("""(?<!\\)\$(?!\s)([^$\n]+?)(?<!\s)\$(?!\d)""")

    private val TEXT_WRAPPER_COMMANDS = listOf(
        "\\text", "\\mathrm", "\\mathbf", "\\mathit", "\\mathsf", "\\mathtt", "\\operatorname"
    )

    private val SYMBOL_MAP = linkedMapOf(
        // Multi-char / longer prefixes first
        "\\leftrightarrow" to "↔",
        "\\Leftrightarrow" to "⇔",
        "\\rightarrow" to "→",
        "\\Rightarrow" to "⇒",
        "\\leftarrow" to "←",
        "\\Leftarrow" to "⇐",
        "\\varepsilon" to "ε",
        "\\vartheta" to "ϑ",
        "\\varphi" to "φ",
        "\\therefore" to "∴",
        "\\subseteq" to "⊆",
        "\\supseteq" to "⊇",
        "\\emptyset" to "∅",
        "\\varnothing" to "∅",
        "\\parallel" to "∥",
        "\\partial" to "∂",
        "\\nexists" to "∄",
        "\\because" to "∵",
        "\\implies" to "⇒",
        "\\approx" to "≈",
        "\\forall" to "∀",
        "\\exists" to "∃",
        "\\subset" to "⊂",
        "\\supset" to "⊃",
        "\\degree" to "°",
        "\\mapsto" to "↦",
        "\\propto" to "∝",
        "\\lambda" to "λ",
        "\\Lambda" to "Λ",
        "\\epsilon" to "ε",
        "\\Epsilon" to "Ε",
        "\\upsilon" to "υ",
        "\\Upsilon" to "Υ",
        "\\otimes" to "⊗",
        "\\oplus" to "⊕",
        "\\bullet" to "•",
        "\\cdots" to "⋯",
        "\\ldots" to "…",
        "\\iiint" to "∭",
        "\\notin" to "∉",
        "\\equiv" to "≡",
        "\\nabla" to "∇",
        "\\infty" to "∞",
        "\\times" to "×",
        "\\theta" to "θ",
        "\\Theta" to "Θ",
        "\\gamma" to "γ",
        "\\Gamma" to "Γ",
        "\\delta" to "δ",
        "\\Delta" to "Δ",
        "\\sigma" to "σ",
        "\\Sigma" to "Σ",
        "\\omega" to "ω",
        "\\Omega" to "Ω",
        "\\alpha" to "α",
        "\\Alpha" to "Α",
        "\\kappa" to "κ",
        "\\Kappa" to "Κ",
        "\\prime" to "′",
        "\\angle" to "∠",
        "\\cdot" to "·",
        "\\iint" to "∬",
        "\\oint" to "∮",
        "\\prod" to "∏",
        "\\cong" to "≅",
        "\\beta" to "β",
        "\\Beta" to "Β",
        "\\zeta" to "ζ",
        "\\Zeta" to "Ζ",
        "\\iota" to "ι",
        "\\Iota" to "Ι",
        "\\dots" to "…",
        "\\circ" to "∘",
        "\\star" to "⋆",
        "\\land" to "∧",
        "\\lnot" to "¬",
        "\\perp" to "⊥",
        "\\int" to "∫",
        "\\sum" to "∑",
        "\\neq" to "≠",
        "\\leq" to "≤",
        "\\geq" to "≥",
        "\\div" to "÷",
        "\\ast" to "∗",
        "\\sim" to "∼",
        "\\cup" to "∪",
        "\\cap" to "∩",
        "\\lor" to "∨",
        "\\neg" to "¬",
        "\\iff" to "⇔",
        "\\eta" to "η",
        "\\Eta" to "Η",
        "\\rho" to "ρ",
        "\\Rho" to "Ρ",
        "\\tau" to "τ",
        "\\Tau" to "Τ",
        "\\phi" to "φ",
        "\\Phi" to "Φ",
        "\\chi" to "χ",
        "\\Chi" to "Χ",
        "\\psi" to "ψ",
        "\\Psi" to "Ψ",
        "\\to" to "→",
        "\\pm" to "±",
        "\\mp" to "∓",
        "\\ne" to "≠",
        "\\le" to "≤",
        "\\ge" to "≥",
        "\\ll" to "≪",
        "\\gg" to "≫",
        "\\in" to "∈",
        "\\ni" to "∋",
        "\\mu" to "μ",
        "\\Mu" to "Μ",
        "\\nu" to "ν",
        "\\Nu" to "Ν",
        "\\xi" to "ξ",
        "\\Xi" to "Ξ",
        "\\pi" to "π",
        "\\Pi" to "Π"
    )

    private val PLAIN_FUNCTION_REGEX = Regex(
        """\\(arcsin|arccos|arctan|sinh|cosh|tanh|sin|cos|tan|cot|sec|csc|log|ln|exp|lim|max|min|sup|inf|det|dim)(?![a-zA-Z])"""
    )

    private val SUPERSCRIPT_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ',
        'd' to 'ᵈ', 'e' to 'ᵉ', 'f' to 'ᶠ', 'g' to 'ᵍ', 'h' to 'ʰ',
        'j' to 'ʲ', 'k' to 'ᵏ', 'l' to 'ˡ', 'm' to 'ᵐ', 'o' to 'ᵒ',
        'p' to 'ᵖ', 'r' to 'ʳ', 's' to 'ˢ', 't' to 'ᵗ', 'u' to 'ᵘ',
        'v' to 'ᵛ', 'w' to 'ʷ', 'x' to 'ˣ', 'y' to 'ʸ', 'z' to 'ᶻ'
    )

    private val SUBSCRIPT_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )

    private val BLACKBOARD_MAP = mapOf(
        "R" to "ℝ",
        "N" to "ℕ",
        "Z" to "ℤ",
        "Q" to "ℚ",
        "C" to "ℂ"
    )

    fun formatMathInMarkdown(markdown: String): String {
        if (markdown.isBlank()) return markdown

        var result = markdown

        // 1. Display math blocks ($$...$$ and \[...\]) -> separated indented/bolded line
        result = DISPLAY_DOLLAR_REGEX.replace(result) { match ->
            formatDisplayBlock(match.groupValues[1])
        }
        result = DISPLAY_BRACKET_REGEX.replace(result) { match ->
            formatDisplayBlock(match.groupValues[1])
        }

        // 2. Inline math (\(...\) and $...$)
        result = INLINE_PAREN_REGEX.replace(result) { match ->
            formatLatexExpression(match.groupValues[1])
        }
        result = INLINE_DOLLAR_REGEX.replace(result) { match ->
            formatLatexExpression(match.groupValues[1])
        }

        // 3. Unescape literal \$ if present
        result = result.replace("\\$", "$")

        return result
    }

    private fun formatDisplayBlock(rawExpr: String): String {
        val formattedLines = rawExpr.trim()
            .lines()
            .map { formatLatexExpression(it) }
            .filter { it.isNotBlank() }
        if (formattedLines.isEmpty()) return ""
        val joined = formattedLines.joinToString("  ")
        return "\n    **$joined**\n"
    }

    internal fun formatLatexExpression(raw: String): String {
        var expr = raw.trim()
        if (expr.isEmpty()) return ""

        // Strip \left and \right sizing prefixes
        expr = expr.replace(Regex("""\\(left|right)(?![a-zA-Z])"""), "")

        // Replace spacing commands
        expr = expr.replace(Regex("""\\(qquad|quad|,|;|:)"""), " ")
        expr = expr.replace("\\!", "")

        // Unwrap \text{...}, \mathrm{...}, etc.
        for (cmd in TEXT_WRAPPER_COMMANDS) {
            expr = replaceCommandWithSingleArg(expr, cmd) { inner -> inner }
        }

        // Blackboard bold (\mathbb{R}, etc.)
        expr = replaceCommandWithSingleArg(expr, "\\mathbb") { inner ->
            BLACKBOARD_MAP[inner.trim()] ?: inner
        }

        // Convert \frac{a}{b}, \dfrac{a}{b}, \tfrac{a}{b}
        for (fracCmd in listOf("\\dfrac", "\\tfrac", "\\frac")) {
            expr = replaceFrac(expr, fracCmd)
        }

        // Convert \sqrt{x} and \sqrt[n]{x}
        expr = replaceSqrt(expr)

        // Replace known Greek & math symbols
        for ((cmd, unicode) in SYMBOL_MAP) {
            val pattern = Regex("""${Regex.escape(cmd)}(?![a-zA-Z])""")
            expr = pattern.replace(expr, Regex.escapeReplacement(unicode))
        }

        // Strip leading backslash from standard functions (\sin, \cos, \ln, \lim, etc.)
        expr = PLAIN_FUNCTION_REGEX.replace(expr) { it.groupValues[1] }

        // Convert superscripts and subscripts
        expr = replaceScripts(expr, '^', SUPERSCRIPT_MAP)
        expr = replaceScripts(expr, '_', SUBSCRIPT_MAP)

        // Strip remaining LaTeX grouping braces and collapse extra spaces
        expr = expr.replace("{", "").replace("}", "")
        expr = expr.replace(Regex(""" {2,}"""), " ").trim()

        return expr
    }

    private fun replaceFrac(input: String, command: String): String {
        var text = input
        var searchFrom = 0
        while (true) {
            val idx = text.indexOf(command, searchFrom)
            if (idx == -1) break

            val afterCmd = idx + command.length
            if (afterCmd < text.length && text[afterCmd].isLetter()) {
                searchFrom = afterCmd
                continue
            }

            val numRange = readBracedArg(text, afterCmd)
            if (numRange == null) {
                searchFrom = afterCmd
                continue
            }
            val denRange = readBracedArg(text, numRange.last + 1)
            if (denRange == null) {
                searchFrom = afterCmd
                continue
            }

            val num = formatLatexExpression(text.substring(numRange.first + 1, numRange.last))
            val den = formatLatexExpression(text.substring(denRange.first + 1, denRange.last))

            val isSimpleNum = num.none { it.isWhitespace() || it in "+-*/=" }
            val isSimpleDen = den.none { it.isWhitespace() || it in "+-*/=" }
            val replacement = if (isSimpleNum && isSimpleDen) {
                "$num/$den"
            } else {
                val formattedNum = if (isSimpleNum) num else "($num)"
                val formattedDen = if (isSimpleDen) den else "($den)"
                "$formattedNum/$formattedDen"
            }

            text = text.substring(0, idx) + replacement + text.substring(denRange.last + 1)
            searchFrom = idx + replacement.length
        }
        return text
    }

    private fun replaceSqrt(input: String): String {
        var text = input
        var searchFrom = 0
        while (true) {
            val idx = text.indexOf("\\sqrt", searchFrom)
            if (idx == -1) break

            var cursor = idx + "\\sqrt".length
            if (cursor < text.length && text[cursor].isLetter()) {
                searchFrom = cursor
                continue
            }

            while (cursor < text.length && text[cursor].isWhitespace()) cursor++

            var rootDegree: String? = null
            if (cursor < text.length && text[cursor] == '[') {
                val closeBracket = text.indexOf(']', cursor + 1)
                if (closeBracket != -1) {
                    rootDegree = text.substring(cursor + 1, closeBracket).trim()
                    cursor = closeBracket + 1
                }
            }

            val argRange = readBracedArg(text, cursor)
            if (argRange == null) {
                searchFrom = cursor
                continue
            }

            val inner = formatLatexExpression(text.substring(argRange.first + 1, argRange.last))
            val prefix = if (!rootDegree.isNullOrEmpty()) {
                toUnicodeScript(rootDegree, SUPERSCRIPT_MAP) ?: rootDegree
            } else {
                ""
            }
            val replacement = "${prefix}√($inner)"
            text = text.substring(0, idx) + replacement + text.substring(argRange.last + 1)
            searchFrom = idx + replacement.length
        }
        return text
    }

    private fun replaceCommandWithSingleArg(
        input: String,
        command: String,
        transform: (String) -> String
    ): String {
        var text = input
        var searchFrom = 0
        while (true) {
            val idx = text.indexOf(command, searchFrom)
            if (idx == -1) break

            val afterCmd = idx + command.length
            if (afterCmd < text.length && text[afterCmd].isLetter()) {
                searchFrom = afterCmd
                continue
            }

            val argRange = readBracedArg(text, afterCmd)
            if (argRange == null) {
                searchFrom = afterCmd
                continue
            }

            val inner = text.substring(argRange.first + 1, argRange.last)
            val replacement = transform(inner)
            text = text.substring(0, idx) + replacement + text.substring(argRange.last + 1)
            searchFrom = idx + replacement.length
        }
        return text
    }

    private fun replaceScripts(
        input: String,
        marker: Char,
        scriptMap: Map<Char, Char>
    ): String {
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (ch == marker && i + 1 < input.length) {
                if (input[i + 1] == '{') {
                    val range = readBracedArg(input, i + 1)
                    if (range != null) {
                        val content = input.substring(range.first + 1, range.last).trim()
                        val converted = toUnicodeScript(content, scriptMap)
                        if (converted != null) {
                            sb.append(converted)
                        } else {
                            sb.append(marker).append('(').append(content).append(')')
                        }
                        i = range.last + 1
                        continue
                    }
                } else {
                    val nextChar = input[i + 1]
                    val mapped = scriptMap[nextChar]
                    if (mapped != null) {
                        sb.append(mapped)
                        i += 2
                        continue
                    }
                }
            }
            sb.append(ch)
            i++
        }
        return sb.toString()
    }

    private fun toUnicodeScript(text: String, scriptMap: Map<Char, Char>): String? {
        if (text.isEmpty()) return ""
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (c == ' ') continue
            val mapped = scriptMap[c] ?: return null
            sb.append(mapped)
        }
        return sb.toString()
    }

    private fun readBracedArg(text: String, startIndex: Int): IntRange? {
        var idx = startIndex
        while (idx < text.length && text[idx].isWhitespace()) idx++
        if (idx >= text.length || text[idx] != '{') return null

        var depth = 0
        var i = idx
        while (i < text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return idx..i
                }
            }
            i++
        }
        return null
    }
}
