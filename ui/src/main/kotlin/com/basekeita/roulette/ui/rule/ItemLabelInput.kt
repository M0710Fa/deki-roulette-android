package com.basekeita.roulette.ui.rule

/** 項目の追加の入力欄の整形。iOS 版 `ItemLabel` の入力欄向けの部分。 */
object ItemLabelInput {
    /** Swift の `Character.isNewline` に当たる改行。CRLF は 1 つの改行として数える。 */
    private val newline = Regex("\r\n|[\n\r\u000B\u000C\u0085  ]")

    /** 連続する空白（改行・全角空白を含む）。`data:definition` の `Item.normalizeLabel` と同じ規則。 */
    private val whitespaceRun = Regex("""[\s\p{Z}]+""")

    /**
     * 連続する空白を 1 つに潰し、前後を落として上限の文字数で切る。
     * 値の正は `data:definition` の `Item.normalizeLabel` で、ui はそれに依存しないため同じ規則をここにも持つ
     * （同名の判定と、追加する件数の表示に使う）。
     */
    fun normalize(
        raw: String,
        maxLength: Int,
    ): String = raw.split(whitespaceRun).filter { part -> part.isNotEmpty() }.joinToString(" ").takeCharacters(maxLength)

    /**
     * 改行で行に分け、行ごとに `normalize` を掛けて空行を除く。
     * 改行区切りの文章を貼り付けてまとめて追加するときに使う。1 行の規則は `normalize` のまま変えない。
     */
    fun splitLines(
        raw: String,
        maxLength: Int,
    ): List<String> = raw.split(newline).map { line -> normalize(line, maxLength) }.filter { line -> line.isNotEmpty() }

    /**
     * 入力中の文字列を行ごとに上限の文字数で切る。改行は保つ（`\n` にそろえる）。
     * 入力欄全体で切ると 2 行目以降が消えるため、上限は行単位に掛ける。
     * 文字は見た目の 1 文字（書記素クラスタ）で数える。
     */
    fun clampLines(
        raw: String,
        maxLength: Int,
    ): String = raw.split(newline).joinToString("\n") { line -> line.takeCharacters(maxLength) }
}
