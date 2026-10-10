package com.basekeita.roulette.local

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DekiLocalDataSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    // ファイルを渡す create は、JVM のテストでは Build.VERSION.SDK_INT が 0 になり File.renameTo で書き換えるため、
    // Windows では既存のファイルを置き換えられず 2 回目の書き込みが失敗する。OkioStorage は置き換えて書くため、テストではこちらを使う。
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

    private val dataSource = DekiLocalDataSource(dataStore)

    private suspend fun writeRaw(
        name: String,
        value: String,
    ) {
        dataStore.edit { it[stringPreferencesKey(name)] = value }
    }

    private suspend fun readRaw(name: String): String? = dataStore.data.first()[stringPreferencesKey(name)]

    // 項目

    @Test
    fun `保存した項目はidとラベルごと戻る`() = testScope.runTest {
        val items = listOf(StoredItem(UUID.randomUUID(), "ラーメン"), StoredItem(UUID.randomUUID(), "カレー"))
        dataSource.saveItems(StoredItemsKey.ROULETTE, items)

        assertEquals(items, dataSource.items(StoredItemsKey.ROULETTE).first())
    }

    @Test
    fun `保存データが無ければnull`() = testScope.runTest {
        assertNull(dataSource.items(StoredItemsKey.ROULETTE).first())
    }

    @Test
    fun `空のリストも保存済みとして扱う`() = testScope.runTest {
        dataSource.saveItems(StoredItemsKey.ORDER, emptyList())

        assertEquals(emptyList<StoredItem>(), dataSource.items(StoredItemsKey.ORDER).first())
    }

    @Test
    fun `壊れたデータはnull`() = testScope.runTest {
        writeRaw("items.roulette", "not json")

        assertNull(dataSource.items(StoredItemsKey.ROULETTE).first())
    }

    @Test
    fun `形の違うJSONはnull`() = testScope.runTest {
        writeRaw("items.roulette", """{"items":[]}""")
        assertNull(dataSource.items(StoredItemsKey.ROULETTE).first())

        writeRaw("items.roulette", """[{"label":"A"}]""")
        assertNull(dataSource.items(StoredItemsKey.ROULETTE).first())
    }

    @Test
    fun `UUIDとして読めないidを含むJSONはnull`() = testScope.runTest {
        writeRaw("items.roulette", """[{"id":"not-a-uuid","label":"A"}]""")

        assertNull(dataSource.items(StoredItemsKey.ROULETTE).first())
    }

    @Test
    fun `消すとnullに戻る`() = testScope.runTest {
        dataSource.saveItems(StoredItemsKey.ROULETTE, listOf(StoredItem(UUID.randomUUID(), "A")))
        dataSource.clearItems(StoredItemsKey.ROULETTE)

        assertNull(dataSource.items(StoredItemsKey.ROULETTE).first())
    }

    @Test
    fun `画面ごとに別のキーへ保存する`() = testScope.runTest {
        val rouletteItems = listOf(StoredItem(UUID.randomUUID(), "A"))
        val orderItems = listOf(StoredItem(UUID.randomUUID(), "B"))
        dataSource.saveItems(StoredItemsKey.ROULETTE, rouletteItems)
        dataSource.saveItems(StoredItemsKey.ORDER, orderItems)

        assertEquals(rouletteItems, dataSource.items(StoredItemsKey.ROULETTE).first())
        assertEquals(orderItems, dataSource.items(StoredItemsKey.ORDER).first())
        assertEquals(setOf("items.roulette", "items.order"), dataStore.data.first().asMap().keys.map { it.name }.toSet())
    }

    @Test
    fun `項目の保存形式はidとラベルだけ`() = testScope.runTest {
        val item = StoredItem(UUID.randomUUID(), "A")
        dataSource.saveItems(StoredItemsKey.ROULETTE, listOf(item))

        val entry = Json.parseToJsonElement(readRaw("items.roulette")!!).jsonArray.first().jsonObject
        assertEquals(setOf("id", "label"), entry.keys)
        assertEquals(item.id.toString(), entry.getValue("id").jsonPrimitive.content)
    }

    // 保存したリスト

    @Test
    fun `保存したリストは名前と項目ごと戻る`() = testScope.runTest {
        val lists =
            listOf(
                StoredSavedList(UUID.randomUUID(), "ランチ", listOf(StoredItem(UUID.randomUUID(), "ラーメン"))),
                StoredSavedList(UUID.randomUUID(), "チーム", emptyList()),
            )
        dataSource.updateSavedLists { lists }

        assertEquals(lists, dataSource.savedLists.first())
    }

    @Test
    fun `保存したリストが無ければnullで壊れていてもnull`() = testScope.runTest {
        assertNull(dataSource.savedLists.first())

        writeRaw("savedLists", "broken")
        assertNull(dataSource.savedLists.first())
    }

    @Test
    fun `更新には今の値を渡しnullを返したら書き込まない`() = testScope.runTest {
        val first = StoredSavedList(UUID.randomUUID(), "A", emptyList())
        dataSource.updateSavedLists { current ->
            assertNull(current)
            listOf(first)
        }

        var received: List<StoredSavedList>? = null
        dataSource.updateSavedLists { current ->
            received = current
            null
        }

        assertEquals(listOf(first), received)
        assertEquals(listOf(first), dataSource.savedLists.first())
    }

    @Test
    fun `保存したリストの保存形式に指定は含まれない`() = testScope.runTest {
        val list = StoredSavedList(UUID.randomUUID(), "A", listOf(StoredItem(UUID.randomUUID(), "B")))
        dataSource.updateSavedLists { listOf(list) }

        val entry = Json.parseToJsonElement(readRaw("savedLists")!!).jsonArray.first().jsonObject
        assertEquals(setOf("id", "name", "items"), entry.keys)
        assertEquals(setOf("id", "label"), entry.getValue("items").jsonArray.first().jsonObject.keys)
    }

    // 設定

    @Test
    fun `設定は未設定ならnullで保存した値が戻る`() = testScope.runTest {
        assertNull(dataSource.hapticsEnabled.first())
        assertNull(dataSource.soundEnabled.first())
        assertNull(dataSource.glowStyle.first())

        dataSource.setHapticsEnabled(false)
        dataSource.setSoundEnabled(false)
        dataSource.setGlowStyle("rainbow")

        assertEquals(false, dataSource.hapticsEnabled.first())
        assertEquals(false, dataSource.soundEnabled.first())
        assertEquals("rainbow", dataSource.glowStyle.first())
        assertEquals(
            setOf("hapticsEnabled", "soundEnabled", "glowStyle"),
            dataStore.data.first().asMap().keys.map { it.name }.toSet(),
        )
    }
}
