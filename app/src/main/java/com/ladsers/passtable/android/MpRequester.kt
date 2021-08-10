package com.ladsers.passtable.android

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.DocumentsContract
import android.text.Editable
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.ladsers.passtable.android.databinding.DialogAskpasswordBinding

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
    private val binding: DialogAskpasswordBinding =
        DialogAskpasswordBinding.inflate(window.layoutInflater)

    private var rememberMasterPass = false
    private var rememberingAvailable = true

    fun startRequest(
        uri: Uri,
        isFileCreation: Boolean,
        showErrInvalidPass: Boolean = false,
        canRememberPass: Boolean = true
    ) {
        val builder = AlertDialog.Builder(context)
        val binding = DialogAskpasswordBinding.inflate(window.layoutInflater)
        builder.setView(binding.root)
        binding.tvTitle.text = context.getString(R.string.dlg_title_enterMasterPassword)

        rememberMasterPass = false
        val biometricAuthAvailable = biometricAuth.checkAvailability()
        if (!canRememberPass) rememberingAvailable = false
        var closedViaButton = false

        binding.cbRememberPass.isChecked =
            ParamStorage.getBool(context, Param.CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT)
        binding.cbRememberPass.visibility =
            if (biometricAuthAvailable && rememberingAvailable) View.VISIBLE else View.GONE
        if (showErrInvalidPass) binding.tvInvalidPassword.visibility = View.VISIBLE

        val posBtnText = if (isFileCreation) context.getString(R.string.app_bt_save)
        else context.getString(R.string.app_bt_ok)
        builder.setPositiveButton(posBtnText) { _, _ ->
            if (biometricAuthAvailable && rememberingAvailable) rememberMasterPass =
                binding.cbRememberPass.isChecked

            val pass = binding.etPassword.text.toString()
            if (isFileCreation) completeCreation(pass) else completeOpening(pass)
            closedViaButton = true
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
            setupDialog(this)
            val button = this.getButton(AlertDialog.BUTTON_POSITIVE)
            button.isEnabled = false
            binding.etPassword.doAfterTextChanged { x -> reactionToChanges(x, button) }
        }
    }

    fun forNewFileRequest(pathNewFile: Uri) {
        val builder = AlertDialog.Builder(context)
        val binding = DialogAskpasswordBinding.inflate(window.layoutInflater)
        builder.setView(binding.root)
        binding.tvTitle.text = context.getString(R.string.dlg_title_enterNewMasterPassword)
        binding.cbRememberPass.visibility = View.GONE

        var closedViaButton = false

        builder.setPositiveButton(context.getString(R.string.app_bt_ok)) { _, _ ->
            val pass = binding.etPassword.text.toString()
            if (completeSavingWithNewPassword(pathNewFile.toString(), pass)) afterSaving(pathNewFile)
            closedViaButton = true
        }
        builder.setNeutralButton(context.getString(R.string.app_bt_doNotChangePassword)) { _, _ ->
            if (completeSaving(pathNewFile.toString())) afterSaving(pathNewFile)
            closedViaButton = true
        }
        builder.setOnDismissListener {
            if (!closedViaButton) {
                DocumentsContract.deleteDocument(contentResolver, pathNewFile)
                Toast.makeText(
                    context, context.getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
                ).show()
            }
        }

        builder.show().apply {
            setupDialog(this)
            val button = this.getButton(AlertDialog.BUTTON_POSITIVE)
            button.isEnabled = false
            binding.etPassword.doAfterTextChanged { x -> reactionToChanges(x, button) }
        }
    }

    fun isNeedToRemember() = rememberMasterPass

    private fun setupDialog(alertDialog: AlertDialog){
        alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.window!!.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )
        binding.etPassword.requestFocus()
    }

    private fun reactionToChanges(text: Editable?, btnOk: Button){
        btnOk.isEnabled = text.toString().isNotEmpty()
    }
}