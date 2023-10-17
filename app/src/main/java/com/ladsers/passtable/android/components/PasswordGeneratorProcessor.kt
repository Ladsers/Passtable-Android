package com.ladsers.passtable.android.components

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.LifecycleOwner
import com.ladsers.passtable.android.contracts.PasswordGeneratorContract

class PasswordGeneratorProcessor(
    activityResultRegistry: ActivityResultRegistry,
    lifecycleOwner: LifecycleOwner,
    callback: (str: String?) -> Unit
) {
    private val activityResultLauncher: ActivityResultLauncher<Boolean> =
        activityResultRegistry.register(
            CopyModeKey,
            lifecycleOwner,
            PasswordGeneratorContract(),
            callback
        )

    fun start(isCopyMode: Boolean = false) = activityResultLauncher.launch(isCopyMode)

    companion object {
        const val CopyModeKey = "CopyModeKey"
        const val ResultKey = "ResultKey"
    }
}