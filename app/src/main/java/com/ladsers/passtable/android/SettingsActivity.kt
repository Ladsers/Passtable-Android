package com.ladsers.passtable.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private var biometricAuthIsAvailable = false
    private lateinit var msgDialog: MsgDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        msgDialog = MsgDialog(this, window)

        binding.toolbar.root.title = getString(R.string.ui_ct_settings)
        binding.toolbar.root.navigationIcon =
            ContextCompat.getDrawable(this, R.drawable.ic_back_arrow)
        setSupportActionBar(binding.toolbar.root)
        binding.toolbar.root.setNavigationOnClickListener { finish() }

        binding.svLayout.setOnScrollChangeListener { _, _, y, _, oldY ->
            if (y > oldY || y < oldY) binding.toolbar.root.elevation = 7f
            if (y == 0) binding.toolbar.root.elevation = 0f
        }

        componentValInit()
        componentActionInit()
    }

    override fun onResume() {
        super.onResume()

        binding.etLockSecs.setText(ParamStorage.getInt(this, Param.LOCK_SECS).toString())
        binding.etLockSecs.clearFocus()
    }

    private fun componentValInit() {
        binding.swShowPasswordInCard.isChecked =
            ParamStorage.getBool(this, Param.SHOW_PASSWORD_IN_CARD)

        binding.swCheckboxRememberPasswordByDefault.isChecked =
            ParamStorage.getBool(this, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT)

        val lockMode = ParamStorage.getInt(this, Param.LOCK_MODE)
        when (lockMode) {
            0 -> binding.rbLockModeTimePeriod.isChecked = true
            1 -> binding.rbLockModeAlways.isChecked = true
            2 -> binding.rbLockModeNever.isChecked = true
        }

        if (lockMode == 2) binding.cbLockAllowWhenEditing.isEnabled = false
        binding.cbLockAllowWhenEditing.isChecked =
            ParamStorage.getBool(this, Param.LOCK_ALLOW_WHEN_EDITING)

        if (lockMode != 0) binding.etLockSecs.isEnabled = false
        binding.etLockSecs.setText(ParamStorage.getInt(this, Param.LOCK_SECS).toString())

        binding.swRememberRecentFiles.isChecked =
            ParamStorage.getBool(this, Param.REMEMBER_RECENT_FILES)
        updateComponentsForRecentFiles(binding.swRememberRecentFiles.isChecked)

        val biometricAuth = BiometricAuth(this, this, {}, {}, {})
        biometricAuthIsAvailable = biometricAuth.checkAvailability()
        if (biometricAuthIsAvailable) {
            binding.swCheckboxRememberPasswordByDefault.visibility = View.VISIBLE
            binding.tvForgetPasswords.visibility = View.VISIBLE
            binding.btForgetPasswords.visibility = View.VISIBLE
            binding.tvClearRecentFiles.text =
                getString(R.string.ui_ct_clearRecentFilesAndForgetPasswords)
        }
    }

    private fun componentActionInit() {
        binding.swShowPasswordInCard.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(this, Param.SHOW_PASSWORD_IN_CARD, isChecked)
        }

        binding.swCheckboxRememberPasswordByDefault.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(this, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT, isChecked)
        }

        binding.rbLockModeTimePeriod.setOnClickListener {
            if (ParamStorage.getInt(this, Param.LOCK_MODE) != 0) {
                ParamStorage.set(this, Param.LOCK_MODE, 0)
                binding.cbLockAllowWhenEditing.isEnabled = true
                binding.etLockSecs.isEnabled = true
                binding.etLockSecs.clearFocus()
            }
        }

        binding.rbLockModeAlways.setOnClickListener {
            ParamStorage.set(this, Param.LOCK_MODE, 1)
            binding.cbLockAllowWhenEditing.isEnabled = true
            binding.etLockSecs.isEnabled = false
            binding.etLockSecs.setText(ParamStorage.getInt(this, Param.LOCK_SECS).toString())
        }

        binding.rbLockModeNever.setOnClickListener {
            ParamStorage.set(this, Param.LOCK_MODE, 2)
            binding.cbLockAllowWhenEditing.isEnabled = false
            binding.etLockSecs.isEnabled = false
            binding.etLockSecs.setText(ParamStorage.getInt(this, Param.LOCK_SECS).toString())
        }

        binding.cbLockAllowWhenEditing.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(this, Param.LOCK_ALLOW_WHEN_EDITING, isChecked)
        }

        etLockSecsInit()

        binding.swRememberRecentFiles.setOnClickListener {
            if (RecentFiles.isNotEmpty(this)) {
                val msg =
                    if (biometricAuthIsAvailable) getString(R.string.dlg_msg_disableRememberingRecentFilesAndPasswords)
                    else getString(R.string.dlg_msg_disableRememberingRecentFiles)

                msgDialog.create(getString(R.string.dlg_title_warning), msg)
                msgDialog.addPositiveBtn(
                    getString(R.string.app_bt_ok),
                    R.drawable.ic_accept
                ) { changeRememberingRecentFiles(false) }
                msgDialog.addNegativeBtn(
                    getString(R.string.app_bt_cancel),
                    R.drawable.ic_close
                ) { binding.swRememberRecentFiles.isChecked = true }
                msgDialog.addSkipAction { binding.swRememberRecentFiles.isChecked = true }
                msgDialog.show(it)
            } else changeRememberingRecentFiles(binding.swRememberRecentFiles.isChecked)
        }

        binding.btClearRecentFiles.setOnClickListener {
            val msg =
                if (biometricAuthIsAvailable) getString(R.string.dlg_msg_recentFilesAndPasswordsWillBeCleared)
                else getString(R.string.dlg_msg_recentFilesWillBeCleared)

            msgDialog.quickDialog(getString(R.string.dlg_title_warning), msg, {
                RecentFiles.clear(this)
                Toast.makeText(
                    this, getString(R.string.ui_msg_done), Toast.LENGTH_SHORT
                ).show()
            }, it)
        }

        binding.btForgetPasswords.setOnClickListener {
            msgDialog.quickDialog(
                getString(R.string.dlg_title_warning),
                getString(R.string.dlg_msg_passwordsWillBeForgotten), {
                    RecentFiles.forgetMpsEncrypted(this)
                    Toast.makeText(
                        this, getString(R.string.ui_msg_done), Toast.LENGTH_SHORT
                    ).show()
                }, it
            )
        }
    }

    private fun etLockSecsInit() {
        binding.etLockSecs.setOnKeyListener { v, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                v.clearFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                if (binding.etLockSecs.text.toString().isNotEmpty()) ParamStorage.set(
                    this,
                    Param.LOCK_SECS,
                    binding.etLockSecs.text.toString().toInt()
                )
                else binding.etLockSecs.setText(
                    ParamStorage.getInt(this, Param.LOCK_SECS).toString()
                )

                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        binding.etLockSecs.doAfterTextChanged { x ->
            if (x.toString().startsWith('0')) binding.etLockSecs.setText("")
        }
    }

    private fun updateComponentsForRecentFiles(isEnabled: Boolean) {
        binding.swCheckboxRememberPasswordByDefault.isEnabled = isEnabled
        binding.btClearRecentFiles.isEnabled = isEnabled
        binding.btForgetPasswords.isEnabled = isEnabled
    }

    private fun changeRememberingRecentFiles(isEnabled: Boolean) {
        ParamStorage.set(this, Param.REMEMBER_RECENT_FILES, isEnabled)
        updateComponentsForRecentFiles(isEnabled)
        RecentFiles.clear(this)
    }
}
