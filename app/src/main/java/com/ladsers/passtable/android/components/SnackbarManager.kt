package com.ladsers.passtable.android.components

import android.content.Context
import android.view.View
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.containers.Param
import com.ladsers.passtable.android.containers.ParamStorage

object SnackbarManager {
    fun showInitInfo(
        context: Context,
        view: View,
        param: Param,
        infoText: String,
        duration: Int = 10000
    ) {
        ParamStorage.set(context, param, false)

        val snackbar = Snackbar.make(view, infoText, Snackbar.LENGTH_INDEFINITE)
        snackbar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
        snackbar.duration = duration
        snackbar.setAction(context.getText(R.string.app_bt_close)) {}
        snackbar.show()
    }
}