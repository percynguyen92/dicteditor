package com.dicteditor.percynguyen92.utils

import android.content.Context
import androidx.annotation.StringRes

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                if (args.isEmpty()) {
                    context.getString(resId)
                } else {
                    context.getString(resId, *args.toTypedArray())
                }
            }
        }
    }
}
