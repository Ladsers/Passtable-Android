package com.ladsers.passtable.android

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.view.Window
import android.view.WindowManager
import androidx.core.widget.doAfterTextChanged
import com.ladsers.passtable.android.databinding.DialogAskfilenameBinding

class AskFileName(
    private val context: Context,
    private val window: Window,
    private val createFile: (Uri, String) -> Unit
) {

    fun ask(uri: Uri) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val binding: DialogAskfilenameBinding =
            DialogAskfilenameBinding.inflate(window.layoutInflater)
        builder.setView(binding.root)
        val title = context.getString(R.string.dlg_title_enterFileName)
        builder.setTitle(title)

        builder.setPositiveButton(context.getString(R.string.app_bt_ok)) { _, _ ->
            val fileName =
                binding.etFileName.text.toString() + context.getString(R.string.app_com_fileExtension)
            createFile(uri, fileName)
        }

        builder.show().apply {
            this.setCanceledOnTouchOutside(false)

            this.window!!.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            )
            binding.etFileName.requestFocus()

            val button = this.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            button.isEnabled = false
            binding.etFileName.doAfterTextChanged { x ->
                button.isEnabled = x.toString().isNotEmpty() &&
                        x.toString().matches("^[^.\\\\/:*?\"<>|]?[^\\\\/:*?\"<>|]*".toRegex())
            }
        }
    }
}