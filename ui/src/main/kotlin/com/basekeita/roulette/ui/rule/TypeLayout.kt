package com.basekeita.roulette.ui.rule

/**
 * 文字の大きさの設定（`fontScale`）に応じて切り替えるレイアウト値。iOS 版の `TypeLayout` に対応する。
 * しきい値以上（iOS の `isAccessibilitySize` にあたる）を「アクセシビリティサイズ」と呼ぶ。
 * Composable は `LocalDensity.current.fontScale` をそのまま渡す。
 */
object TypeLayout {
    /** アクセシビリティサイズとみなす `fontScale` の下限。 */
    const val ACCESSIBILITY_FONT_SCALE = 1.5f

    /**
     * 項目リストの行と順番の結果行に入れる最大行数。
     * 通常は 1 行に収めて省略するが、アクセシビリティサイズでは短い名前でも省略されてしまうため 2 行まで許す。
     */
    fun labelMaxLines(fontScale: Float): Int = if (isAccessibilitySize(fontScale)) {
        2
    } else {
        1
    }

    /**
     * 順番決めのコピーボタンの領域を内容に合わせて伸ばすか。
     * 通常は高さを固定して、結果の有無で下の要素の位置が動かないようにする。
     * アクセシビリティサイズでは固定のままだと文字が枠からはみ出るため、最小高さだけ残して伸ばす。
     */
    fun growsFixedAreas(fontScale: Float): Boolean = isAccessibilitySize(fontScale)

    private fun isAccessibilitySize(fontScale: Float): Boolean = fontScale >= ACCESSIBILITY_FONT_SCALE
}
