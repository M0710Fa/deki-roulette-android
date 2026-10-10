package com.basekeita.roulette.domain.spin

import com.basekeita.roulette.data.definition.model.SpinDirection
import kotlin.random.Random

/**
 * スピンの回転角の計算。針は盤面の真上（0 度）に固定され、盤面が回る。画面上の時計回りが正。
 * スライス i は角度 [i * slice, (i + 1) * slice) を占め、回転 R のとき針の下に来る盤面角は (360 - R) mod 360。
 */
object RouletteMath {
    /**
     * `targetIndex` のスライスが針の下で止まる、次の累積回転角を返す。
     * 見た目をランダムにするため、スライス内の停止位置と周回数は乱数で散らす。
     */
    fun nextRotation(
        current: Double,
        targetIndex: Int,
        count: Int,
        random: Random,
        direction: SpinDirection = SpinDirection.CLOCKWISE,
    ): Double {
        val fullSpins = SpinConfig.FULL_SPIN_RANGE.random(random)
        return nextRotation(current, targetIndex, count, fullSpins, random, direction)
    }

    /**
     * 周回数を外から与える版。フリックの強さを周回数にだけ反映するために使う。スライス内の停止位置の決め方は乱数のまま変えない。
     *
     * `direction` が反時計回りのときは累積角を減らす向きに決める。停止位置までの差分を逆向きに測るだけで、
     * 近すぎるとき 1 周足す処理と周回数の扱いは時計回りと同じ。
     */
    fun nextRotation(
        current: Double,
        targetIndex: Int,
        count: Int,
        fullSpins: Int,
        random: Random,
        direction: SpinDirection = SpinDirection.CLOCKWISE,
    ): Double {
        require(count > 0 && targetIndex in 0 until count && fullSpins >= 0)
        val sliceAngle = 360.0 / count
        val rawAngle = targetIndex * sliceAngle + random.nextDouble() * sliceAngle
        val desiredMod = positiveMod(360 - rawAngle)
        val currentMod = positiveMod(current)
        var delta =
            when (direction) {
                SpinDirection.CLOCKWISE -> positiveMod(desiredMod - currentMod)
                SpinDirection.COUNTERCLOCKWISE -> positiveMod(currentMod - desiredMod)
            }
        // 止まる位置がほぼ同じだと回った感じが出ないので、1 周足す
        if (delta < 10) delta += 360

        return when (direction) {
            SpinDirection.CLOCKWISE -> current + fullSpins * 360.0 + delta
            SpinDirection.COUNTERCLOCKWISE -> current - fullSpins * 360.0 - delta
        }
    }

    /** 回転角 `rotation` のとき針の下にあるスライスの添字。 */
    fun indexUnderPointer(
        rotation: Double,
        count: Int,
    ): Int {
        require(count > 0)
        val wheelAngle = positiveMod(360 - positiveMod(rotation))
        val index = (wheelAngle / (360.0 / count)).toInt()
        return minOf(index, count - 1)
    }

    fun positiveMod(value: Double): Double {
        val remainder = value % 360
        return if (remainder < 0) {
            remainder + 360
        } else {
            remainder
        }
    }
}
