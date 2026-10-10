package com.basekeita.roulette.data.impl.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.SavedList
import com.basekeita.roulette.local.DekiLocalDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SavedListsRepositoryImplTest {
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

    private val repository = SavedListsRepositoryImpl(DekiLocalDataSource(dataStore))

    private val items = listOf("ラーメン", "カレー", "寿司").map { Item(label = it) }

    private suspend fun currentLists(): List<SavedList> = repository.lists.first()

    // 追加・変更・削除

    @Test
    fun `追加は正規化した名前で末尾に足す`() = testScope.runTest {
        assertTrue(repository.save(" ランチ ", items))
        assertTrue(repository.save("チーム", emptyList()))

        val lists = currentLists()
        assertEquals(listOf("ランチ", "チーム"), lists.map { it.name })
        assertEquals(items, lists.first().items)
    }

    @Test
    fun `空の名前では追加しない`() = testScope.runTest {
        assertFalse(repository.save("   ", items))
        assertEquals(emptyList<SavedList>(), currentLists())
    }

    @Test
    fun `上限に達していたら追加しない`() = testScope.runTest {
        repeat(SavedList.MAX_COUNT) { index -> assertTrue(repository.save("$index", items)) }

        assertFalse(repository.save("もう 1 つ", items))
        assertEquals(SavedList.MAX_COUNT, currentLists().size)
    }

    @Test
    fun `名前を変更できる`() = testScope.runTest {
        repository.save("ランチ", items)
        repository.save("チーム", emptyList())
        val original = currentLists()

        assertTrue(repository.rename(original.first().id, " 夕飯 "))

        val renamed = currentLists()
        assertEquals(listOf("夕飯", "チーム"), renamed.map { it.name })
        assertEquals(original.first().id, renamed.first().id)
        assertEquals(items, renamed.first().items)
    }

    @Test
    fun `空の名前や無いidには変更しない`() = testScope.runTest {
        repository.save("ランチ", items)
        val original = currentLists()

        assertFalse(repository.rename(original.first().id, ""))
        assertFalse(repository.rename(UUID.randomUUID(), "夕飯"))
        assertEquals(original, currentLists())
    }

    @Test
    fun `削除はidで選ぶ`() = testScope.runTest {
        repository.save("A", items)
        repository.save("B", items)
        val original = currentLists()

        assertFalse(repository.delete(UUID.randomUUID()))
        assertEquals(original, currentLists())

        assertTrue(repository.delete(original.first().id))
        assertEquals(listOf("B"), currentLists().map { it.name })
    }

    // 読み込み

    @Test
    fun `保存データが無ければ空`() = testScope.runTest {
        assertEquals(emptyList<SavedList>(), currentLists())
    }

    @Test
    fun `壊れたデータは空として扱い追加はできる`() = testScope.runTest {
        dataStore.edit { it[stringPreferencesKey("savedLists")] = "broken" }
        assertEquals(emptyList<SavedList>(), currentLists())

        assertTrue(repository.save("ランチ", items))
        assertEquals(listOf("ランチ"), currentLists().map { it.name })
    }

    @Test
    fun `読み込み時に名前と項目を整える`() = testScope.runTest {
        dataStore.edit {
            it[stringPreferencesKey("savedLists")] =
                """
                [
                  {"id":"00000000-0000-0000-0000-000000000001","name":"  ランチ  ","items":[
                    {"id":"00000000-0000-0000-0000-000000000011","label":" A  B "},
                    {"id":"00000000-0000-0000-0000-000000000012","label":"   "}
                  ]},
                  {"id":"00000000-0000-0000-0000-000000000002","name":"   ","items":[]},
                  {"id":"00000000-0000-0000-0000-000000000001","name":"重複","items":[]}
                ]
                """.trimIndent()
        }

        val lists = currentLists()
        assertEquals(1, lists.size)
        assertEquals("ランチ", lists.first().name)
        assertEquals(listOf("A B"), lists.first().items.map { it.label })
    }

    @Test
    fun `上限を超えるリストは切り捨てる`() = testScope.runTest {
        val storedLists =
            (0 until SavedList.MAX_COUNT + 3).joinToString(prefix = "[", postfix = "]") { index ->
                """{"id":"${UUID.randomUUID()}","name":"$index","items":[]}"""
            }
        dataStore.edit { it[stringPreferencesKey("savedLists")] = storedLists }

        assertEquals(SavedList.MAX_COUNT, currentLists().size)
    }
}
