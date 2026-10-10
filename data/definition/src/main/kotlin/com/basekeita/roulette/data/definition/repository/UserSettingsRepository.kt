package com.basekeita.roulette.data.definition.repository

import com.basekeita.roulette.data.definition.model.GlowStyle
import com.basekeita.roulette.data.definition.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setHapticsEnabled(enabled: Boolean)

    suspend fun setSoundEnabled(enabled: Boolean)

    suspend fun setGlowStyle(style: GlowStyle)
}
