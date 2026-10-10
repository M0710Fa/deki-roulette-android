package com.basekeita.roulette.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

/**
 * DataStore（Preferences、`deki_prefs`）の読み書き。キー名は iOS の `UserDefaults` と同じ。
 * 値が無い・読めないときは `null` を流し、初期値や既定値は呼ぶ側（data:impl）で決める。
 */
class DekiLocalDataSource
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val preferences: Flow<Preferences> =
        dataStore.data.catch { exception ->
            if (exception !is IOException) throw exception
            emit(emptyPreferences())
        }

    /** 保存済みの項目。保存データが無い、または JSON として読めないときは `null`。空のリストは保存済みとして扱う。 */
    fun items(key: StoredItemsKey): Flow<List<StoredItem>?> = preferences
        .map { it[itemsPreferenceKey(key)] }
        .distinctUntilChanged()
        .map { encoded -> encoded?.let { decodeOrNull<List<StoredItem>>(it) } }

    suspend fun saveItems(
        key: StoredItemsKey,
        items: List<StoredItem>,
    ) {
        dataStore.edit { it[itemsPreferenceKey(key)] = json.encodeToString(items) }
    }

    /** 保存データを消す。以降の `items` は `null` を流す。 */
    suspend fun clearItems(key: StoredItemsKey) {
        dataStore.edit { it.remove(itemsPreferenceKey(key)) }
    }

    /** 保存したリスト。保存データが無い、または JSON として読めないときは `null`。 */
    val savedLists: Flow<List<StoredSavedList>?> =
        preferences
            .map { it[SAVED_LISTS] }
            .distinctUntilChanged()
            .map { encoded -> encoded?.let { decodeOrNull<List<StoredSavedList>>(it) } }

    /**
     * 保存したリストを読んで書き換える。読み書きを 1 回の `edit` で行い、途中で別の書き込みが割り込まないようにする。
     * `transform` には今の値（無い・読めないときは `null`）を渡し、`null` を返したときは書き込まない。
     */
    suspend fun updateSavedLists(transform: (List<StoredSavedList>?) -> List<StoredSavedList>?) {
        dataStore.edit { mutablePreferences ->
            val current = mutablePreferences[SAVED_LISTS]?.let { decodeOrNull<List<StoredSavedList>>(it) }
            val updated = transform(current) ?: return@edit
            mutablePreferences[SAVED_LISTS] = json.encodeToString(updated)
        }
    }

    val hapticsEnabled: Flow<Boolean?> = preferences.map { it[HAPTICS_ENABLED] }.distinctUntilChanged()

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTICS_ENABLED] = enabled }
    }

    val soundEnabled: Flow<Boolean?> = preferences.map { it[SOUND_ENABLED] }.distinctUntilChanged()

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    /** 光彩の色の保存値（iOS の `GlowStyle.rawValue`）。 */
    val glowStyle: Flow<String?> = preferences.map { it[GLOW_STYLE] }.distinctUntilChanged()

    suspend fun setGlowStyle(value: String) {
        dataStore.edit { it[GLOW_STYLE] = value }
    }

    private fun itemsPreferenceKey(key: StoredItemsKey): Preferences.Key<String> = stringPreferencesKey(key.preferenceName)

    /** 形の違う JSON、UUID として読めない id を含む JSON は `null`（iOS の `try? JSONDecoder().decode` と同じ）。 */
    private inline fun <reified T> decodeOrNull(encoded: String): T? = try {
        json.decodeFromString<T>(encoded)
    } catch (exception: IllegalArgumentException) {
        null
    }

    private companion object {
        val SAVED_LISTS = stringPreferencesKey("savedLists")
        val HAPTICS_ENABLED = booleanPreferencesKey("hapticsEnabled")
        val SOUND_ENABLED = booleanPreferencesKey("soundEnabled")
        val GLOW_STYLE = stringPreferencesKey("glowStyle")

        /** iOS の `JSONDecoder` と同じく、知らないキーは読み飛ばす。 */
        val json = Json { ignoreUnknownKeys = true }
    }
}
