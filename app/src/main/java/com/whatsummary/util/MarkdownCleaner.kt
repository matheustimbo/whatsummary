package com.whatsummary.util

/**
 * Strips lightweight Markdown markup (bold/italic/inline code) from model
 * output so it renders cleanly in plain [androidx.compose.material3.Text].
 * Preserves emojis, line breaks, bullets (`-`, `*` at start of line) and
 * numbered list prefixes.
 *
 * The regexes are intentionally simple: we only care about common bold /
 * italic markers that small LLMs often emit even when asked not to.
 */
object MarkdownCleaner {

    // Bold: **text** or __text__  (non-greedy; text must not start/end with space)
    private val BOLD = Regex("""(\*\*|__)(\S(?:.*?\S)?)\1""")

    // Italic: *text* or _text_  (non-greedy; stricter to avoid eating bullets)
    private val ITALIC = Regex("""(?<![\w*])[*_](\S(?:.*?\S)?)[*_](?![\w*])""")

    // Inline code: `text`
    private val CODE = Regex("""`([^`\n]+)`""")

    // Stray **/__ left after a truncated response
    private val STRAY = Regex("""\*\*|__""")

    fun clean(raw: String): String {
        if (raw.isBlank()) return raw
        var out = raw
        out = BOLD.replace(out, "$2")
        out = ITALIC.replace(out, "$1")
        out = CODE.replace(out, "$1")
        out = STRAY.replace(out, "")
        return out
    }
}
