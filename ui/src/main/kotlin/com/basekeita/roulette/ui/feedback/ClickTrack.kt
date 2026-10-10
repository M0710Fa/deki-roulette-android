package com.basekeita.roulette.ui.feedback

/**
 * クリック音を指定の時刻に重ねた 1 本の波形を作る。
 * 1 発ずつタイマーで鳴らすとスケジューリングの揺れでリズムが乱れるので、
 * 鳴らす位置を先に波形へ焼いてしまい、サンプル単位でずれないようにする。
 */
internal object ClickTrack {
    /**
     * `click` を `timesSeconds`（開始からの秒）の位置に `gain` を掛けて足し合わせた波形を返す。
     * 重なった部分は加算され、-1..1 に収める。鳴らす時刻が無ければ空。
     */
    fun render(
        click: FloatArray,
        sampleRate: Int,
        timesSeconds: List<Double>,
        gain: Float,
    ): FloatArray {
        if (click.isEmpty() || timesSeconds.isEmpty() || sampleRate <= 0) return FloatArray(0)

        val offsets = timesSeconds.map { time -> (maxOf(0.0, time) * sampleRate).toInt() }
        val track = FloatArray(offsets.max() + click.size)
        offsets.forEach { offset ->
            click.forEachIndexed { index, sample -> track[offset + index] += sample * gain }
        }
        track.indices.forEach { index -> track[index] = track[index].coerceIn(-1f, 1f) }
        return track
    }
}
