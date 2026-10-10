package com.basekeita.roulette.ui.feedback

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FeedbackModule {
    @Binds
    abstract fun bindHapticPlayer(impl: HapticPlayerImpl): HapticPlayer

    @Binds
    abstract fun bindSpinSoundPlayer(impl: SpinSoundPlayerImpl): SpinSoundPlayer

    @Binds
    abstract fun bindScreenCaptureMonitor(impl: ScreenCaptureMonitorImpl): ScreenCaptureMonitor

    @Binds
    abstract fun bindReduceMotionMonitor(impl: ReduceMotionMonitorImpl): ReduceMotionMonitor

    @Binds
    abstract fun bindResultSharer(impl: ResultSharerImpl): ResultSharer
}
