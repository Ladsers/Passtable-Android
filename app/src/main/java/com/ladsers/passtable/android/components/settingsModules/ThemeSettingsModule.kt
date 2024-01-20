package com.ladsers.passtable.android.components.settingsModules

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import com.ladsers.passtable.android.dialogs.MessageDlg

class ThemeSettingsModule(
    override val activity: Activity,
    override val binding: ActivitySettingsBinding,
    override val messageDlg: MessageDlg
) : ISettingsModule {

    override val rootView: View
        get() = binding.theme.root


    override fun configure() {
        when (ParamStorage.getInt(activity, Param.THEME)) {
            0 -> binding.theme.rbThemeDefault.isChecked = true
            1 -> binding.theme.rbThemeLight.isChecked = true
            2 -> binding.theme.rbThemeDark.isChecked = true
        }
    }

    override fun attachActionsOnCreate() {
        binding.theme.rbThemeDefault.setOnClickListener {
            ParamStorage.set(activity, Param.THEME, 0)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        binding.theme.rbThemeLight.setOnClickListener {
            ParamStorage.set(activity, Param.THEME, 1)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        binding.theme.rbThemeDark.setOnClickListener {
            ParamStorage.set(activity, Param.THEME, 2)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    override fun attachActionsOnResume() {
        // not used
    }
}