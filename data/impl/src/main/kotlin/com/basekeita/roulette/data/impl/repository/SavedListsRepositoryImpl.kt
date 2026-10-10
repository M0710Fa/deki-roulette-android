package com.basekeita.roulette.data.impl.repository

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.SavedList
import com.basekeita.roulette.data.definition.repository.SavedListsRepository
import com.basekeita.roulette.data.impl.toSavedLists
import com.basekeita.roulette.data.impl.toStored
import com.basekeita.roulette.local.DekiLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/** 追加・名前の変更・削除は iOS の `SavedList` の `adding` / `renaming` / `deleting` と同じ規則。 */
internal class SavedListsRepositoryImpl
@Inject
constructor(
    private val localDataSource: DekiLocalDataSource,
) : SavedListsRepository {
    override val lists: Flow<List<SavedList>> =
        localDataSource.savedLists.map { stored -> stored?.toSavedLists().orEmpty() }

    override suspend fun save(
        name: String,
        items: List<Item>,
    ): Boolean {
        val normalizedName = SavedList.normalizeName(name)
        if (normalizedName.isEmpty()) return false

        var saved = false
        localDataSource.updateSavedLists { stored ->
            val current = stored?.toSavedLists().orEmpty()
            if (current.size >= SavedList.MAX_COUNT) return@updateSavedLists null

            saved = true
            (current + SavedList(name = normalizedName, items = items)).map { it.toStored() }
        }
        return saved
    }

    override suspend fun rename(
        id: UUID,
        name: String,
    ): Boolean {
        val normalizedName = SavedList.normalizeName(name)
        if (normalizedName.isEmpty()) return false

        var renamed = false
        localDataSource.updateSavedLists { stored ->
            val current = stored?.toSavedLists().orEmpty()
            if (current.none { it.id == id }) return@updateSavedLists null

            renamed = true
            current.map { list ->
                if (list.id == id) {
                    list.copy(name = normalizedName).toStored()
                } else {
                    list.toStored()
                }
            }
        }
        return renamed
    }

    override suspend fun delete(id: UUID): Boolean {
        var deleted = false
        localDataSource.updateSavedLists { stored ->
            val current = stored?.toSavedLists().orEmpty()
            if (current.none { it.id == id }) return@updateSavedLists null

            deleted = true
            current.filter { it.id != id }.map { it.toStored() }
        }
        return deleted
    }
}
