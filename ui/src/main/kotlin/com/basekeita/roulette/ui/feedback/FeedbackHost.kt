package com.basekeita.roulette.ui.feedback

import android.app.Activity
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 触覚・共有・画面キャプチャの検知が使う画面（Activity）の受け口。
 *
 * 触覚（`View.performHapticFeedback`）と画面キャプチャの検知（`WindowManager`）は画面に結び付くが、
 * 呼び出し元の ViewModel や Singleton は画面を持てない。MainActivity が `attach` で渡し、ここでは弱参照で持つ。
 * 画面が見えている間（`onStart`〜`onStop`）だけ画面キャプチャの検知を登録する。
 */
@Singleton
class FeedbackHost
@Inject
internal constructor(
    private val screenCaptureMonitor: ScreenCaptureMonitorImpl,
) {
    private var activityReference: WeakReference<Activity>? = null

    /** 画面が見えている間の Activity。見えていなければ null。 */
    internal val startedActivity: Activity?
        get() = activityReference?.get()

    /** 画面のいちばん外側の View。触覚を鳴らすのに使う。 */
    internal val view: View?
        get() = startedActivity?.window?.decorView

    /** Activity の `onCreate` で呼ぶ。以降はその Activity のライフサイクルに合わせて登録と解除を行う。 */
    fun <HostActivity> attach(activity: HostActivity) where HostActivity : Activity, HostActivity : LifecycleOwner {
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    activityReference = WeakReference(activity)
                    screenCaptureMonitor.start(activity)
                }

                override fun onStop(owner: LifecycleOwner) {
                    screenCaptureMonitor.stop(activity)
                    if (activityReference?.get() === activity) activityReference = null
                }
            },
        )
    }
}
