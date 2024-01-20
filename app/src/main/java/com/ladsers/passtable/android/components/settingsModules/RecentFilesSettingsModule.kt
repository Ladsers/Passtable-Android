package com.ladsers.passtable.android.components.settingsModules

import android.app.Activity
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.BiometricAuth
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.containers.RecentFiles
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import com.ladsers.passtable.android.dialogs.MessageDlg

class RecentFilesSettingsModule(
    override val activity: Activity,
    override val binding: ActivitySettingsBinding,
    override val messageDlg: MessageDlg
) : ISettingsModule {

    private lateinit var biometricAuth: BiometricAuth

    override val rootView: View
        get() = binding.recentFiles.root


    override fun configure() {
        biometricAuth = BiometricAuth(activity, activity as FragmentActivity, {}, {}, {})

        binding.recentFiles.swRememberRecentFiles.isChecked =
            ParamStorage.getBool(activity, Param.REMEMBER_RECENT_FILES)
        updateComponentsForRecentFiles(binding.recentFiles.swRememberRecentFiles.isChecked)
    }

    override fun attachActionsOnCreate() {
        binding.recentFiles.swRememberRecentFiles.setOnClickListener {
            if (RecentFiles.isNotEmpty(activity)) {
                val msg =
                    if (biometricAuth.checkAvailability()) activity.getString(R.string.dlg_msg_actionWillClearRecentFilesWithBiometric)
                    else activity.getString(R.string.dlg_msg_actionWillClearRecentFiles)

                messageDlg.create(activity.getString(R.string.dlg_title_disable), msg)
                messageDlg.addPositiveBtn(
                    activity.getString(R.string.app_bt_disable),
                    R.drawable.ic_accept
                ) { changeRememberingRecentFiles(false) }
                messageDlg.addNegativeBtn(
                    activity.getString(R.string.app_bt_cancel),
                    R.drawable.ic_close
                ) { binding.recentFiles.swRememberRecentFiles.isChecked = true }
                messageDlg.addSkipAction {
                    binding.recentFiles.swRememberRecentFiles.isChecked = true
                }
                messageDlg.show(it)
            } else changeRememberingRecentFiles(binding.recentFiles.swRememberRecentFiles.isChecked)
        }

        binding.recentFiles.btClearRecentFiles.setOnClickListener {
            val msg =
                if (biometricAuth.checkAvailability()) activity.getString(R.string.dlg_msg_clearRecentFilesWithBiometric)
                else activity.getString(R.string.dlg_msg_permanentAction)

            messageDlg.quickDialog(activity.getString(R.string.dlg_title_clearList), msg, {
                RecentFiles.clear(activity)
                Toast.makeText(
                    activity, activity.getString(R.string.ui_msg_done), Toast.LENGTH_SHORT
                ).show()
            }, it, activity.getString(R.string.app_bt_clear), posIcon = R.drawable.ic_delete)
        }
    }

    override fun attachActionsOnResume() {
        // not used
    }

    private fun updateComponentsForRecentFiles(isEnabled: Boolean) {
        binding.biometricAuth.swCheckboxRememberPasswordByDefault.isEnabled = isEnabled
        binding.recentFiles.btClearRecentFiles.isEnabled = isEnabled
        binding.biometricAuth.btDisableBiometric.isEnabled = isEnabled
        binding.recentFiles.btClearRecentFiles.alpha = if (isEnabled) 1.0f else 0.5f
        binding.biometricAuth.btDisableBiometric.alpha = if (isEnabled) 1.0f else 0.5f
    }

    private fun changeRememberingRecentFiles(isEnabled: Boolean) {
        ParamStorage.set(activity, Param.REMEMBER_RECENT_FILES, isEnabled)
        updateComponentsForRecentFiles(isEnabled)
        RecentFiles.clear(activity)
    }
}