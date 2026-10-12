package com.basekeita.roulette.feature.roulette.uimodel

import androidx.compose.runtime.Immutable
import java.util.UUID

/** 盤面のスライス 1 つ。色は並び順と件数から盤面が決める。 */
@Immutable
data class WheelSliceUiModel(
    val id: UUID,
    val label: String,
)
