package com.basekeita.roulette.feature.savedlists

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.SavedList
import com.basekeita.roulette.data.definition.repository.SavedListsRepository
import com.basekeita.roulette.feature.savedlists.SavedListsViewModel.UiState
import com.basekeita.roulette.feature.savedlists.SavedListsViewModel.ViewState
import com.basekeita.roulette.ui.viewdata.SavedListSummaryUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SavedListsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val lunch = SavedList(name = "ランチ", items = listOf("ラーメン", "カレー", "寿司").map { label -> Item(label = label) })
    private val team = SavedList(name = "チーム", items = listOf("A", "B").map { label -> Item(label = label) })

    /** メモリに持つ `SavedListsRepository`。名前の正規化は省き、空白だけの名前を拒む。 */
    private class FakeSavedListsRepository(
        initial: List<SavedList>,
    ) : SavedListsRepository {
        override val lists = MutableStateFlow(initial)

        override suspend fun save(
            name: String,
            items: List<Item>,
        ): Boolean = false

        override suspend fun rename(
            id: UUID,
            name: String,
        ): Boolean {
            if (name.isBlank() || lists.value.none { list -> list.id == id }) return false

            lists.value = lists.value.map { list ->
                if (list.id == id) {
                    list.copy(name = name)
                } else {
                    list
                }
            }
            return true
        }

        override suspend fun delete(id: UUID): Boolean {
            if (lists.value.none { list -> list.id == id }) return false

            lists.value = lists.value.filter { list -> list.id != id }
            return true
        }
    }

    /** `WhileSubscribed` の状態を購読しておき、いまの UiState を読めるようにする。 */
    private fun TestScope.observe(viewModel: SavedListsViewModel): () -> UiState {
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        return { (viewModel.viewState.value as ViewState.Success).uiState }
    }

    @Test
    fun `保存したリストを名前と件数で並べる`() = runTest {
        val uiState = observe(SavedListsViewModel(FakeSavedListsRepository(listOf(lunch, team))))

        assertEquals(
            listOf(
                SavedListSummaryUiModel(id = lunch.id, name = "ランチ", itemCount = 3),
                SavedListSummaryUiModel(id = team.id, name = "チーム", itemCount = 2),
            ),
            uiState().lists,
        )
    }

    @Test
    fun `保存したリストを読めなければErrorを出す`() = runTest {
        val failingRepository =
            object : SavedListsRepository {
                override val lists: Flow<List<SavedList>> = flow { throw IllegalStateException("読めません") }

                override suspend fun save(
                    name: String,
                    items: List<Item>,
                ): Boolean = false

                override suspend fun rename(
                    id: UUID,
                    name: String,
                ): Boolean = false

                override suspend fun delete(id: UUID): Boolean = false
            }
        val viewModel = SavedListsViewModel(failingRepository)
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()

        assertEquals(ViewState.Error("読めません"), viewModel.viewState.value)
    }

    @Test
    fun `名前を変えると一覧に反映する`() = runTest {
        val viewModel = SavedListsViewModel(FakeSavedListsRepository(listOf(lunch, team)))
        val uiState = observe(viewModel)

        viewModel.onRename(team.id, "開発チーム")
        runCurrent()

        assertEquals(listOf("ランチ", "開発チーム"), uiState().lists.map { list -> list.name })
    }

    @Test
    fun `削除すると一覧から消える`() = runTest {
        val viewModel = SavedListsViewModel(FakeSavedListsRepository(listOf(lunch, team)))
        val uiState = observe(viewModel)

        viewModel.onDelete(lunch.id)
        runCurrent()

        assertEquals(listOf(team.id), uiState().lists.map { list -> list.id })

        viewModel.onDelete(team.id)
        runCurrent()

        assertTrue(uiState().lists.isEmpty())
    }
}
