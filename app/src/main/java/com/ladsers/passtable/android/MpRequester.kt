package com.ladsers.passtable.android

import Verifier
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.DocumentsContract
import android.text.Editable
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import com.google.android.material.button.MaterialButton
import com.ladsers.passtable.android.databinding.DialogEnterdataBinding
import java.util.*


class MpRequester(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val activity: Activity,
    private val window: Window,
    private val biometricAuth: BiometricAuth,
    private val completeCreation: (password: String) -> Unit,
    private val completeOpening: (password: String) -> Unit,
    private val completeSavingAs: (newPath: Uri, newPass: String?) -> Unit
) {
    enum class Mode { OPEN, NEW, SAVEAS }

    private var rememberMasterPass = false
    private var rememberingAvailable = true
    private var rememberingChecked = false

    private var passwordIsVisible = false
    private var confirmIsVisible = false

    private var btConfirmClicked = false

    fun start(
        mode: Mode,
        uri: Uri? = null,
        incorrectPassword: Boolean = false,
        canRememberPass: Boolean = true
    ) {
        val builder = AlertDialog.Builder(context)
        val binding = DialogEnterdataBinding.inflate(window.layoutInflater)
        builder.setView(binding.root)

        rememberMasterPass = false
        val biometricAuthAvailable = biometricAuth.checkAvailability()
        if (!canRememberPass) rememberingAvailable = false
        var closedViaButton = false

        binding.tvTitle.text = context.getString(
            when (mode) {
                Mode.OPEN -> R.string.dlg_title_openTheFile
                Mode.NEW -> R.string.dlg_title_createNewFile
                Mode.SAVEAS -> R.string.dlg_title_saveAs
            }
        )
        binding.clPassword.visibility = View.VISIBLE

        binding.cbRememberPass.isChecked =
            ParamStorage.getBool(context, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT)
        val biometricAuthActive = biometricAuthAvailable && rememberingAvailable
        binding.cbRememberPass.visibility =
            if (mode != Mode.SAVEAS && biometricAuthActive) View.VISIBLE else View.GONE

        binding.btPositive.text =
            context.getString(if (mode == Mode.OPEN) R.string.app_bt_enter else R.string.app_bt_save)
        binding.btPositive.icon = ContextCompat.getDrawable(
            context,
            if (mode == Mode.OPEN) R.drawable.ic_enter else R.drawable.ic_save
        )

        if (mode == Mode.SAVEAS) {
            binding.btNeutral.visibility = View.VISIBLE
            binding.btNeutral.text = context.getString(R.string.app_bt_saveWithCurrent)
            binding.btNeutral.icon = ContextCompat.getDrawable(context, R.drawable.ic_save)
        }

        binding.btNegative.text = context.getString(R.string.app_bt_cancel)
        binding.btNegative.icon = ContextCompat.getDrawable(context, R.drawable.ic_close)

        binding.btShowPass.setOnClickListener {
            passwordIsVisible =
                showHidePassword(context, binding.etPassword, binding.btShowPass, passwordIsVisible)
        }

        if (mode != Mode.OPEN) {
            binding.clConfirm.visibility = View.VISIBLE
            binding.btShowConfirm.setOnClickListener {
                btConfirmClicked = true
                confirmIsVisible =
                    showHidePassword(context, binding.etConfirm, binding.btShowConfirm, confirmIsVisible)
            }
        }

        if (incorrectPassword) {
            binding.cbRememberPass.isChecked = rememberingChecked
            binding.clErr.visibility = View.VISIBLE
            binding.tvErrMsg.text = context.getString(R.string.dlg_ct_incorrectPassword)
        }

        builder.setOnDismissListener {
            if (!closedViaButton) {
                uri?.let {
                    DocumentsContract.deleteDocument(contentResolver, it)
                    Toast.makeText(
                        context, context.getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
                    ).show()
                }
                if (mode != Mode.SAVEAS) activity.finish()
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
                    widgetBehavior(context, x, binding.etPassword, binding.btShowPass, passwordIsVisible)
                binding.btNeutral.isEnabled =
                    x.toString().isEmpty() && binding.etConfirm.text.toString().isEmpty()
                binding.cbRememberPass.visibility =
                    if (!binding.btNeutral.isEnabled && biometricAuthActive) View.VISIBLE else View.GONE

                errCode = Verifier.verifyMp(x.toString())
                binding.btPositive.isEnabled = errCode == 0
                binding.clErr.visibility = if (errCode == 0) View.GONE else View.VISIBLE
                val errMsg = when (errCode) {
                    1 -> context.getString(R.string.dlg_ct_primaryEmpty)
                    2 -> context.getString(
                        R.string.dlg_ct_primaryInvalidChars,
                        Verifier.getMpAllowedChars(context.getString(R.string.app_com_spaceChar))
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
                if (biometricAuthAvailable && rememberingAvailable) rememberMasterPass =
                    binding.cbRememberPass.isChecked
                rememberingChecked = rememberMasterPass

                val pass = binding.etPassword.text.toString()
                when (mode) {
                    Mode.OPEN -> completeOpening(pass)
                    Mode.NEW -> completeCreation(pass)
                    Mode.SAVEAS -> completeSavingAs(uri!!, pass)
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

                            if (biometricAuthAvailable && rememberingAvailable) rememberMasterPass =
                                binding.cbRememberPass.isChecked
                            rememberingChecked = rememberMasterPass

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

            if (mode == Mode.SAVEAS) {
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
                if (btConfirmClicked) {
                    btConfirmClicked = false
                    return@doBeforeTextChanged
                }
                binding.clErr.removeCallbacks(doNotMatchMsgWithDelay)
                if (errCode == 0) binding.clErr.visibility = View.GONE
            }

            binding.etConfirm.doAfterTextChanged { x ->
                confirmIsVisible =
                    widgetBehavior(context, x, binding.etConfirm, binding.btShowConfirm, confirmIsVisible)
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

    fun isNeedToRemember() = rememberMasterPass

    companion object {
        fun widgetBehavior(
            context: Context,
            x: Editable?,
            etPassword: EditText,
            btShow: MaterialButton,
            isVisible: Boolean
        ): Boolean {
            var current = isVisible

            val isNotEmpty = x.toString().isNotEmpty()
            if (!isNotEmpty) current = showHidePassword(context, etPassword, btShow, true)
            etPassword.typeface = ResourcesCompat.getFont(
                context,
                if (isNotEmpty) if (current) R.font.overpassmono_semibold else R.font.passmono_asterisk
                else R.font.manrope
            )
            btShow.visibility = if (isNotEmpty) View.VISIBLE else View.INVISIBLE

            return current
        }

        fun showHidePassword(
            context: Context,
            etPassword: EditText,
            btShow: MaterialButton,
            isVisible: Boolean
        ): Boolean {
            val current = !isVisible

            etPassword.transformationMethod =
                if (current) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text.length)

            etPassword.typeface = ResourcesCompat.getFont(
                context,
                if (current) R.font.overpassmono_semibold else R.font.passmono_asterisk
            )

            btShow.icon = ContextCompat.getDrawable(
                context,
                if (current) R.drawable.ic_lock else R.drawable.ic_password_show
            )

            return current
        }
    }
}