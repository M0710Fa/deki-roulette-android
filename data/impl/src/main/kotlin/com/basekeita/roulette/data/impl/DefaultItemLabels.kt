package com.basekeita.roulette.data.impl

import android.content.Context
import com.basekeita.roulette.data.definition.model.ListKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** 保存データが無い・読めないときの初期項目のラベル。言語の変更に追従するよう、読むたびにそのときの言語で返す。 */
internal interface DefaultItemLabels {
    fun labels(kind: ListKind): List<String>
}

/** `res` の `string-array`（日英）から読む。iOS の `L10n.defaultItems` / `orderDefaultItems` と同じ文言。 */
internal class ResourceDefaultItemLabels
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : DefaultItemLabels {
    override fun labels(kind: ListKind): List<String> {
        val arrayResId =
            when (kind) {
                ListKind.ROULETTE -> R.array.default_items_roulette
                ListKind.ORDER -> R.array.default_items_order
            }
        return context.resources.getStringArray(arrayResId).toList()
    }
}
