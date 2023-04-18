package com.ladsers.passtable.android.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object ClipboardManager {
    fun copy(context: Context, data: String?, toastMsg: String) {
        data ?: return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("data", data)
        clipboard.setPrimaryClip(clip)

        if (toastMsg.isBlank()) return
        Toast.makeText(context.applicationContext, toastMsg, Toast.LENGTH_SHORT).show()
    }
}