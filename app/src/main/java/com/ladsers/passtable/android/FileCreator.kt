package com.ladsers.passtable.android

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.view.Window
import android.view.WindowManager
import androidx.core.widget.doAfterTextChanged
import com.ladsers.passtable.android.databinding.DialogAskfilenameBinding

class FileCreator(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val window: Window,
    private val openFile: (Uri) -> Unit,
) {

    fun askName(tree: Uri, cancelable: Boolean = true) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val binding: DialogAskfilenameBinding =
            DialogAskfilenameBinding.inflate(window.layoutInflater)
        builder.setView(binding.root)
        val title = context.getString(R.string.dlg_title_enterFileName)
        builder.setTitle(title)
        builder.setCancelable(cancelable)

        builder.setPositiveButton(context.getString(R.string.app_bt_ok)) { _, _ ->
            val fileName =
                binding.etFileName.text.toString() + context.getString(R.string.app_com_fileExtension)
            createFile(tree, fileName)
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

    private fun createFile(tree: Uri, fileName: String){
        val docId = DocumentsContract.getTreeDocumentId(tree)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)

        val file = DocumentsContract.createDocument(
            contentResolver,
            docUri,
            "application/octet-stream",
            fileName
        )!!

        openFile(file)
    }
}