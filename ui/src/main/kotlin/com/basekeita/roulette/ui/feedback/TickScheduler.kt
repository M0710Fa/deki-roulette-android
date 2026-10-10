package com.basekeita.roulette.ui.feedback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * あらかじめ決めた時刻（開始からの秒）に順に `onTick` を呼ぶ。触覚の刻みに使う。
 * 返した `Job` をキャンセルすると以降は呼ばれない。時刻が無ければ何もせず null。
 * 待ち時間は前の時刻との差で求める（テストでは仮想時間で進められるよう `delay` で待つ）。
 */
fun CoroutineScope.launchTicks(
    timesSeconds: List<Double>,
    onTick: () -> Unit,
): Job? {
    if (timesSeconds.isEmpty()) return null

    return launch {
        var elapsedMillis = 0L
        timesSeconds.forEach { time ->
            val targetMillis = (time * 1_000).toLong()
            delay(targetMillis - elapsedMillis)
            elapsedMillis = targetMillis
            onTick()
        }
    }
}
