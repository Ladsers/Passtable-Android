package com.ladsers.passtable.android.components.settingsModules

import android.app.Activity
import android.view.View
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import com.ladsers.passtable.android.dialogs.MessageDlg

interface ISettingsModule {

    val activity: Activity
    val binding: ActivitySettingsBinding
    val messageDlg: MessageDlg

    val rootView : View

    fun configure()

    fun attachActionsOnCreate()

    fun attachActionsOnResume()

    fun setVisible() {
        rootView.visibility = View.VISIBLE
    }
}