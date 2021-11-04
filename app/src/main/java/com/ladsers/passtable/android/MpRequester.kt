package com.ladsers.passtable.android

import Verifier
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.DocumentsContract
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
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
    private val completeSaving: (newPath: String) -> Boolean,
    private val completeSavingWithNewPassword: (newPath: String, newPass: String) -> Boolean,
    private val afterSaving: (newPath: Uri) -> Unit
) {
    private var rememberMasterPass = false
    private var rememberingAvailable = true

    private var passwordIsVisible = false

    fun startRequest(
        uri: Uri,
        isFileCreation: Boolean,
        showErrInvalidPass: Boolean = false,
        canRememberPass: Boolean = true
    ) {
        val builder = AlertDialog.Builder(context)
        val binding = DialogEnterdataBinding.inflate(window.layoutInflater)
        builder.setView(binding.root)

        rememberMasterPass = false
        val biometricAuthAvailable = biometricAuth.checkAvailability()
        if (!canRememberPass) rememberingAvailable = false
        var closedViaButton = false

        binding.tvTitle.text = if (isFileCreation) context.getString(R.string.dlg_ct_createNewFile)
        else context.getString(R.string.dlg_ct_openTheFile)
        binding.clPassword.visibility = View.VISIBLE

        binding.cbRememberPass.isChecked =
            ParamStorage.getBool(context, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT)
        binding.cbRememberPass.visibility =
            if (biometricAuthAvailable && rememberingAvailable) View.VISIBLE else View.GONE

        binding.btPositive.text = if (isFileCreation) context.getString(R.string.app_bt_save)
        else context.getString(R.string.app_bt_enter)
        binding.btPositive.icon =
            if (isFileCreation) ContextCompat.getDrawable(context, R.drawable.ic_save)
            else ContextCompat.getDrawable(context, R.drawable.ic_enter)

        binding.btNegative.text = context.getString(R.string.app_bt_cancel).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }
        binding.btNegative.icon = ContextCompat.getDrawable(context, R.drawable.ic_close)

        binding.btShowPass.setOnClickListener { showHidePassword(binding) }

        if (showErrInvalidPass) {
            binding.clErr.visibility = View.VISIBLE
            binding.tvErrMsg.text = context.getString(R.string.dlg_ct_incorrectPassword)
        }

        builder.setOnDismissListener {
            if (!closedViaButton) {
                if (isFileCreation) {
                    DocumentsContract.deleteDocument(contentResolver, uri)
                    Toast.makeText(
                        context, context.getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
                    ).show()
                }
                activity.finish()
            }
        }

        builder.show().apply {
            setupDialog(this, binding)

            binding.btPositive.setOnClickListener {
                if (biometricAuthAvailable && rememberingAvailable) rememberMasterPass =
                    binding.cbRememberPass.isChecked

                val pass = binding.etPassword.text.toString()
                if (isFileCreation) completeCreation(pass) else completeOpening(pass)
                closedViaButton = true
                this.dismiss()
            }

            binding.btNegative.setOnClickListener {
                this.dismiss()
            }
        }
    }

    fun forNewFileRequest(pathNewFile: Uri) {
        val builder = AlertDialog.Builder(context)
        val binding = DialogEnterdataBinding.inflate(window.layoutInflater)
        builder.setView(binding.root)

        var closedViaButton = false

        binding.tvTitle.text = context.getString(R.string.dlg_ct_saveAs)
        binding.clPassword.visibility = View.VISIBLE
        binding.cbRememberPass.visibility = View.GONE

        binding.btPositive.text = context.getString(R.string.app_bt_save)
        binding.btPositive.icon = ContextCompat.getDrawable(context, R.drawable.ic_save)

        binding.btNeutral.visibility = View.VISIBLE
        binding.btNeutral.text = context.getString(R.string.app_bt_saveWithCurrent)
        binding.btNeutral.icon = ContextCompat.getDrawable(context, R.drawable.ic_save)

        binding.btNegative.text = context.getString(R.string.app_bt_cancel).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }
        binding.btNegative.icon = ContextCompat.getDrawable(context, R.drawable.ic_close)

        binding.btShowPass.setOnClickListener { showHidePassword(binding) }

        builder.setOnDismissListener {
            if (!closedViaButton) {
                DocumentsContract.deleteDocument(contentResolver, pathNewFile)
                Toast.makeText(
                    context, context.getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
                ).show()
            }
        }

        builder.show().apply {
            setupDialog(this, binding)

            binding.btPositive.setOnClickListener {
                val pass = binding.etPassword.text.toString()
                if (completeSavingWithNewPassword(
                        pathNewFile.toString(),
                        pass
                    )
                ) afterSaving(pathNewFile)
                closedViaButton = true
                this.dismiss()
            }

            binding.btNeutral.setOnClickListener {
                if (completeSaving(pathNewFile.toString())) afterSaving(pathNewFile)
                closedViaButton = true
                this.dismiss()
            }

            binding.btNegative.setOnClickListener {
                this.dismiss()
            }
        }
    }

    fun isNeedToRemember() = rememberMasterPass

    private fun setupDialog(
        alertDialog: AlertDialog,
        binding: DialogEnterdataBinding
    ) {
        alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.window!!.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )
        binding.etPassword.requestFocus()
        binding.etPassword.doAfterTextChanged { x ->
            val isNotEmpty = x.toString().isNotEmpty()
            if (!isNotEmpty) showHidePassword(binding, true) //hide password
            binding.etPassword.typeface = ResourcesCompat.getFont(
                context,
                if (isNotEmpty) if (passwordIsVisible) R.font.overpassmono_semibold else R.font.passmono_asterisk
                else R.font.manrope
            )
            binding.btShowPass.visibility = if (isNotEmpty) View.VISIBLE else View.INVISIBLE

            val res = Verifier.verifyMp(x.toString())
            binding.btPositive.isEnabled = res == 0
            binding.clErr.visibility = if (res == 0) View.GONE else View.VISIBLE
            val errMsg = when (res) {
                1 -> context.getString(R.string.dlg_err_mpEmpty)
                2 -> context.getString(R.string.dlg_err_mpInvalidChars) + ' ' + Verifier.getMpAllowedChars(
                    context.getString(R.string.app_com_spaceChar)
                )
                3 -> context.getString(R.string.dlg_err_mpSlashChar)
                else -> ""
            }
            binding.tvErrMsg.text = errMsg
        }
    }

    private fun showHidePassword(
        binding: DialogEnterdataBinding,
        isVisible: Boolean = passwordIsVisible
    ) {
        passwordIsVisible = !isVisible

        binding.etPassword.transformationMethod =
            if (passwordIsVisible) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
        binding.etPassword.setSelection(binding.etPassword.text.length)

        binding.etPassword.typeface = ResourcesCompat.getFont(
            context,
            if (passwordIsVisible) R.font.overpassmono_semibold else R.font.passmono_asterisk
        )

        binding.btShowPass.icon = ContextCompat.getDrawable(
            context,
            if (passwordIsVisible) R.drawable.ic_password_hide else R.drawable.ic_password_show
        )
    }
}