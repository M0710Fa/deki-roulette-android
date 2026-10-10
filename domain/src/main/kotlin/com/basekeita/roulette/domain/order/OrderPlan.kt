package com.basekeita.roulette.domain.order

import com.basekeita.roulette.data.definition.model.Item

/**
 * 並べ替えの結果と演出の時間。
 *
 * @property revealDurationSeconds 結果がすべて出終わるまでの時間（秒）。この間は並べ替えを受け付けない
 * @property hapticTimes 結果の行が 1 件ずつ現れる時刻（開始からの秒）。「アニメーションを削除」のときは空
 */
data class OrderPlan(
    val ordered: List<Item>,
    val revealDurationSeconds: Double,
    val hapticTimes: List<Double>,
)
