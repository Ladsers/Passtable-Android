package com.ladsers.passtable.android.dialogs

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.databinding.DialogMessageBinding
import java.util.*

class MessageDlg(
    val context: Context,
    private val window: Window
) {
    private var title = ""
    private var message = ""
    private var isCreated = false

    private var textPositive = ""
    private var textNegative = ""
    private var textNeutral = ""

    private var iconPositive = 0
    private var iconNegative = 0
    private var iconNeutral = 0

    private var isAddedPositive = false
    private var isAddedNegative = false
    private var isAddedNeutral = false

    private var actionPositive: () -> Unit = {}
    private var actionNegative: () -> Unit = {}
    private var actionNeutral: () -> Unit = {}
    private var actionDismiss: () -> Unit = {}

    private var isCancelable = true

    fun create(title: String, message: String) {
        this.title = title
        this.message = message
        isCancelable = true
        isAddedPositive = false
        isAddedNegative = false
        isAddedNeutral = false
        actionDismiss = {}

        isCreated = true
    }

    fun addPositiveBtn(text: String, iconRes: Int, action: () -> Unit) {
        textPositive =
            text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        iconPositive = iconRes
        actionPositive = action
        isAddedPositive = true
    }

    fun addNegativeBtn(text: String, iconRes: Int, action: () -> Unit = {}) {
        textNegative =
            text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        iconNegative = iconRes
        actionNegative = action
        isAddedNegative = true
    }

    fun addNeutralBtn(text: String, iconRes: Int, action: () -> Unit) {
        textNeutral =
            text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        iconNeutral = iconRes
        actionNeutral = action
        isAddedNeutral = true
    }

    fun addSkipAction(action: () -> Unit) {
        actionDismiss = action
    }

    fun disableSkip() {
        isCancelable = false
    }

    fun show(btView: View? = null) {
        if (!isCreated || !isAddedPositive) return
        isCreated = false

        val binding: DialogMessageBinding = DialogMessageBinding.inflate(window.layoutInflater)
        val builder = AlertDialog.Builder(context)
        builder.setView(binding.root)
        builder.setCancelable(isCancelable)

        binding.tvTitle.text = title
        binding.tvMessage.movementMethod = ScrollingMovementMethod()
        binding.tvMessage.text = message

        configureBtn(binding.btPositive, true, textPositive, iconPositive)
        configureBtn(binding.btNegative, isAddedNegative, textNegative, iconNegative)
        configureBtn(binding.btNeutral, isAddedNeutral, textNeutral, iconNeutral)

        var doSkip = true
        builder.setOnDismissListener { if (doSkip) actionDismiss() }

        builder.show().apply {
            this.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            binding.btPositive.setOnClickListener {
                actionPositive()
                doSkip = false
                this.dismiss()
            }

            binding.btNegative.setOnClickListener {
                actionNegative()
                doSkip = false
                this.dismiss()
            }

            binding.btNeutral.setOnClickListener {
                actionNeutral()
                doSkip = false
                this.dismiss()
            }

            binding.tvMessage.post {
                val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                val dialogHeight = this.window!!.decorView.height
                val rect = Rect()
                window!!.decorView.getWindowVisibleDisplayFrame(rect)
                val statusBar = rect.top

                if (dialogHeight + statusBar >= screenHeight) binding.tvMessage.maxLines = 4
            }
        }

        // Protection against two copies of the same dialog.
        btView?.let { it ->
            it.isClickable = false
            it.postDelayed({ it.isClickable = true }, 200)
        }
    }

    private fun configureBtn(btn: MaterialButton, isEnabled: Boolean, text: String, icon: Int) {
        if (!isEnabled) {
            btn.visibility = View.GONE
            return
        }
        btn.visibility = View.VISIBLE
        btn.text = text
        btn.icon = ContextCompat.getDrawable(context, icon)
    }

    fun quickDialog(
        title: String,
        message: String,
        posAction: () -> Unit,
        btView: View? = null,
        posText: String = context.getString(R.string.app_bt_ok),
        negText: String = context.getString(R.string.app_bt_cancel),
        posIcon: Int = R.drawable.ic_accept,
        negIcon: Int = R.drawable.ic_close
    ) {
        create(title, message)
        addPositiveBtn(posText, posIcon, posAction)
        addNegativeBtn(negText, negIcon)
        show(btView)
    }
}