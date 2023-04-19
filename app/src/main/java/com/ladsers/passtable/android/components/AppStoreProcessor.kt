package com.ladsers.passtable.android.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ladsers.passtable.android.containers.AppStore
import com.ladsers.passtable.android.extensions.isPackageInstalled

object AppStoreProcessor {
    fun isInstalled(context: Context, appStore: AppStore) =
        context.packageManager.isPackageInstalled(appStore.packageName)

    fun open(context: Context, appStore: AppStore) {
        appStore.appPath ?: return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appStore.appPath)))
        } catch (e: ActivityNotFoundException) {
            /* do nothing */
        }
    }
}