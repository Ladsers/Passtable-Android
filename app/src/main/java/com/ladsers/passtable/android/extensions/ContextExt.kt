package com.ladsers.passtable.android.extensions

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

@SuppressLint("Range")
fun Context.getFileNameWithExt(uri: Uri): String?{
    if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
    return try {
        contentResolver.query(uri, null, null, null, null)?.let { cursor ->
            cursor.run {
                if (moveToFirst()) getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))
                else null
            }.also { cursor.close() }
        }
    } catch (e : Exception) {
        null
    }
}

fun Context.getFileName(uri: Uri): String?{
    return getFileNameWithExt(uri)?.substringBeforeLast('.')
}