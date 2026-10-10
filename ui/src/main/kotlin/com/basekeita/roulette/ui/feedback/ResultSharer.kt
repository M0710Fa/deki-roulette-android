package com.basekeita.roulette.ui.feedback

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** 結果のコピーと共有。本文は呼び出し側が組み立てる（`ResultText`）。 */
interface ResultSharer {
    /** クリップボードに書き込む。 */
    fun copy(text: String)

    /** 共有の一覧（`ACTION_SEND`）を開く。 */
    fun share(text: String)
}

internal class ResultSharerImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val feedbackHost: FeedbackHost,
) : ResultSharer {
    override fun copy(text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
    }

    override fun share(text: String) {
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        val chooser = Intent.createChooser(send, null)
        // 画面が見えていればその画面から開く。見えていなければ（通常は起きない）新しいタスクで開く
        val activity = feedbackHost.startedActivity
        if (activity != null) {
            activity.startActivity(chooser)
        } else {
            context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
