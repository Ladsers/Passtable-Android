package com.ladsers.passtable.android.components.tableActivity

import android.content.Context
import android.content.res.Configuration
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.SnackbarManager
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.databinding.ActivityTableBinding
import com.ladsers.passtable.lib.DataItem

object TableInitInfo {
    fun showItemMenu(context: Context, binding: ActivityTableBinding, dataList: List<DataItem>) {
        val param = Param.INITIAL_INFO_ITEM_MENU
        if (ParamStorage.getBool(context, param) && dataList.isNotEmpty()) SnackbarManager.showInitInfo(
            context,
            binding.root,
            param,
            context.getString(R.string.app_info_itemMenu)
        )
    }

    fun showKeyboardShortcuts(context: Context, binding: ActivityTableBinding) {
        val param = Param.INITIAL_INFO_KEYBOARD_SHORTCUTS
        if (ParamStorage.getBool(context, param) &&
            context.resources.configuration.keyboard == Configuration.KEYBOARD_QWERTY
        ) {
            val info = context.getString(R.string.app_info_keyboardShortcuts)
            SnackbarManager.showInitInfo(context, binding.root, param, info)
        }
    }
}