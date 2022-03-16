package com.ladsers.passtable.android

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import licenseText

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
        aboutInit()
    }

    override fun onResume() {
        super.onResume()

        binding.lockFile.etLockSecs.setText(ParamStorage.getInt(this, Param.LOCK_SECS).toString())
        binding.lockFile.etLockSecs.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.lockFile.etLockSecs.windowToken, 0)
    }

    private fun componentValInit() {
        when (ParamStorage.getInt(this, Param.THEME)){
            0 -> binding.theme.rbThemeDefault.isChecked = true
            1 -> binding.theme.rbThemeLight.isChecked = true
            2 -> binding.theme.rbThemeDark.isChecked = true
        }

        val lockMode = ParamStorage.getInt(this, Param.LOCK_MODE)
        when (lockMode) {
            0 -> binding.lockFile.rbLockModeTimePeriod.isChecked = true
            1 -> binding.lockFile.rbLockModeAlways.isChecked = true
            2 -> binding.lockFile.rbLockModeNever.isChecked = true
        }

        if (lockMode == 2) binding.lockFile.swLockAllowWhenEditing.isEnabled = false
        binding.lockFile.swLockAllowWhenEditing.isChecked =
            ParamStorage.getBool(this, Param.LOCK_ALLOW_WHEN_EDITING)

        if (lockMode != 0) binding.lockFile.etLockSecs.isEnabled = false
        binding.lockFile.etLockSecs.setText(ParamStorage.getInt(this, Param.LOCK_SECS).toString())

        binding.security.swPreventScreenCapture.isChecked =
            ParamStorage.getBool(this, Param.PREVENT_SCREEN_CAPTURE)

        binding.recentFiles.swRememberRecentFiles.isChecked =
            ParamStorage.getBool(this, Param.REMEMBER_RECENT_FILES)
        updateComponentsForRecentFiles(binding.recentFiles.swRememberRecentFiles.isChecked)

        binding.biometricAuth.swCheckboxRememberPasswordByDefault.text = getString(
            R.string.ui_ct_checkboxRememberPasswordByDefault,
            getString(R.string.dlg_ct_fingerprintLogin)
        )
        binding.biometricAuth.swCheckboxRememberPasswordByDefault.isChecked =
            ParamStorage.getBool(this, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT)

        val biometricAuth = BiometricAuth(this, this, {}, {}, {})
        biometricAuthIsAvailable = biometricAuth.checkAvailability()
        binding.biometricAuth.clBiometricAuth.visibility =
            if (biometricAuthIsAvailable) View.VISIBLE else View.GONE
    }

    private fun componentActionInit() {
        binding.theme.rbThemeDefault.setOnClickListener {
            ParamStorage.set(this, Param.THEME, 0)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        binding.theme.rbThemeLight.setOnClickListener {
            ParamStorage.set(this, Param.THEME, 1)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        binding.theme.rbThemeDark.setOnClickListener {
            ParamStorage.set(this, Param.THEME, 2)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

       binding.lockFile.rbLockModeTimePeriod.setOnClickListener {
            if (ParamStorage.getInt(this, Param.LOCK_MODE) != 0) {
                ParamStorage.set(this, Param.LOCK_MODE, 0)
               binding.lockFile.swLockAllowWhenEditing.isEnabled = true
               binding.lockFile.etLockSecs.isEnabled = true
               binding.lockFile.etLockSecs.clearFocus()
            }
        }
       binding.lockFile.rbLockModeAlways.setOnClickListener {
            ParamStorage.set(this, Param.LOCK_MODE, 1)
           binding.lockFile.swLockAllowWhenEditing.isEnabled = true
           binding.lockFile.etLockSecs.isEnabled = false
           binding.lockFile.etLockSecs.setText(ParamStorage.getInt(this, Param.LOCK_SECS).toString())
        }
       binding.lockFile.rbLockModeNever.setOnClickListener {
            ParamStorage.set(this, Param.LOCK_MODE, 2)
           binding.lockFile.swLockAllowWhenEditing.isEnabled = false
           binding.lockFile.etLockSecs.isEnabled = false
           binding.lockFile.etLockSecs.setText(ParamStorage.getInt(this, Param.LOCK_SECS).toString())
        }
       binding.lockFile.swLockAllowWhenEditing.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(this, Param.LOCK_ALLOW_WHEN_EDITING, isChecked)
        }
        binding.lockFile.clLockSecs.setOnClickListener {
            binding.lockFile.etLockSecs.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.lockFile.etLockSecs, 0)
        }
        etLockSecsInit()

        binding.security.swPreventScreenCapture.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(this, Param.PREVENT_SCREEN_CAPTURE, isChecked)
        }

       binding.recentFiles.swRememberRecentFiles.setOnClickListener {
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
                ) {binding.recentFiles.swRememberRecentFiles.isChecked = true }
                msgDialog.addSkipAction {binding.recentFiles.swRememberRecentFiles.isChecked = true }
                msgDialog.show(it)
            } else changeRememberingRecentFiles(binding.recentFiles.swRememberRecentFiles.isChecked)
        }

       binding.recentFiles.btClearRecentFiles.setOnClickListener {
            val msg =
                if (biometricAuthIsAvailable) getString(R.string.dlg_msg_recentFilesAndPasswordsWillBeCleared)
                else getString(R.string.dlg_msg_recentFilesWillBeCleared)

            msgDialog.quickDialog(getString(R.string.dlg_title_areYouSure), msg, {
                RecentFiles.clear(this)
                Toast.makeText(
                    this, getString(R.string.ui_msg_done), Toast.LENGTH_SHORT
                ).show()
            }, it)
        }

        binding.biometricAuth.swCheckboxRememberPasswordByDefault.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(this, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT, isChecked)
        }


        binding.biometricAuth.btForgetPasswords.setOnClickListener {
            msgDialog.quickDialog(
                getString(R.string.dlg_title_areYouSure),
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
        binding.lockFile.etLockSecs.setOnKeyListener { v, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                v.clearFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                if (binding.lockFile.etLockSecs.text.toString().isNotEmpty()) ParamStorage.set(
                    this,
                    Param.LOCK_SECS,
                    binding.lockFile.etLockSecs.text.toString().toInt()
                )
                else binding.lockFile.etLockSecs.setText(
                    ParamStorage.getInt(this, Param.LOCK_SECS).toString()
                )

                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        binding.lockFile.etLockSecs.doAfterTextChanged { x ->
            if (x.toString().startsWith('0')) binding.lockFile.etLockSecs.setText("")
        }
    }

    private fun updateComponentsForRecentFiles(isEnabled: Boolean) {
        binding.biometricAuth.swCheckboxRememberPasswordByDefault.isEnabled = isEnabled
        binding.recentFiles.btClearRecentFiles.isEnabled = isEnabled
        binding.biometricAuth.btForgetPasswords.isEnabled = isEnabled
        binding.recentFiles.btClearRecentFiles.alpha = if (isEnabled) 1.0f else 0.5f
        binding.biometricAuth.btForgetPasswords.alpha = if (isEnabled) 1.0f else 0.5f
    }

    private fun changeRememberingRecentFiles(isEnabled: Boolean) {
        ParamStorage.set(this, Param.REMEMBER_RECENT_FILES, isEnabled)
        updateComponentsForRecentFiles(isEnabled)
        RecentFiles.clear(this)
    }

    private fun aboutInit() {
        binding.help.btCloudSync.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            intent.putExtra("title", getString(R.string.app_bt_cloudFileSync))
            intent.putExtra("info", getString(R.string.app_info_cloudFileSync))
            startActivity(intent)
        }
        binding.help.btShortcuts.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            intent.putExtra("title", getString(R.string.app_bt_keyboardShortcuts))
            intent.putExtra("info", getString(R.string.app_info_keyboardShortcuts))
            startActivity(intent)
        }

        binding.web.btWebPage.setOnClickListener {
            val webPage = Uri.parse(getString(R.string.app_info_webPage))
            startActivity(Intent(Intent.ACTION_VIEW, webPage))
        }
        binding.web.btRepo.setOnClickListener {
            val repo = Uri.parse(getString(R.string.app_info_repo))
            startActivity(Intent(Intent.ACTION_VIEW, repo))
        }

        binding.about.tvAuthor.text =
            getString(R.string.ui_ct_createdBy, getString(R.string.app_com_maxKorolev))
        binding.about.btLicense.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            intent.putExtra("title", getString(R.string.app_bt_license))
            intent.putExtra("info", licenseText)
            startActivity(intent)
        }
        binding.about.btThirdParty.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            intent.putExtra("title", getString(R.string.app_bt_thirdPartyResources))
            intent.putExtra("info", getString(R.string.app_info_thirdPartyResources))
            startActivity(intent)
        }
        binding.about.tvVersion.text = BuildConfig.VERSION_NAME
    }
}
