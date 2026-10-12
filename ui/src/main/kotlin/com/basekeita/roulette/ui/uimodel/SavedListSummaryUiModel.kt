package com.basekeita.roulette.ui.uimodel

import androidx.compose.runtime.Immutable
import java.util.UUID

/** 項目リストの見出しのメニューで、読み込める保存したリストを並べるための 1 件。 */
@Immutable
data class SavedListSummaryUiModel(
    val id: UUID,
    val name: String,
    val itemCount: Int,
)
