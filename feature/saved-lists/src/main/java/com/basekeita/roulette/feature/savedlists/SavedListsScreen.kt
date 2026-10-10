package com.basekeita.roulette.feature.savedlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.roulette.feature.savedlists.SavedListsViewModel.ViewState
import com.basekeita.roulette.ui.component.molecules.SwipeToDeleteBox
import com.basekeita.roulette.ui.component.template.SheetFrame
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography
import com.basekeita.roulette.ui.viewdata.SavedListSummaryUiModel
import java.util.UUID
import com.basekeita.roulette.ui.R as UiR

/** 保存したリストの管理の Route。項目リストの見出しのメニューからボトムシートで開く。 */
@Composable
internal fun SavedListsScreen(viewModel: SavedListsViewModel = hiltViewModel()) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    SheetFrame(title = stringResource(R.string.saved_lists_title)) {
        when (val state = viewState) {
            ViewState.Loading -> Unit

            is ViewState.Error -> {
                CenteredMessage(text = state.message)
            }

            is ViewState.Success -> {
                SavedListsContent(
                    lists = state.uiState.lists,
                    onRename = viewModel::onRename,
                    onDelete = viewModel::onDelete,
                )
            }
        }
    }
}

/**
 * 名前と件数の一覧。行を押すと名前を変更し、左にスワイプするか行のメニューから削除する。
 * 名前の変更中の対象と入力はこの画面だけの一時的な状態なので、ここで持つ。
 */
@Composable
private fun SavedListsContent(
    lists: List<SavedListSummaryUiModel>,
    onRename: (UUID, String) -> Unit,
    onDelete: (UUID) -> Unit,
) {
    var renamingId by rememberSaveable { mutableStateOf<UUID?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }

    if (lists.isEmpty()) {
        CenteredMessage(text = stringResource(R.string.saved_lists_empty_hint))
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            lists.forEachIndexed { index, savedList ->
                key(savedList.id) {
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                    SwipeToDeleteBox(
                        onDelete = { onDelete(savedList.id) },
                        enabled = true,
                        shape = RectangleShape,
                    ) {
                        SavedListRow(
                            savedList = savedList,
                            onClick = {
                                newName = savedList.name
                                renamingId = savedList.id
                            },
                            onDelete = { onDelete(savedList.id) },
                        )
                    }
                }
            }
        }
    }

    // 名前の変更中にそのリストが消えたら（別の画面で削除したなど）ダイアログも閉じる
    val renamingList = lists.firstOrNull { savedList -> savedList.id == renamingId }
    if (renamingList != null) {
        RenameDialog(
            name = newName,
            onNameChange = { changedName -> newName = changedName },
            onRename = {
                renamingId = null
                onRename(renamingList.id, newName)
            },
            onDismiss = { renamingId = null },
        )
    }
}

/** 1 件の行。行を押すと名前を変更し、右端のメニューから削除する。 */
@Composable
private fun SavedListRow(
    savedList: SavedListSummaryUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val renameLabel = stringResource(R.string.saved_lists_rename_accessibility_label, savedList.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // スワイプで横にずらしたとき、下の削除の背景が透けないよう、シートの既定の色（surfaceContainerLow）で塗る
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = savedList.name,
                    style = DekiTypography.subheadline.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    text = pluralStringResource(UiR.plurals.saved_list_item_count, savedList.itemCount, savedList.itemCount),
                    style = DekiTypography.caption,
                )
            },
            trailingContent = {
                Icon(painter = painterResource(R.drawable.ic_edit), contentDescription = null)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onClick)
                // 名前と件数は読み上げず、「〇〇 の名前を変更」とだけ伝える（iOS と同じ）
                .clearAndSetSemantics { contentDescription = renameLabel },
        )
        RowMenu(
            listName = savedList.name,
            onDelete = onDelete,
            modifier = Modifier.padding(end = 4.dp),
        )
    }
}

/** 行のメニュー。削除だけを置く。スワイプの削除は読み上げに出さないので、同じ操作をここでも行えるようにしておく。 */
@Composable
private fun RowMenu(
    listName: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(UiR.drawable.ic_more),
                contentDescription = stringResource(R.string.saved_lists_row_menu_label, listName),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.saved_lists_delete)) },
                leadingIcon = { Icon(painter = painterResource(UiR.drawable.ic_delete), contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
            )
        }
    }
}

/** 名前を付け替える。名前が空白だけのうちは変更できない。 */
@Composable
private fun RenameDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onRename: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.saved_lists_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text(text = stringResource(UiR.string.save_list_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onRename, enabled = name.isNotBlank()) {
                Text(text = stringResource(R.string.saved_lists_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(UiR.string.cancel))
            }
        },
    )
}

@Composable
private fun CenteredMessage(text: String) {
    Text(
        text = text,
        style = DekiTypography.subheadline,
        color = DekiTheme.colorScheme.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    )
}

@Preview(widthDp = 360)
@Composable
private fun SavedListsContentPreview() {
    DekiTheme {
        SheetFrame(title = "保存したリスト") {
            SavedListsContent(
                lists = listOf(
                    SavedListSummaryUiModel(id = UUID.randomUUID(), name = "ランチ", itemCount = 3),
                    SavedListSummaryUiModel(id = UUID.randomUUID(), name = "チーム", itemCount = 4),
                ),
                onRename = { _, _ -> },
                onDelete = {},
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun SavedListsEmptyPreview() {
    DekiTheme {
        SheetFrame(title = "保存したリスト") {
            SavedListsContent(lists = emptyList(), onRename = { _, _ -> }, onDelete = {})
        }
    }
}
