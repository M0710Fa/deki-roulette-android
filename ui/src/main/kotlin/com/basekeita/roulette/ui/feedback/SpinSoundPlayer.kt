package com.basekeita.roulette.ui.feedback

/**
 * スピン中の「カチッ」を鳴らす。止まる位置も回転角もスピン開始時に決まっているので、鳴らす時刻も先に分かる。
 * 設定の「効果音」が OFF のときに呼ばないのは呼び出し側（ViewModel）の役目。
 * 端末の着信音モードが通常でない（マナー・サイレント）ときは鳴らさない（iOS の消音スイッチの代わり）。
 */
interface SpinSoundPlayer {
    /** 素材の読み込み。画面を出したところで呼んでおく。 */
    fun prepare()

    /** `timesSeconds`（呼んだ時点からの秒）にクリック音を鳴らす。前の再生は止める。 */
    fun play(timesSeconds: List<Double>)

    /** いますぐクリック音を 1 回鳴らす（指で動かしている盤面の針が境目を越えたとき）。 */
    fun playClick()

    /** 鳴らしている途中で止める（画面から離れるとき）。 */
    fun stop()
}
