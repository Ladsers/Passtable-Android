package com.ladsers.passtable.android

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.ladsers.passtable.android.databinding.ActivityEditBinding
import java.util.*


class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private var editMode = false
    private lateinit var selectedTag: String
    private var blockClosing = false

    private lateinit var originalTag: String
    private lateinit var originalNote: String
    private lateinit var originalLogin: String
    private lateinit var originalPassword: String

    private var isBackgrounded = false
    private var backgroundSecs = 0L

    private var passwordIsVisible = false
    private var confirmIsVisible = false

    private var btConfirmClicked = false
    private val doNotMatchMsgWithDelay = Runnable { showError(2) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.svLayout.setOnScrollChangeListener { _, _, y, _, oldY ->
            if (y > oldY || y < oldY) binding.toolbar.root.elevation = 7f
            if (y == 0) binding.toolbar.root.elevation = 0f
        }

        originalTag = intent.getStringExtra("dataTag") ?: "0"
        originalNote = intent.getStringExtra("dataNote") ?: ""
        originalLogin = intent.getStringExtra("dataLogin") ?: ""
        originalPassword = intent.getStringExtra("dataPassword") ?: ""

        editMode = intent.getBooleanExtra("modeEdit", false)
        blockClosing = intent.getBooleanExtra("blockClosing", false)

        if (!editMode) {
            binding.toolbar.root.title = getString(R.string.ui_ct_addItem)
        } else {
            binding.toolbar.root.title = getString(R.string.ui_ct_editItem)
            binding.btUndoNote.visibility = View.VISIBLE
            binding.btUndoLogin.visibility = View.VISIBLE
            binding.btUndoPassword.visibility = View.VISIBLE
        }

        if (!blockClosing) {
            binding.toolbar.root.navigationIcon =
                ContextCompat.getDrawable(this, R.drawable.ic_back_arrow)
            setSupportActionBar(binding.toolbar.root)
            binding.toolbar.root.setNavigationOnClickListener { unsavedChangesCheck() }
        }

        binding.etNote.inputType =
            EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.etNote.setHorizontallyScrolling(false)
        binding.etNote.maxLines = Integer.MAX_VALUE

        selectedTag = originalTag
        preselectTag()
        binding.btTagNone.setOnClickListener { v -> selectTag(v) }
        binding.btTagRed.setOnClickListener { v -> selectTag(v) }
        binding.btTagGreen.setOnClickListener { v -> selectTag(v) }
        binding.btTagBlue.setOnClickListener { v -> selectTag(v) }
        binding.btTagYellow.setOnClickListener { v -> selectTag(v) }
        binding.btTagPurple.setOnClickListener { v -> selectTag(v) }

        binding.clTagButtons.post {
            val maxWidth = (500 * resources.displayMetrics.density).toInt()
            if (binding.clTagButtons.width > maxWidth) {
                binding.clTagButtons.layoutParams.width = maxWidth
                binding.clTagButtons.requestLayout()
            }
        }

        editTextBehavior(binding.etNote, binding.btUndoNote, originalNote)
        editTextBehavior(binding.etLogin, binding.btUndoLogin, originalLogin)
        editTextBehavior(binding.etPassword, binding.btUndoPassword, originalPassword)

        if (resources.configuration.keyboard == Configuration.KEYBOARD_QWERTY) binding.etNote.requestFocus()

        binding.btShowPass.setOnClickListener {
            passwordIsVisible =
                MpRequester.showHidePassword(this, binding.etPassword, binding.btShowPass, passwordIsVisible)
        }

        binding.btShowConfirm.setOnClickListener {
            btConfirmClicked = true
            confirmIsVisible =
                MpRequester.showHidePassword(this, binding.etConfirm, binding.btShowConfirm, confirmIsVisible)
        }

        passwordsMatchCheck()

        binding.btSave.setOnClickListener { returnNewData() }

        canBeSavedCheck()
    }

    private fun editTextBehavior(editText: EditText, button: Button, originalVal: String) {
        editText.post { editText.setText(originalVal) }

        editText.doAfterTextChanged { x ->
            binding.clErr.removeCallbacks(doNotMatchMsgWithDelay)
            button.isEnabled = editMode && x.toString() != originalVal
            canBeSavedCheck()
        }

        button.setOnClickListener {
            it.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )

            editText.setText(originalVal)
            editText.setSelection(editText.text.length)
            button.isEnabled = false
        }
    }

    private fun passwordsMatchCheck(){
        binding.etPassword.doAfterTextChanged { x ->
            passwordIsVisible =
                MpRequester.widgetBehavior(this, x, binding.etPassword, binding.btShowPass, passwordIsVisible)
            if ((editMode && x.toString() != originalPassword) ||
                (!editMode && x.toString().isNotEmpty())
            ) {
                binding.etConfirm.isEnabled = true
                binding.clConfirm.visibility = View.VISIBLE
                binding.tvConfirmMsg.visibility = View.VISIBLE
            } else {
                binding.etConfirm.setText("")
                binding.etConfirm.isEnabled = false
                binding.clConfirm.visibility = View.GONE
                binding.tvConfirmMsg.visibility = View.GONE
            }
        }

        binding.etConfirm.doBeforeTextChanged { _, _, _, _ ->
            if (btConfirmClicked) {
                btConfirmClicked = false
                return@doBeforeTextChanged
            }
            binding.clErr.removeCallbacks(doNotMatchMsgWithDelay)
            if (canBeSavedCheck(false) != 1) showError(0)
        }

        binding.etConfirm.doAfterTextChanged { x ->
            confirmIsVisible =
                MpRequester.widgetBehavior(this, x, binding.etConfirm, binding.btShowConfirm, confirmIsVisible)

            if (canBeSavedCheck(false) != 1 &&
                x.toString().isNotEmpty() &&
                binding.etPassword.text.toString() != x.toString()
            ) {
                binding.clErr.postDelayed(
                    doNotMatchMsgWithDelay,
                    750
                )
            }
        }
    }

    private fun canBeSavedCheck(show: Boolean = true): Int {
        return if (binding.etNote.text.isBlank() &&
            (binding.etLogin.text.isBlank() || binding.etPassword.text.isEmpty())
        ) {
            disableBtSave()
            if (show) showError(1)
            1
        } else {
            if (binding.etConfirm.text.isNotEmpty() &&
                binding.etPassword.text.toString() != binding.etConfirm.text.toString()
            ) {
                disableBtSave()
                if (show) showError(2)
                2
            } else {
                enableBtSave()
                if (show) showError(0)
                0
            }
        }
    }

    private fun showError(code: Int) {
        when (code) {
            0 -> binding.clErr.visibility = View.GONE
            1 -> {
                binding.tvErrMsg.text = getString(R.string.ui_ct_editItemErr)
                binding.clErr.visibility = View.VISIBLE
            }
            2 -> {
                binding.tvErrMsg.text = getString(R.string.dlg_ct_passwordsDoNotMatch)
                binding.clErr.visibility = View.VISIBLE
            }
        }
    }

    private fun enableBtSave() {
        binding.btSave.isEnabled = true
        binding.btSave.elevation = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2.0F,
            resources.displayMetrics
        )
    }

    private fun disableBtSave() {
        binding.btSave.isEnabled = false
        binding.btSave.elevation = 0.0F
    }

    private fun returnNewData() {
        val intent = Intent()
        intent.putExtra("newDataTag", selectedTag)
        intent.putExtra("newDataNote", binding.etNote.text.toString())
        intent.putExtra("newDataLogin", binding.etLogin.text.toString())
        intent.putExtra("newDataPassword", binding.etPassword.text.toString())

        setResult(RESULT_OK, intent)
        finish()
    }

    private fun selectTag(clickedTag: View) {
        resetAllTags()
        updateTag(clickedTag as MaterialButton, true)
        selectedTag = when (clickedTag) {
            binding.btTagRed -> "1"
            binding.btTagGreen -> "2"
            binding.btTagBlue -> "3"
            binding.btTagYellow -> "4"
            binding.btTagPurple -> "5"
            else -> "0"
        }
    }

    private fun preselectTag(){
        resetAllTags()
        updateTag(
            when (selectedTag) {
                "1" -> binding.btTagRed
                "2" -> binding.btTagGreen
                "3" -> binding.btTagBlue
                "4" -> binding.btTagYellow
                "5" -> binding.btTagPurple
                else -> binding.btTagNone
            }, true
        )
    }

    private fun resetAllTags(){
        updateTag(binding.btTagNone, false)
        updateTag(binding.btTagRed, false)
        updateTag(binding.btTagGreen, false)
        updateTag(binding.btTagBlue, false)
        updateTag(binding.btTagYellow, false)
        updateTag(binding.btTagPurple, false)
    }

    private fun updateTag(button: MaterialButton, selected: Boolean) {
        button.setBackgroundColor(
            MaterialColors.getColor(
                button,
                if (selected) R.attr.editBackground else R.attr.whiteOrBlack
            )
        )
    }

    private fun unsavedChangesCheck(){
        if (selectedTag != originalTag ||
            binding.etNote.text.toString() != originalNote ||
            binding.etLogin.text.toString() != originalLogin ||
            binding.etPassword.text.toString() != originalPassword
        ) {
            MsgDialog(this, window).quickDialog(
                getString(R.string.dlg_title_closeWithoutSaving),
                getString(R.string.dlg_msg_unsavedDataWillBeLost),
                { finish() },
                posIcon = R.drawable.ic_exit,
                posText = getString(R.string.app_bt_close)
            )
        }
        else finish()
    }

    override fun onBackPressed() {
        if (!blockClosing) unsavedChangesCheck()
    }

    override fun onPause() {
        super.onPause()

        if (!blockClosing && ParamStorage.getBool(this, Param.LOCK_ALLOW_WHEN_EDITING)) {
            isBackgrounded = true
            backgroundSecs = Date().time / 1000
        }
    }

    override fun onResume() {
        super.onResume()

        if (isBackgrounded) {
            isBackgrounded = false

            when (ParamStorage.getInt(this, Param.LOCK_MODE)) {
                0 -> {
                    val secs = Date().time / 1000
                    val allowedTime = ParamStorage.getInt(this, Param.LOCK_SECS)
                    if (secs - backgroundSecs >= allowedTime) lockFile()
                }
                1 -> lockFile()
            }
        }
    }

    private fun lockFile() {
        val intent = Intent()
        intent.putExtra("needToLock", true)
        setResult(RESULT_OK, intent)
        finish()
    }
}