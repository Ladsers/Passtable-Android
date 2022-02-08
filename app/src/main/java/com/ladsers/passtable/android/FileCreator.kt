package com.ladsers.passtable.android

import Verifier
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.DocumentsContract
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.ladsers.passtable.android.databinding.DialogEnterdataBinding
import java.util.*


class FileCreator(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val window: Window,
    private val selectTree: () -> Unit,
) {
    private var fileName = ""

    fun askName(oldName: String? = null, isCancelable: Boolean = true, btView: View? = null) {
        fileName = ""

        val binding: DialogEnterdataBinding = DialogEnterdataBinding.inflate(window.layoutInflater)
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setView(binding.root)
        builder.setCancelable(isCancelable)

        binding.tvTitle.text =
            context.getString(if (oldName == null) R.string.dlg_title_createNewFile else R.string.dlg_title_saveAs)
        binding.clFileName.visibility = View.VISIBLE

        binding.btPositive.text = context.getString(R.string.app_bt_selectFolder)
        binding.btPositive.icon = ContextCompat.getDrawable(context, R.drawable.ic_next_arrow)

        binding.btNegative.text = context.getString(R.string.app_bt_cancel)
        binding.btNegative.icon = ContextCompat.getDrawable(context, R.drawable.ic_close)
        binding.btNegative.visibility = if (isCancelable) View.VISIBLE else View.GONE

        builder.show().apply {
            var maxWidthIsSet = false

            this.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            this.setCanceledOnTouchOutside(false)

            binding.btPositive.isEnabled = binding.etFileName.text.isNotBlank()

            this.window!!.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            )
            binding.etFileName.requestFocus()

            binding.etFileName.doAfterTextChanged { x ->
                binding.tvExtension.visibility =
                    if (x.toString().isNotEmpty()) View.VISIBLE else View.INVISIBLE
                binding.etFileName.hint =
                    if (x.toString().isEmpty()) context.getString(R.string.dlg_ct_fileName) else ""

                if (!maxWidthIsSet) {
                    binding.etFileName.maxWidth = binding.etFileName.width
                    binding.etFileName.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    binding.etFileName.requestLayout()
                    maxWidthIsSet = true
                }

                val res = Verifier.verifyFileName(x.toString())
                binding.btPositive.isEnabled = res == 0
                binding.clErr.visibility = if (res == 0) View.GONE else View.VISIBLE
                val errMsg = when (res) {
                    1 -> context.getString(R.string.dlg_ct_fileNameBlank)
                    2 -> context.getString(R.string.dlg_ct_fileNameInvalidChars, Verifier.fileNameInvalidChars)
                    3 -> context.getString(R.string.dlg_ct_fileNameWhitespaceChar)
                    4 -> context.getString(R.string.dlg_ct_fileNameInvalidForWindows, Verifier.fileNameInvalidWinWords)
                    else -> ""
                }
                binding.tvErrMsg.text = errMsg
            }

            oldName?.let { it ->
                with(binding.etFileName) {
                    post {
                        setText(it)
                        selectAll()
                    }
                }
            }

            binding.btPositive.setOnClickListener {
                fileName =
                    binding.etFileName.text.toString() + context.getString(R.string.app_com_fileExtension)
                selectTree()
                this.dismiss()
            }

            val alertDialog = this
            binding.etFileName.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        if (!binding.btPositive.isEnabled) return true
                        fileName =
                            binding.etFileName.text.toString() + context.getString(R.string.app_com_fileExtension)
                        selectTree()
                        alertDialog.dismiss()
                        return true
                    }
                    return false
                }
            })

            binding.btNegative.setOnClickListener {
                this.dismiss()
            }
        }

        // Protection against two copies of the same dialog.
        btView?.let { it ->
            it.isClickable = false
            it.postDelayed({ it.isClickable = true }, 200)
        }
    }

    fun createFile(tree: Uri): Uri {
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