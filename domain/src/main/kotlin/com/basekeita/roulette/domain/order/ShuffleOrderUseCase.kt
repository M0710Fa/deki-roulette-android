package com.basekeita.roulette.domain.order

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.domain.haptic.HapticSchedule
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

/** 順番決めの並べ替えと演出の時間を決める。先頭・末尾の指定は ViewModel から引数で受け取るだけで、保存しない。 */
class ShuffleOrderUseCase
@Inject
constructor(
    private val random: Random,
) {
    /** 項目が `Item.MIN_COUNT` 未満なら null。 */
    operator fun invoke(
        items: List<Item>,
        firstId: UUID?,
        lastId: UUID?,
        reducedMotion: Boolean,
    ): OrderPlan? {
        if (items.size < Item.MIN_COUNT) return null

        return OrderPlan(
            ordered = Shuffler.shuffle(items, firstId, lastId, random),
            revealDurationSeconds = RevealTiming.duration(items.size, reducedMotion),
            hapticTimes = HapticSchedule.revealTicks(items.size, reducedMotion),
        )
    }
}
