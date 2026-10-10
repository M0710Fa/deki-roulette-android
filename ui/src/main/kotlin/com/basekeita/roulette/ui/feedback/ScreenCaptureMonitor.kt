package com.basekeita.roulette.ui.feedback

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 画面が録画されているか。録画中は印を伏せる（相手側にも映るため）。
 * API 35 以上だけ検知する（`WindowManager.addScreenRecordingCallback`）。API 30〜34 では録画されていない扱いにする。
 */
interface ScreenCaptureMonitor {
    val isCaptured: StateFlow<Boolean>
}

/** `FeedbackHost` が画面の見えている間だけ `start` / `stop` で登録と解除を行う。 */
@Singleton
internal class ScreenCaptureMonitorImpl
@Inject
constructor() : ScreenCaptureMonitor {
    private val captured = MutableStateFlow(false)
    override val isCaptured: StateFlow<Boolean> = captured.asStateFlow()

    /** 登録中の Activity とその callback。API 35 未満では使わない。 */
    private var registration: Pair<Activity, Consumer<Int>>? = null

    fun start(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

        register(activity)
    }

    fun stop(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

        unregister(activity)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun register(activity: Activity) {
        registration?.let { (registered, _) -> unregister(registered) }
        val callback = Consumer<Int> { state -> captured.value = state == WindowManager.SCREEN_RECORDING_STATE_VISIBLE }
        val windowManager = activity.getSystemService(WindowManager::class.java)
        val initialState = windowManager.addScreenRecordingCallback(activity.mainExecutor, callback)
        captured.value = initialState == WindowManager.SCREEN_RECORDING_STATE_VISIBLE
        registration = activity to callback
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun unregister(activity: Activity) {
        val (registered, callback) = registration ?: return
        if (registered !== activity) return

        registered.getSystemService(WindowManager::class.java).removeScreenRecordingCallback(callback)
        registration = null
        // 見えていない間は録画されても印は映らない。次に見えたときに登録し直して読み直す
        captured.value = false
    }
}
