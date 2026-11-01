package li.joye.yakuyomi.engine

/**
 * 翻譯器介面（CLAUDE.md §5 Translator）。
 * translate(queries) → 對應筆數的譯文；失敗的項回傳原文（§11 不變式：永不比原文更糟）。
 */
interface Translator {
    suspend fun translate(queries: List<String>): List<String>
}

/**
 * A translator that can take several pages in one call (see [TranslationEngine.translateBatch]). Implemented by the
 * cloud [LlmTranslator] and by translators the app supplies (for example an on-device model, passed to
 * [Yakuyomi.create]). A translator that is [AutoCloseable] is closed with the engine.
 */
interface BatchTranslator : Translator {
    suspend fun translatePages(pages: List<List<String>>): BatchTranslation
}

/**
 * The numbered-line protocol shared by every translator: queries go out as `<|1|>text` lines and come back the same
 * way, so each line maps to exactly one bubble.
 */
object LineProtocol {
    // 只認「<、可選|、數字、一串 |/>、譯文」⇒ 容 <|1|> / <|1>| / <|1>，非決定性格式錯不再整頁失敗。
    private val LINE_RE = Regex("""^<\|?(\d+)\s*[|>]+\s*(.*)$""")
    private val THINK_RE = Regex("""(</think>)?<think>.*?</think>""", RegexOption.DOT_MATCHES_ALL)

    /** `<|1|>first`, `<|2|>second`, … one per line. */
    fun numbered(queries: List<String>): String =
        queries.mapIndexed { i, q -> "<|${i + 1}|>${q.replace('\n', ' ')}" }.joinToString("\n")

    /** Line number (1-based) → text. Lines without a number are ignored; a thinking block is dropped first. */
    fun parse(raw: String): Map<Int, String> {
        val cleaned = THINK_RE.replace(raw, "")
        val map = HashMap<Int, String>()
        for (line in cleaned.lineSequence()) {
            val m = LINE_RE.find(line.trim()) ?: continue
            map[m.groupValues[1].toInt()] = m.groupValues[2].trim()
        }
        return map
    }

    /**
     * Indices of queries the answer did not cover: number absent, or empty although the line has words (an empty
     * answer for "…" or "!?" is fine).
     */
    fun missing(queries: List<String>, parsed: Map<Int, String>): List<Int> = queries.indices.filter { i ->
        val got = parsed[i + 1]
        got == null || (got.isBlank() && queries[i].count { it.isLetter() } >= 2)
    }
}
