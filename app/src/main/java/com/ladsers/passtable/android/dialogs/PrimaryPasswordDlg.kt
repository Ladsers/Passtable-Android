package com.ladsers.passtable.android.dialogs

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.DocumentsContract
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.BiometricAuth
import com.ladsers.passtable.android.components.PasswordInput
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.databinding.DialogDataEntryBinding
import com.ladsers.passtable.lib.Verifier

class PrimaryPasswordDlg(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val activity: Activity,
    private val window: Window,
    private val biometricAuth: BiometricAuth,
    private val completeCreation: (password: String) -> Unit,
    private val completeOpening: (password: String) -> Unit,
    private val completeSavingAs: (newPath: Uri, newPass: String?) -> Unit
) {
    enum class Mode { OPEN, NEW, SAVE_AS }

    var isNeedRememberPassword = false
        private set

    private var rememberingAvailable = true
    private var checkboxInitState = false
    private var isNeedSkipTextChangedCheck = false

    private var passwordIsVisible = false
    private var confirmIsVisible = false

    fun show(
        mode: Mode,
        uri: Uri? = null,
        incorrectPassword: Boolean = false,
        canRememberPass: Boolean = true
    ) {
        val builder = AlertDialog.Builder(context)
        val binding = DialogDataEntryBinding.inflate(window.layoutInflater)
        builder.setView(binding.root)

        isNeedRememberPassword = false
        val biometricAuthAvailable = biometricAuth.checkAvailability()
        if (!canRememberPass) rememberingAvailable = false // disable remembering
        val biometricAuthActive = biometricAuthAvailable && rememberingAvailable
        var closedViaButton = false

        binding.tvTitle.text = getTitle(mode)
        binding.clPassword.visibility = View.VISIBLE
        binding.btShowPass.setOnClickListener {
            passwordIsVisible =
                PasswordInput.showHidePassword(
                    context,
                    binding.etPassword,
                    binding.btShowPass,
                    passwordIsVisible
                )
        }
        configureMainButtons(binding, mode)

        configureRememberingCheckbox(binding, mode, biometricAuthActive)
        configureConfirmWidget(binding, mode)
        configureNeutralBtn(binding, mode)

        handleIncorrectPassword(binding, incorrectPassword)

        builder.setOnDismissListener {
            if (!closedViaButton) {
                uri?.let {
                    DocumentsContract.deleteDocument(contentResolver, it)
                    Toast.makeText(
                        context, context.getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
                    ).show()
                }
                if (mode != Mode.SAVE_AS) activity.finish()
            }
        }

        builder.show().apply {
            this.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            this.setCanceledOnTouchOutside(false)
            this.window!!.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            )
            binding.etPassword.requestFocus()

            var errCode = -1

            binding.etPassword.doAfterTextChanged { x ->
                passwordIsVisible =
                    PasswordInput.widgetBehavior(
                        context,
                        x,
                        binding.etPassword,
                        binding.btShowPass,
                        passwordIsVisible
                    )
                binding.btNeutral.isEnabled =
                    x.toString().isEmpty() && binding.etConfirm.text.toString().isEmpty()
                binding.cbRememberPass.visibility =
                    if (!binding.btNeutral.isEnabled && biometricAuthActive) View.VISIBLE else View.GONE

                errCode = Verifier.verifyPrimary(x.toString())
                binding.btPositive.isEnabled = errCode == 0
                binding.clErr.visibility = if (errCode == 0) View.GONE else View.VISIBLE
                val errMsg = when (errCode) {
                    1 -> context.getString(R.string.dlg_ct_primaryEmpty)
                    2 -> context.getString(
                        R.string.dlg_ct_primaryInvalidChars,
                        Verifier.getPrimaryAllowedChars(context.getString(R.string.app_com_spaceChar))
                    )
                    3 -> context.getString(R.string.dlg_ct_primarySlashChar)
                    else -> ""
                }
                binding.tvErrMsg.text = errMsg

                if (mode != Mode.OPEN && errCode == 0) {
                    val confirmPass = binding.etConfirm.text.toString()
                    val matched = x.toString() == confirmPass
                    binding.btPositive.isEnabled = matched
                    binding.tvErrMsg.text = context.getString(R.string.dlg_ct_passwordsDoNotMatch)
                    binding.clErr.visibility =
                        if (matched || confirmPass.isEmpty()) View.GONE else View.VISIBLE
                }
            }

            binding.btPositive.setOnClickListener {
                if (biometricAuthAvailable && rememberingAvailable) isNeedRememberPassword =
                    binding.cbRememberPass.isChecked
                checkboxInitState = isNeedRememberPassword

                val pass = binding.etPassword.text.toString()
                when (mode) {
                    Mode.OPEN -> completeOpening(pass)
                    Mode.NEW -> completeCreation(pass)
                    Mode.SAVE_AS -> completeSavingAs(uri!!, pass)
                }
                closedViaButton = true
                this.dismiss()
            }

            if (mode == Mode.OPEN) {
                val alertDialog = this
                binding.etPassword.setOnKeyListener(object : View.OnKeyListener {
                    override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                            if (!binding.btPositive.isEnabled) return true

                            if (biometricAuthAvailable && rememberingAvailable) isNeedRememberPassword =
                                binding.cbRememberPass.isChecked
                            checkboxInitState = isNeedRememberPassword

                            val pass = binding.etPassword.text.toString()
                            completeOpening(pass)
                            closedViaButton = true
                            alertDialog.dismiss()
                            return true
                        }
                        return false
                    }
                })
            }

            if (mode == Mode.SAVE_AS) {
                binding.btNeutral.setOnClickListener {
                    completeSavingAs(uri!!, null)
                    closedViaButton = true
                    this.dismiss()
                }
            }

            binding.btNegative.setOnClickListener {
                this.dismiss()
            }


            if (mode == Mode.OPEN) return@apply

            val doNotMatchMsgWithDelay = Runnable { binding.clErr.visibility = View.VISIBLE }

            binding.etConfirm.doBeforeTextChanged { _, _, _, _ ->
                if (isNeedSkipTextChangedCheck) {
                    // fixes error message flickering when user click on Eye button
                    isNeedSkipTextChangedCheck = false
                    return@doBeforeTextChanged
                }
                binding.clErr.removeCallbacks(doNotMatchMsgWithDelay)
                if (errCode == 0) binding.clErr.visibility = View.GONE
            }

            binding.etConfirm.doAfterTextChanged { x ->
                confirmIsVisible =
                    PasswordInput.widgetBehavior(
                        context,
                        x,
                        binding.etConfirm,
                        binding.btShowConfirm,
                        confirmIsVisible
                    )
                binding.btNeutral.isEnabled =
                    x.toString().isEmpty() && binding.etPassword.text.toString().isEmpty()
                binding.cbRememberPass.visibility =
                    if (!binding.btNeutral.isEnabled && biometricAuthActive) View.VISIBLE else View.GONE

                if (errCode == 0 && x.toString().isNotEmpty()) {
                    val matched = x.toString() == binding.etPassword.text.toString()
                    binding.btPositive.isEnabled = matched
                    binding.tvErrMsg.text = context.getString(R.string.dlg_ct_passwordsDoNotMatch)
                    if (!matched) binding.clErr.postDelayed(
                        doNotMatchMsgWithDelay,
                        750
                    )
                }
            }
        }
    }

    private fun getTitle(mode: Mode): String {
        return context.getString(
            when (mode) {
                Mode.OPEN -> R.string.dlg_title_openFile
                Mode.NEW -> R.string.dlg_title_createNewFile
                Mode.SAVE_AS -> R.string.dlg_title_saveAs
            }
        )
    }

    private fun configureMainButtons(binding: DialogDataEntryBinding, mode: Mode) {
        binding.btPositive.text =
            context.getString(if (mode == Mode.OPEN) R.string.app_bt_enter else R.string.app_bt_save)
        binding.btPositive.icon = ContextCompat.getDrawable(
            context,
            if (mode == Mode.OPEN) R.drawable.ic_enter else R.drawable.ic_save
        )

        binding.btNegative.text = context.getString(R.string.app_bt_cancel)
        binding.btNegative.icon = ContextCompat.getDrawable(context, R.drawable.ic_close)
    }

    private fun configureRememberingCheckbox(
        binding: DialogDataEntryBinding,
        mode: Mode,
        biometricAuthActive: Boolean
    ) {
        binding.cbRememberPass.isChecked =
            ParamStorage.getBool(context, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT)
        binding.cbRememberPass.visibility =
            if (mode != Mode.SAVE_AS && biometricAuthActive) View.VISIBLE else View.GONE
    }

    private fun configureNeutralBtn(binding: DialogDataEntryBinding, mode: Mode) {
        if (mode != Mode.SAVE_AS) return // no configuration required
        binding.btNeutral.visibility = View.VISIBLE
        binding.btNeutral.text = context.getString(R.string.app_bt_saveWithCurrentPassword)
        binding.btNeutral.icon = ContextCompat.getDrawable(context, R.drawable.ic_save)
    }

    private fun configureConfirmWidget(binding: DialogDataEntryBinding, mode: Mode) {
        if (mode == Mode.OPEN) return // no configuration required
        binding.clConfirm.visibility = View.VISIBLE
        binding.btShowConfirm.setOnClickListener {
            isNeedSkipTextChangedCheck = true
            confirmIsVisible = PasswordInput.showHidePassword(
                context,
                binding.etConfirm,
                binding.btShowConfirm,
                confirmIsVisible
            )
        }
    }

    private fun handleIncorrectPassword(
        binding: DialogDataEntryBinding,
        incorrectPassword: Boolean
    ) {
        if (!incorrectPassword) return // don't do anything
        binding.cbRememberPass.isChecked = checkboxInitState
        binding.clErr.visibility = View.VISIBLE
        binding.tvErrMsg.text = context.getString(R.string.dlg_ct_incorrectPassword)
    }
}