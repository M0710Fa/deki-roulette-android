package com.basekeita.roulette.data.impl.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.impl.DefaultItemLabels
import com.basekeita.roulette.local.DekiLocalDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ItemsRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    // ファイルを渡す create は JVM のテストでは File.renameTo で書き換え、Windows では既存のファイルを置き換えられない。
    // OkioStorage は置き換えて書くため、テストではこちらを使う。
    private val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            storage =
            OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
                temporaryFolder.root.resolve("test.preferences_pb").toOkioPath()
            },
            corruptionHandler = null,
            migrations = emptyList(),
            scope = testScope.backgroundScope,
        )

    /** 言語ごとの初期項目の代わり。`labelsByKind` を差し替えると言語の変更にあたる。 */
    private class FakeDefaultItemLabels(
        var labelsByKind: Map<ListKind, List<String>>,
    ) : DefaultItemLabels {
        override fun labels(kind: ListKind): List<String> = labelsByKind.getValue(kind)
    }

    private val defaultItemLabels =
        FakeDefaultItemLabels(mapOf(ListKind.ROULETTE to listOf("X", "Y"), ListKind.ORDER to listOf("P", "Q")))

    private val repository = ItemsRepositoryImpl(DekiLocalDataSource(dataStore), defaultItemLabels)

    private suspend fun writeRaw(
        name: String,
        value: String,
    ) {
        dataStore.edit { it[stringPreferencesKey(name)] = value }
    }

    private suspend fun labels(kind: ListKind): List<String> = repository.items(kind).first().map { it.label }

    @Test
    fun `保存した項目はidとラベルごと戻る`() = testScope.runTest {
        val items = listOf("ラーメン", "カレー", "寿司").map { Item(label = it) }
        repository.save(ListKind.ROULETTE, items)

        assertEquals(items, repository.items(ListKind.ROULETTE).first())
    }

    @Test
    fun `保存データが無ければ画面ごとの初期項目`() = testScope.runTest {
        assertEquals(listOf("X", "Y"), labels(ListKind.ROULETTE))
        assertEquals(listOf("P", "Q"), labels(ListKind.ORDER))
    }

    @Test
    fun `保存データが無い間は読み直しても同じidの初期項目を返す`() = testScope.runTest {
        val first = repository.items(ListKind.ROULETTE).first()
        val second = repository.items(ListKind.ROULETTE).first()

        assertEquals(first, second)
    }

    @Test
    fun `初期項目は読むたびにそのときの言語で評価する`() = testScope.runTest {
        assertEquals(listOf("X", "Y"), labels(ListKind.ROULETTE))

        defaultItemLabels.labelsByKind = mapOf(ListKind.ROULETTE to listOf("Z"), ListKind.ORDER to emptyList())

        assertEquals(listOf("Z"), labels(ListKind.ROULETTE))
    }

    @Test
    fun `空のリストも保存済みとして扱う`() = testScope.runTest {
        repository.save(ListKind.ROULETTE, emptyList())

        assertEquals(emptyList<Item>(), repository.items(ListKind.ROULETTE).first())
    }

    @Test
    fun `壊れたデータは初期項目に戻す`() = testScope.runTest {
        writeRaw("items.roulette", "not json")

        assertEquals(listOf("X", "Y"), labels(ListKind.ROULETTE))
    }

    @Test
    fun `消すと初期項目に戻る`() = testScope.runTest {
        repository.save(ListKind.ROULETTE, listOf(Item(label = "A")))
        repository.reset(ListKind.ROULETTE)

        assertEquals(listOf("X", "Y"), labels(ListKind.ROULETTE))
    }

    @Test
    fun `画面ごとに別のキーへ保存する`() = testScope.runTest {
        repository.save(ListKind.ROULETTE, listOf(Item(label = "A")))
        repository.save(ListKind.ORDER, listOf(Item(label = "B")))

        assertEquals(listOf("A"), labels(ListKind.ROULETTE))
        assertEquals(listOf("B"), labels(ListKind.ORDER))
    }

    @Test
    fun `読み込み時にラベルを正規化し空になったものは落とす`() = testScope.runTest {
        val longLabel = "あ".repeat(30)
        writeRaw(
            "items.roulette",
            """
            [
              {"id":"00000000-0000-0000-0000-000000000001","label":"  A   B  "},
              {"id":"00000000-0000-0000-0000-000000000002","label":"   "},
              {"id":"00000000-0000-0000-0000-000000000003","label":"$longLabel"}
            ]
            """.trimIndent(),
        )

        assertEquals(listOf("A B", "あ".repeat(Item.MAX_LABEL_LENGTH)), labels(ListKind.ROULETTE))
    }

    @Test
    fun `idが重複する項目は最初のものだけ残す`() = testScope.runTest {
        writeRaw(
            "items.roulette",
            """
            [
              {"id":"00000000-0000-0000-0000-000000000001","label":"A"},
              {"id":"00000000-0000-0000-0000-000000000001","label":"B"},
              {"id":"00000000-0000-0000-0000-000000000002","label":"C"}
            ]
            """.trimIndent(),
        )

        assertEquals(listOf("A", "C"), labels(ListKind.ROULETTE))
    }

    @Test
    fun `上限を超える項目は切り捨てる`() = testScope.runTest {
        val items = (0 until Item.MAX_COUNT + 5).map { Item(label = "$it") }
        repository.save(ListKind.ROULETTE, items)

        val loaded = labels(ListKind.ROULETTE)
        assertEquals(Item.MAX_COUNT, loaded.size)
        assertEquals("${Item.MAX_COUNT - 1}", loaded.last())
    }
}
