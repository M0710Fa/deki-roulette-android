package com.basekeita.roulette.data.impl.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.basekeita.roulette.data.definition.model.GlowStyle
import com.basekeita.roulette.data.definition.model.UserSettings
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
class UserSettingsRepositoryImplTest {
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

    private val repository = UserSettingsRepositoryImpl(DekiLocalDataSource(dataStore))

    @Test
    fun `未設定なら触覚と効果音はONで光彩は既定の色`() = testScope.runTest {
        assertEquals(UserSettings(hapticsEnabled = true, soundEnabled = true, glowStyle = GlowStyle.WHITE), repository.settings.first())
    }

    @Test
    fun `変えた値が戻る`() = testScope.runTest {
        repository.setHapticsEnabled(false)
        repository.setSoundEnabled(false)
        repository.setGlowStyle(GlowStyle.RAINBOW)

        assertEquals(UserSettings(hapticsEnabled = false, soundEnabled = false, glowStyle = GlowStyle.RAINBOW), repository.settings.first())
    }

    @Test
    fun `光彩の色はiOSのrawValueと同じ値で保存する`() = testScope.runTest {
        val storedValues =
            GlowStyle.entries.map { style ->
                repository.setGlowStyle(style)
                dataStore.data.first()[stringPreferencesKey("glowStyle")]
            }

        assertEquals(listOf("white", "slice", "rainbow"), storedValues)
    }

    @Test
    fun `知らない光彩の保存値は既定の色に戻す`() = testScope.runTest {
        dataStore.edit { it[stringPreferencesKey("glowStyle")] = "aurora" }

        assertEquals(GlowStyle.DEFAULT, repository.settings.first().glowStyle)
    }
}
