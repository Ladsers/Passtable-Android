package com.ladsers.passtable.android.containers

import com.ladsers.passtable.android.BuildConfig

enum class AppStore(
    val packageName: String,
    val appPath: String?
) {
    RUSTORE("ru.vk.store", "rustore://apps.rustore.ru/app/${BuildConfig.APPLICATION_ID}")
}