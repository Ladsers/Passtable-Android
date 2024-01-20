package com.ladsers.passtable.android.components.settingsModules

import android.app.Activity
import android.view.View
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import com.ladsers.passtable.android.dialogs.MessageDlg

class SecuritySettingsModule(
    override val activity: Activity,
    override val binding: ActivitySettingsBinding,
    override val messageDlg: MessageDlg
) : ISettingsModule {

    override val rootView: View
        get() = binding.security.root


    override fun configure() {
        binding.security.swPreventScreenCapture.isChecked =
            ParamStorage.getBool(activity, Param.PREVENT_SCREEN_CAPTURE)
    }

    override fun attachActionsOnCreate() {
        binding.security.swPreventScreenCapture.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(activity, Param.PREVENT_SCREEN_CAPTURE, isChecked)
        }
    }

    override fun attachActionsOnResume() {
        // not used
    }
}