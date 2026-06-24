package com.dicteditor.percynguyen92.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun getFileName(context: Context, uri: Uri): String {
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrEmpty()) {
                        return name
                    }
                }
            }
        } catch (e: Throwable) {
            // ignore
        }
    }
    val path = uri.path ?: return uri.lastPathSegment ?: uri.toString()
    val decoded = try { Uri.decode(path) } catch (e: Exception) { path }
    val afterSlash = decoded.substringAfterLast('/')
    val afterColon = afterSlash.substringAfterLast(':')
    return if (afterColon.isNotEmpty()) afterColon else afterSlash
}
