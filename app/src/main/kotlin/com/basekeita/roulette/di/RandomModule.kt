package com.basekeita.roulette.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/** UseCase が使う乱数。結果と止まる位置を決めるので、予測しにくい `SecureRandom` を使う（テストでは `Random(seed)` に差し替える）。 */
@Module
@InstallIn(SingletonComponent::class)
object RandomModule {
    @Provides
    @Singleton
    fun providesRandom(): Random = SecureRandom().asKotlinRandom()
}
