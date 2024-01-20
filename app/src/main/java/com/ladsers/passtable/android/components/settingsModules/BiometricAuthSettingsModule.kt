package com.ladsers.passtable.android.components.settingsModules

import android.app.Activity
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.BiometricAuth
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import com.ladsers.passtable.android.dialogs.MessageDlg

class BiometricAuthSettingsModule(
    override val activity: Activity,
    override val binding: ActivitySettingsBinding,
    override val messageDlg: MessageDlg
) : ISettingsModule {

    private lateinit var biometricAuth: BiometricAuth

    override val rootView: View
        get() = binding.biometricAuth.root


    override fun configure() {
        biometricAuth = BiometricAuth(activity, activity as FragmentActivity, {}, {}, {})

        binding.biometricAuth.swCheckboxRememberPasswordByDefault.isChecked =
            ParamStorage.getBool(activity, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT)

        binding.biometricAuth.clBiometricAuth.visibility =
            if (biometricAuth.checkAvailability()) View.VISIBLE else View.GONE
    }

    override fun attachActionsOnCreate() {
        binding.biometricAuth.swCheckboxRememberPasswordByDefault.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(activity, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT, isChecked)
        }

        binding.biometricAuth.btDisableBiometric.setOnClickListener {
            messageDlg.quickDialog(
                activity.getString(R.string.dlg_title_disable),
                activity.getString(R.string.dlg_msg_needToEnterPrimaryPasswordForAny), {
                    biometricAuth.resetAuth()
                    Toast.makeText(
                        activity, activity.getString(R.string.ui_msg_done), Toast.LENGTH_SHORT
                    ).show()
                }, it, activity.getString(R.string.app_bt_disable)
            )
        }
    }

    override fun attachActionsOnResume() {
        // not used
    }
}