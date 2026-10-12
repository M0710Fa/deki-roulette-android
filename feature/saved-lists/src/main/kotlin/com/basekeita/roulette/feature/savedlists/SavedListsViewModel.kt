package com.basekeita.roulette.feature.savedlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.roulette.data.definition.repository.SavedListsRepository
import com.basekeita.roulette.ui.uimodel.SavedListSummaryUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** 保存したリストの管理。名前の変更と削除だけを置く（読み込みは項目リストのメニューから）。保存と読み込みだけなので Repository を直接呼ぶ。 */
@HiltViewModel
class SavedListsViewModel
@Inject
constructor(
    private val savedListsRepository: SavedListsRepository,
) : ViewModel() {
    sealed interface ViewState {
        data object Loading : ViewState

        data class Error(
            val message: String,
        ) : ViewState

        data class Success(
            val uiState: UiState,
        ) : ViewState
    }

    data class UiState(
        val lists: List<SavedListSummaryUiModel>,
    )

    val viewState: StateFlow<ViewState> =
        savedListsRepository.lists.map { lists ->
            val summaries = lists.map { list -> SavedListSummaryUiModel(id = list.id, name = list.name, itemCount = list.items.size) }
            ViewState.Success(UiState(lists = summaries)) as ViewState
        }.catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    /** 名前を付け替える。名前が空なら変えない（正規化は Repository が行う）。 */
    fun onRename(
        id: UUID,
        name: String,
    ) {
        viewModelScope.launch { savedListsRepository.rename(id, name) }
    }

    fun onDelete(id: UUID) {
        viewModelScope.launch { savedListsRepository.delete(id) }
    }
}
