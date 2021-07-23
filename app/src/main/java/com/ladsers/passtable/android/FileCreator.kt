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
    private val selectTree: () -> Unit,
) {
    private var fileName = ""

    fun askName(oldName: String? = null, cancelable: Boolean = true) {
        fileName = ""

        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val binding: DialogAskfilenameBinding =
            DialogAskfilenameBinding.inflate(window.layoutInflater)
        builder.setView(binding.root)
        val title = context.getString(R.string.dlg_title_enterFileName)
        builder.setTitle(title)
        builder.setCancelable(cancelable)

        builder.setPositiveButton(context.getString(R.string.app_bt_next)) { _, _ ->
            fileName =
                binding.etFileName.text.toString() + context.getString(R.string.app_com_fileExtension)
            selectTree()
        }

        oldName?.let { it ->
            binding.etFileName.setText(it)
            binding.etFileName.selectAll()
        }

        builder.show().apply {
            this.setCanceledOnTouchOutside(false)

            this.window!!.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            )
            binding.etFileName.requestFocus()

            val button = this.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            button.isEnabled = binding.etFileName.text.isNotBlank()
            binding.etFileName.doAfterTextChanged { x ->
                button.isEnabled = x.toString().isNotBlank() &&
                        x.toString().matches("^[^.\\\\/:*?\"<>|]?[^\\\\/:*?\"<>|]*".toRegex())
            }
        }
    }

    fun createFile(tree: Uri): Uri{
        val docId = DocumentsContract.getTreeDocumentId(tree)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)

        return DocumentsContract.createDocument(
            contentResolver,
            docUri,
            "application/octet-stream",
            fileName
        )!!
    }
}