package com.basekeita.roulette

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * [bottomSheet] を付けたエントリを [ModalBottomSheet] で出す（Navigation 3 の `DialogSceneStrategy` と、
 * 公式サンプル `AnimatedBottomSheetSample` と同じ作り）。シートを下げる・外側を押す・戻る操作でバックスタックから外す。
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        lastEntry.metadata[BottomSheetKey] ?: return null

        return BottomSheetScene(
            entry = lastEntry,
            previousEntries = entries.dropLast(1),
            onBack = onBack,
        )
    }

    companion object {
        object BottomSheetKey : NavMetadataKey<Boolean>

        /** エントリをボトムシートで出す印。`NavEntry.metadata` に渡す。 */
        fun bottomSheet(): Map<String, Any> = metadata { put(BottomSheetKey, true) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class BottomSheetScene<T : Any>(
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val key: Any = entry.contentKey

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val overlaidEntries: List<NavEntry<T>> = previousEntries

    private var sheetState: SheetState? = null

    override val content: @Composable (() -> Unit) = {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        sheetState = state
        ModalBottomSheet(
            onDismissRequest = onBack,
            sheetState = state,
        ) {
            entry.Content()
        }
    }

    /** バックスタックから外れたとき、シートを下げるアニメーションを終えてから消す。 */
    override suspend fun onRemove() {
        sheetState?.hide()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BottomSheetScene<*>) return false

        return entry == other.entry && previousEntries == other.previousEntries
    }

    override fun hashCode(): Int = 31 * entry.hashCode() + previousEntries.hashCode()
}
