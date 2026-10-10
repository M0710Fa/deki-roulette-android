package com.basekeita.roulette.ui.rule

/**
 * コピー・共有に載せる結果の本文。両画面で共有する。
 * 文言そのものは `strings.xml` から渡し、ここでは組み立てだけを担う。指定の有無など、結果以外の情報は載せない。
 */
object ResultText {
    /** ルーレットの結果 1 行。「結果: ラーメン」の形にする。区切りは日英とも半角の「: 」で固定。 */
    fun roulette(
        label: String,
        heading: String,
    ): String = "$heading: $label"

    /** 順番決めの結果。「1. A チーム」を改行区切りで並べる。 */
    fun order(labels: List<String>): String = labels.mapIndexed { index, label -> "${index + 1}. $label" }.joinToString("\n")

    /** 共有に渡す本文。コピー用の本文の後に空行を挟んでアプリ名を 1 行足す。 */
    fun share(
        body: String,
        appName: String,
    ): String = "$body\n\n$appName"
}
