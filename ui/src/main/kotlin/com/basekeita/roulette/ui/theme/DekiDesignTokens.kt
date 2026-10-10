@file:Suppress("ktlint:standard:comment-wrapping", "ktlint:standard:discouraged-comment-location")

package com.basekeita.roulette.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// 配色。iOS 版 `Design/Theme.swift` の値をそのまま使う（3D の盤面のトークンは対象外）。
//
// トークン名は Web 版のまま据え置き、ライトでは役割ごとに値を反転させる
// （`ink` 系は「地の側」、`ivory` と `muted` は「文字の側」として読む）。
// 盤面・開始ボタン・削除ボタンなど外観に依らない色は `DekiFixedColors` に置く。
// ─────────────────────────────────────────────────────────────────────────────

/** 端末の外観設定（ライト / ダーク）で値が変わる色。 */
@Immutable
data class DekiColorScheme(
    val ink900: Color, // 画面の背景。スプラッシュの背景にも同じ 2 値を使う
    val ink800: Color, // カード・入力欄・タブバーの背景
    val ink700: Color, // 枠線、無効ボタンの背景
    val ink600: Color, // 入力欄の枠、破線枠
    val ink500: Color, // 指定中の行の枠
    val ink400: Color, // 削除ボタン
    val ivory: Color, // 本文
    val muted: Color, // 補助テキスト
    val gold: Color, // 順番決めの 1 位とフォーカスリング専用。ルーレットの結果は sliceAccent を使う
    val flareText: Color, // flare を文字に使うときの色。ライトの地では塗りのままだと読めないため濃くする
    val flareGlow: Color, // 開始ボタンの光。ライトの地では強く出すぎるため弱める
    val wheelShadow: Color, // 盤面の影。ライトの地では濃く出すぎるため弱める
    val pointerShadow: Color, // 針の影・結果の帯の影
    val sliceAccents: List<Color>, // 文字・色見本・細い枠に使うスライス色。指定中の印にもこれを流用する
)

/** ライト。 */
val lightDekiColorScheme = DekiColorScheme(
    ink900 = Color(0xFFFAF7F2),
    ink800 = Color(0xFFFFFFFF),
    ink700 = Color(0xFFE6DFD5),
    ink600 = Color(0xFFD5CBBE),
    ink500 = Color(0xFFB6A9C6),
    ink400 = Color(0xFF7A6C90),
    ivory = Color(0xFF241C30),
    muted = Color(0xFF655877),
    gold = Color(0xFF8A6410),
    flareText = Color(0xFFC81E37),
    flareGlow = Color(0x4DFF4E63), // #FF4E63, alpha 0.3
    wheelShadow = Color(0x33000000), // alpha 0.2
    pointerShadow = Color(0x40000000), // alpha 0.25
    // 明るい塗りのままでは白地で読めないため、同じ色相のまま暗くした 10 色
    sliceAccents = listOf(
        Color(0xFFD92323),
        Color(0xFFAF571D),
        Color(0xFF856D20),
        Color(0xFF557B30),
        Color(0xFF327C60),
        Color(0xFF2F7A8A),
        Color(0xFF2F6FC8),
        Color(0xFF785CD0),
        Color(0xFF9E48C8),
        Color(0xFFD32278),
    ),
)

/** ダーク。 */
val darkDekiColorScheme = DekiColorScheme(
    ink900 = Color(0xFF17111F),
    ink800 = Color(0xFF1F1829),
    ink700 = Color(0xFF2A2138),
    ink600 = Color(0xFF3A2F4C),
    ink500 = Color(0xFF4C3F62),
    ink400 = Color(0xFF7D6D96),
    ivory = Color(0xFFF5EFE6),
    muted = Color(0xFFA99BBD),
    gold = Color(0xFFFFC94A),
    flareText = Color(0xFFFF4E63),
    flareGlow = Color(0x73FF4E63), // #FF4E63, alpha 0.45
    wheelShadow = Color(0x80000000), // alpha 0.5
    pointerShadow = Color(0x8C000000), // alpha 0.55
    // ダークの地では盤面の塗りをそのまま使う
    sliceAccents = DekiFixedColors.sliceColors,
)

/**
 * 外観設定に依らない色。
 *
 * 盤面はスライスを「明るい塗り + 暗い文字」でコントラストを取っているため、地の側だけ反転させると成り立たなくなる。
 * 開始ボタン・削除ボタン・結果の帯も、塗りを固定してその上の文字も固定する。
 */
object DekiFixedColors {
    /** スピン・並べ替えの操作専用。 */
    val flare = Color(0xFFFF4E63)

    /** `flare` の塗りに乗る文字。 */
    val onFlare = Color(0xFF17111F)

    /** スライスの区切り線とラベル。 */
    val onSlice = Color(0xFF17111F)

    /** 盤面の縁。 */
    val wheelRim = Color(0xFF2A2138)

    /** 盤面の外周線。 */
    val wheelEdge = Color(0xFF4C3F62)

    /** 中心のハブ。 */
    val wheelHub = Color(0xFF1F1829)

    /** ハブの枠と中心の点。 */
    val wheelHubMark = Color(0xFFF5EFE6)

    /**
     * 盤面のスライスを塗る 10 色。彩度と明度を揃えてあり、ラベルとセパレータを `onSlice` で描くため、
     * どのスライスも暗色テキストで 4.5:1 を超える明るさに寄せてある。
     */
    val sliceColors = listOf(
        Color(0xFFFF8080),
        Color(0xFFFFA366),
        Color(0xFFF2CE5C),
        Color(0xFFA3DB6B),
        Color(0xFF5FD6A8),
        Color(0xFF5CC9E0),
        Color(0xFF7BAEF5),
        Color(0xFFA48CF0),
        Color(0xFFCE8CEE),
        Color(0xFFFF85C0),
    )

    /** 止まっていないスライスに重ねて暗くする色。どの項目かは見分けられる範囲に留める。 */
    val sliceDim = onSlice.copy(alpha = 0.5f)

    /** 止まったスライスの縁取り。沈めた隣のスライスに対して 3:1 以上の明るさにする。 */
    val stopOutline = Color(0xFFF5EFE6)

    /** 光彩の不透明度。白系・止まったスライスの色のどちらにも使う。 */
    const val STOP_GLOW_ALPHA = 0.8f

    /** 止まったスライスから外側へにじませる光彩（白系）。 */
    val stopGlow = Color(0xFFF5EFE6).copy(alpha = STOP_GLOW_ALPHA)

    /**
     * 虹色の光彩の色の並び。新しい色は足さず、色相順に並んだスライスの 10 色を 12 時から時計回りに一周させる
     * （末尾に先頭の色を足して継ぎ目をつなぐ）。盤面の塗りと同じ色なので、`gold` / `flare` の用途とは重ならず、文字にも使わない。
     */
    val stopGlowRainbow = sliceColors + sliceColors.first()

    /** 結果の帯の地。 */
    val resultBandFill = Color(0xFFF5EFE6)

    /** 結果の帯の文字。 */
    val resultBandInk = onSlice

    /** 行を左にスワイプしたときに出る削除ボタンの塗り。 */
    val destructive = Color(0xFFC62B3B)

    /** 削除ボタンの文字。 */
    val onDestructive = Color(0xFFFFFFFF)
}
