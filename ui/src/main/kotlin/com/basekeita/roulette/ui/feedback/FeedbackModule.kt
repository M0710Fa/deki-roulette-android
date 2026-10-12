package com.basekeita.roulette.ui.feedback

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FeedbackModule {
    @Binds
    abstract fun bindsHapticPlayer(impl: HapticPlayerImpl): HapticPlayer

    @Binds
    abstract fun bindsSpinSoundPlayer(impl: SpinSoundPlayerImpl): SpinSoundPlayer

    @Binds
    abstract fun bindsScreenCaptureMonitor(impl: ScreenCaptureMonitorImpl): ScreenCaptureMonitor

    @Binds
    abstract fun bindsReduceMotionMonitor(impl: ReduceMotionMonitorImpl): ReduceMotionMonitor

    @Binds
    abstract fun bindsResultSharer(impl: ResultSharerImpl): ResultSharer
}
