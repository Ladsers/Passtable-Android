package com.ladsers.passtable.android

import android.content.Context
import android.view.View
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

object SnackbarManager {
    fun showInitInfo(context: Context, view: View, param: Param, infoText: String) {
        ParamStorage.set(context, param, false)

        val snackbar = Snackbar.make(view, infoText, Snackbar.LENGTH_INDEFINITE)
        snackbar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
        snackbar.duration = 10000
        snackbar.setAction(context.getText(R.string.app_bt_close)) {}
        snackbar.show()
    }
}