package com.ladsers.passtable.android.components

import android.content.pm.PackageManager
import android.os.Build

object PackageFinder {

    const val packageNameRuStore = "ru.vk.store"

    fun PackageManager.isPackageInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                ).enabled
            } else {
                @Suppress("DEPRECATION")
                this.getApplicationInfo(packageName, 0).enabled
            }
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}