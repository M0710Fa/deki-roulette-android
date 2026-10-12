package com.basekeita.roulette.ui.component.organisms

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.roulette.ui.R
import com.basekeita.roulette.ui.component.atoms.DashedBorderBox
import com.basekeita.roulette.ui.component.molecules.AddItemForm
import com.basekeita.roulette.ui.component.molecules.ItemRow
import com.basekeita.roulette.ui.rule.DuplicateLabels
import com.basekeita.roulette.ui.rule.ItemLabelInput
import com.basekeita.roulette.ui.rule.MarkVisibility
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography
import com.basekeita.roulette.ui.theme.sliceAccent
import com.basekeita.roulette.ui.uimodel.ItemListLimits
import com.basekeita.roulette.ui.uimodel.ItemUiModel
import com.basekeita.roulette.ui.uimodel.MarkUiModel
import com.basekeita.roulette.ui.uimodel.SavedListSummaryUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/** 指定直後だけ印を見せる時間。以降はリストに触れない限り痕跡を残さない。 */
private const val TARGET_HINT_MILLIS = 1_600L

/** 削除した項目を「元に戻す」で戻せる時間。過ぎると削除が確定する。ユーザー補助の設定で長くする（M3 の Snackbar と同じ扱い）。 */
private const val UNDO_MILLIS = 5_000L

/** 見出しのメニューで開いている階層。 */
private enum class ListMenuPage {
    CLOSED,
    ROOT,
    LOAD,
}

/** 「✕」かスワイプで削除した直後の項目。「元に戻す」の Snackbar を出している間だけ持つ。 */
private data class PendingRemoval(
    val id: UUID,
    val label: String,
)

/**
 * 項目の追加・削除・隠しジェスチャの受け口と、印の表示制御。ルーレットと順番決めで共有する。
 *
 * 印は指定した本人だけが確認できればよいので、項目に触れている間と指定直後だけ出す。
 * 演出中と結果表示中（`concealMarks`）、画面がキャプチャされている間は無条件で伏せる。
 *
 * 入力欄は改行区切りでまとめて追加でき、同名の項目があれば追加の前に確かめる（追加は止めない）。
 * 削除した直後は「元に戻す」の Snackbar を 5 秒出す。演出が始まる・すべて削除する・リストを読み込むと、取り消せなくなる。
 *
 * @param marks 項目の id ごとの指定。伏せるかどうかはこの部品が決めるので、指定はそのまま渡す。
 * @param busy 演出中。入力と隠しジェスチャを止める。
 * @param concealMarks 印を無条件に伏せる。演出中と結果表示中に立てる。
 * @param screenCaptured 画面が録画・ミラーリングされている。
 * @param snackbarHostState 「元に戻す」を出す Snackbar。画面の枠（`PageFrame`）が下端に置く。
 * @param onAdd 入力を行ごとに正規化したラベル。上限で切り捨てるのは呼び出し側。
 * @param onRemove 「✕」かスワイプで削除した項目。
 * @param onUndoRemove 直前に削除した項目を元に戻す。
 * @param onSaveList 入力された名前で、いまの項目を保存する。
 * @param onLoadList 保存したリストで項目を置き換える。
 */
@Composable
fun ItemList(
    items: List<ItemUiModel>,
    marks: Map<UUID, MarkUiModel>,
    savedLists: List<SavedListSummaryUiModel>,
    limits: ItemListLimits,
    busy: Boolean,
    concealMarks: Boolean,
    screenCaptured: Boolean,
    snackbarHostState: SnackbarHostState,
    onAdd: (List<String>) -> Unit,
    onRemove: (UUID) -> Unit,
    onUndoRemove: (UUID) -> Unit,
    onRemoveAll: () -> Unit,
    onLongPress: (UUID) -> Unit,
    onSaveList: (String) -> Unit,
    onLoadList: (UUID) -> Unit,
    onManageListsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = DekiTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val accessibilityManager = LocalAccessibilityManager.current
    val inputFocusRequester = remember { FocusRequester() }

    var input by rememberSaveable { mutableStateOf("") }
    var pressingCount by remember { mutableIntStateOf(0) }
    var hintCount by remember { mutableIntStateOf(0) }
    var hinting by remember { mutableStateOf(false) }
    var confirmingRemoveAll by remember { mutableStateOf(false) }
    var savingList by remember { mutableStateOf(false) }
    var listsFull by remember { mutableStateOf(false) }
    // 同名の確認ダイアログで「追加する」を待っているラベル。ダイアログを出している間だけ持つ
    var pendingDuplicateLines by remember { mutableStateOf<List<String>?>(null) }
    var pendingRemoval by remember { mutableStateOf<PendingRemoval?>(null) }
    var removalCount by remember { mutableIntStateOf(0) }
    // 上限で切り捨てた分がある。上限の注記が現れたときに読み上げる
    var truncatedByCapacity by remember { mutableStateOf(false) }

    val lines = ItemLabelInput.splitLines(input, limits.maxLabelLength)
    val atCapacity = items.size >= limits.maxItemCount
    val inputDisabled = busy || atCapacity
    val revealMarks = MarkVisibility.reveals(
        concealed = concealMarks,
        captured = screenCaptured,
        pressing = pressingCount > 0,
        hinting = hinting,
    )
    val dismissUndo = {
        pendingRemoval = null
        snackbarHostState.currentSnackbarData?.dismiss()
    }
    val removedMessage = pendingRemoval?.let { removed -> stringResource(R.string.item_list_removed_snackbar, removed.label) }
    val undoLabel = stringResource(R.string.item_list_undo)
    val currentOnUndoRemove by rememberUpdatedState(onUndoRemove)

    // 指定するたびに表示時間を延ばす。前の指定の待ちは取り消す
    LaunchedEffect(hintCount) {
        if (hintCount == 0) return@LaunchedEffect

        hinting = true
        delay(TARGET_HINT_MILLIS)
        hinting = false
    }

    // 削除するたびに「元に戻す」の Snackbar を出し直す。直前の削除が残っていればそれは確定し、新しい削除に置き換える
    // （多段の取り消しは持たない）。期限はユーザー補助の設定に合わせて延ばす
    LaunchedEffect(removalCount) {
        val removed = pendingRemoval ?: return@LaunchedEffect

        val message = removedMessage ?: return@LaunchedEffect

        val timeoutMillis = accessibilityManager?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = UNDO_MILLIS,
            containsText = true,
            containsControls = true,
        ) ?: UNDO_MILLIS
        val result = withTimeoutOrNull(timeoutMillis) {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Indefinite,
            )
        }
        if (pendingRemoval != removed) return@LaunchedEffect

        pendingRemoval = null
        if (result == SnackbarResult.ActionPerformed) currentOnUndoRemove(removed.id)
    }

    // 上限に達したり演出が始まったりして入力できなくなったら、開いたままのキーボードを閉じる
    LaunchedEffect(inputDisabled) {
        if (inputDisabled) focusManager.clearFocus()
    }

    // スピン／並べ替えを始めたら取り消せなくする（結果と項目リストの整合を保つため）
    LaunchedEffect(busy) {
        if (busy) dismissUndo()
    }

    // 上限を下回ったら、次に切り捨てたときにまた読み上げる
    LaunchedEffect(atCapacity) {
        if (!atCapacity) truncatedByCapacity = false
    }

    val add = add@{ labels: List<String> ->
        if (inputDisabled) return@add

        if (labels.size > limits.maxItemCount - items.size) truncatedByCapacity = true
        onAdd(labels)
        input = ""
        // 続けて次の項目を入力できるようにフォーカスを保つ
        inputFocusRequester.requestFocus()
    }

    val submitInput = submit@{
        if (inputDisabled || pendingDuplicateLines != null) return@submit

        if (lines.isEmpty()) {
            input = ""
            return@submit
        }

        // 同名の項目は追加を止めず、確かめるだけ。読み込み・元に戻すでは確かめない
        if (DuplicateLabels.conflicts(lines, items.map { item -> item.label }, limits.maxLabelLength)) {
            pendingDuplicateLines = lines
            return@submit
        }

        add(lines)
    }

    val removeItem = { id: UUID ->
        val label = items.firstOrNull { item -> item.id == id }?.label
        onRemove(id)
        if (label != null) {
            pendingRemoval = PendingRemoval(id = id, label = label)
            removalCount += 1
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ItemListHeader(
            itemCount = items.size,
            maxItemCount = limits.maxItemCount,
            busy = busy,
            savedLists = savedLists,
            onRemoveAllClick = { confirmingRemoveAll = true },
            onSaveListClick = {
                if (savedLists.size >= limits.maxSavedListCount) {
                    listsFull = true
                } else {
                    savingList = true
                }
            },
            onLoadList = { id ->
                // 置き換える前のリストから消した項目を、読み込んだリストへ戻せないようにする
                dismissUndo()
                onLoadList(id)
            },
            onManageListsClick = onManageListsClick,
        )

        AddItemForm(
            input = input,
            onInputChange = { newValue -> input = ItemLabelInput.clampLines(newValue, limits.maxLabelLength) },
            enabled = !inputDisabled,
            lineCount = lines.size,
            onSubmit = submitInput,
            focusRequester = inputFocusRequester,
            modifier = Modifier.fillMaxWidth(),
        )

        if (items.isEmpty()) {
            DashedBorderBox(
                cornerRadius = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.item_list_empty),
                    style = DekiTypography.subheadline,
                    color = colorScheme.muted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEachIndexed { index, item ->
                    val mark = marks[item.id]
                    // key で行の状態（押している最中のジェスチャ・スワイプの位置）を項目に結び付ける
                    key(item.id) {
                        ItemRow(
                            item = item,
                            color = colorScheme.sliceAccent(index, items.size),
                            mark = mark.takeIf { revealMarks },
                            busy = busy,
                            onPressingChange = { pressing ->
                                if (pressing) {
                                    pressingCount += 1
                                } else {
                                    pressingCount -= 1
                                }
                            },
                            onLongPress = {
                                onLongPress(item.id)
                                hintCount += 1
                            },
                            onRemove = { removeItem(item.id) },
                        )
                    }
                }
            }
        }

        if (items.size < limits.minItemCount) {
            Text(
                text = stringResource(R.string.item_list_need_more_items),
                style = DekiTypography.caption,
                color = colorScheme.flareText,
            )
        }

        if (atCapacity) {
            val capacityMessage = stringResource(R.string.item_list_at_capacity, limits.maxItemCount)
            Text(
                text = capacityMessage,
                style = DekiTypography.caption,
                color = colorScheme.muted,
                // まとめて追加して上限で切り捨てたときは、現れた注記を読み上げる（同じ内容を伝える）
                modifier = Modifier.semantics {
                    if (truncatedByCapacity) paneTitle = capacityMessage
                },
            )
        }
    }

    val duplicateLines = pendingDuplicateLines
    if (duplicateLines != null) {
        DuplicateAddDialog(
            onConfirm = {
                pendingDuplicateLines = null
                add(duplicateLines)
            },
            onDismiss = {
                pendingDuplicateLines = null
                // 「やめる」で戻ったときは、そのまま続きを直せるようにする
                inputFocusRequester.requestFocus()
            },
        )
    }

    if (confirmingRemoveAll) {
        RemoveAllConfirmDialog(
            onConfirm = {
                confirmingRemoveAll = false
                dismissUndo()
                onRemoveAll()
            },
            onDismiss = { confirmingRemoveAll = false },
        )
    }

    if (savingList) {
        SaveListDialog(
            onSave = { name ->
                savingList = false
                onSaveList(name)
            },
            onDismiss = { savingList = false },
        )
    }

    if (listsFull) {
        SavedListsFullDialog(
            maxSavedListCount = limits.maxSavedListCount,
            onDismiss = { listsFull = false },
        )
    }
}

/** 見出し行。「項目」、すべて削除、件数、メニュー。 */
@Composable
private fun ItemListHeader(
    itemCount: Int,
    maxItemCount: Int,
    busy: Boolean,
    savedLists: List<SavedListSummaryUiModel>,
    onRemoveAllClick: () -> Unit,
    onSaveListClick: () -> Unit,
    onLoadList: (UUID) -> Unit,
    onManageListsClick: () -> Unit,
) {
    val colorScheme = DekiTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.item_list_title),
            style = DekiTypography.callout.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.ivory,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (itemCount > 0 && !busy) {
            TextButton(onClick = onRemoveAllClick) {
                Text(
                    text = stringResource(R.string.item_list_remove_all),
                    style = DekiTypography.caption.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.muted,
                )
            }
        }
        Text(
            text = stringResource(R.string.item_list_count, itemCount, maxItemCount),
            // 件数が変わっても幅が揺れないよう、数字を等幅にする
            style = DekiTypography.caption.copy(fontFeatureSettings = "tnum"),
            color = colorScheme.muted,
        )
        ListMenu(
            enabled = !busy,
            canSave = itemCount > 0,
            savedLists = savedLists,
            onSaveListClick = onSaveListClick,
            onLoadList = onLoadList,
            onManageListsClick = onManageListsClick,
        )
    }
}

/** 見出し行のメニュー。保存・読み込み・管理だけを置き、行本体の挙動には触れない。 */
@Composable
private fun ListMenu(
    enabled: Boolean,
    canSave: Boolean,
    savedLists: List<SavedListSummaryUiModel>,
    onSaveListClick: () -> Unit,
    onLoadList: (UUID) -> Unit,
    onManageListsClick: () -> Unit,
) {
    val colorScheme = DekiTheme.colorScheme
    var page by remember { mutableStateOf(ListMenuPage.CLOSED) }
    val closeMenu = { page = ListMenuPage.CLOSED }

    Box {
        IconButton(
            onClick = { page = ListMenuPage.ROOT },
            enabled = enabled,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.list_menu_label),
                tint = colorScheme.muted,
            )
        }
        DropdownMenu(
            expanded = page != ListMenuPage.CLOSED,
            onDismissRequest = closeMenu,
        ) {
            if (page == ListMenuPage.ROOT) {
                MenuItem(
                    text = stringResource(R.string.list_menu_save),
                    iconRes = R.drawable.ic_save,
                    enabled = canSave,
                    onClick = {
                        closeMenu()
                        onSaveListClick()
                    },
                )
                MenuItem(
                    text = stringResource(R.string.list_menu_load),
                    iconRes = R.drawable.ic_folder_open,
                    onClick = { page = ListMenuPage.LOAD },
                )
                MenuItem(
                    text = stringResource(R.string.list_menu_manage),
                    iconRes = R.drawable.ic_list_alt,
                    onClick = {
                        closeMenu()
                        onManageListsClick()
                    },
                )
            } else if (savedLists.isEmpty()) {
                MenuItem(
                    text = stringResource(R.string.list_menu_no_saved_lists),
                    enabled = false,
                    onClick = {},
                )
            } else {
                savedLists.forEach { savedList ->
                    MenuItem(
                        text = savedList.name,
                        supportingText = pluralStringResource(
                            R.plurals.saved_list_item_count,
                            savedList.itemCount,
                            savedList.itemCount,
                        ),
                        onClick = {
                            closeMenu()
                            onLoadList(savedList.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null,
    @DrawableRes iconRes: Int? = null,
) {
    val colorScheme = DekiTheme.colorScheme
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    text = text,
                    style = DekiTypography.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = DekiTypography.footnote,
                        color = colorScheme.muted,
                    )
                }
            }
        },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = iconRes?.let { res ->
            { Icon(painter = painterResource(res), contentDescription = null) }
        },
    )
}

/** 同名の項目を追加しようとしたときの確認。追加は止めず、確かめるだけ。 */
@Composable
private fun DuplicateAddDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.duplicate_add_title)) },
        text = { Text(text = stringResource(R.string.duplicate_add_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.duplicate_add_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.duplicate_add_cancel))
            }
        },
    )
}

@Composable
private fun RemoveAllConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = DekiTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.item_list_remove_all_confirm_title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.item_list_remove_all), color = colorScheme.flareText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

/** リストに名前を付けて保存する。名前が空白だけのうちは保存できない。 */
@Composable
private fun SaveListDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = DekiTheme.colorScheme
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.save_list_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.save_list_message),
                    style = DekiTypography.subheadline,
                    color = colorScheme.muted,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue -> name = newValue },
                    placeholder = { Text(text = stringResource(R.string.save_list_name_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(280.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SavedListsFullDialog(
    maxSavedListCount: Int,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.saved_lists_full_title)) },
        text = { Text(text = stringResource(R.string.saved_lists_full_message, maxSavedListCount)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        },
    )
}

private val previewLimits = ItemListLimits(
    minItemCount = 2,
    maxItemCount = 24,
    maxLabelLength = 20,
    maxSavedListCount = 20,
)

@Preview(widthDp = 360)
@Composable
private fun ItemListPreview() {
    val items = listOf("ラーメン", "カレー", "寿司", "焼肉").map { label ->
        ItemUiModel(id = UUID.randomUUID(), label = label, isDuplicate = false)
    }
    DekiTheme {
        ItemList(
            items = items,
            marks = mapOf(items[1].id to MarkUiModel.TARGET),
            savedLists = emptyList(),
            limits = previewLimits,
            busy = false,
            concealMarks = false,
            screenCaptured = false,
            snackbarHostState = remember { SnackbarHostState() },
            onAdd = {},
            onRemove = {},
            onUndoRemove = {},
            onRemoveAll = {},
            onLongPress = {},
            onSaveList = {},
            onLoadList = {},
            onManageListsClick = {},
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun ItemListEmptyPreview() {
    DekiTheme {
        ItemList(
            items = emptyList(),
            marks = emptyMap(),
            savedLists = emptyList(),
            limits = previewLimits,
            busy = false,
            concealMarks = false,
            screenCaptured = false,
            snackbarHostState = remember { SnackbarHostState() },
            onAdd = {},
            onRemove = {},
            onUndoRemove = {},
            onRemoveAll = {},
            onLongPress = {},
            onSaveList = {},
            onLoadList = {},
            onManageListsClick = {},
        )
    }
}
