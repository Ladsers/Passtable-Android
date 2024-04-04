package com.ladsers.passtable.android.components

import android.app.Activity
import android.content.Intent
import android.net.Uri

object ShareManager {
    fun shareFile(activity: Activity, uri: Uri) {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/octet-stream"
        }
        activity.startActivity(Intent.createChooser(shareIntent, null))
    }
}