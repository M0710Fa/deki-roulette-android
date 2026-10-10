package com.basekeita.roulette.data.impl.repository

import com.basekeita.roulette.data.definition.model.GlowStyle
import com.basekeita.roulette.data.definition.model.UserSettings
import com.basekeita.roulette.data.definition.repository.UserSettingsRepository
import com.basekeita.roulette.local.DekiLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

internal class UserSettingsRepositoryImpl
@Inject
constructor(
    private val localDataSource: DekiLocalDataSource,
) : UserSettingsRepository {
    override val settings: Flow<UserSettings> =
        combine(
            localDataSource.hapticsEnabled,
            localDataSource.soundEnabled,
            localDataSource.glowStyle,
        ) { hapticsEnabled, soundEnabled, glowStyle ->
            val defaults = UserSettings()
            UserSettings(
                hapticsEnabled = hapticsEnabled ?: defaults.hapticsEnabled,
                soundEnabled = soundEnabled ?: defaults.soundEnabled,
                glowStyle = glowStyle?.let(::glowStyleOf) ?: defaults.glowStyle,
            )
        }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        localDataSource.setHapticsEnabled(enabled)
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        localDataSource.setSoundEnabled(enabled)
    }

    override suspend fun setGlowStyle(style: GlowStyle) {
        localDataSource.setGlowStyle(style.storedValue)
    }
}

/**
 * 保存値は iOS の `GlowStyle.rawValue` と同じ。enum の名前から作らず書き出しておき、名前を変えても保存済みの値が読めるようにする。
 */
private val GlowStyle.storedValue: String
    get() =
        when (this) {
            GlowStyle.WHITE -> "white"
            GlowStyle.SLICE -> "slice"
            GlowStyle.RAINBOW -> "rainbow"
        }

/** 知らない保存値は `null`（既定の色に戻す）。 */
private fun glowStyleOf(storedValue: String): GlowStyle? = GlowStyle.entries.firstOrNull { it.storedValue == storedValue }
