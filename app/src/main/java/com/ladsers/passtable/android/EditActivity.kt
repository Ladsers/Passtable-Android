package com.ladsers.passtable.android

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.ladsers.passtable.android.databinding.ActivityEditBinding
import java.util.*


class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private var editMode = false
    private lateinit var selectedTag: String
    private var blockClosing = false

    private var isBackgrounded = false
    private var backgroundSecs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val originalTag = intent.getStringExtra("dataTag") ?: "0"
        val originalNote = intent.getStringExtra("dataNote") ?: ""
        val originalLogin = intent.getStringExtra("dataLogin") ?: ""
        val originalPassword = intent.getStringExtra("dataPassword") ?: ""

        editMode = intent.getBooleanExtra("modeEdit", false)
        blockClosing = intent.getBooleanExtra("blockClosing", false)

        if (!editMode) {
            binding.toolbar.root.title = getString(R.string.ui_ct_addItem)
            binding.btUndoNote.visibility = View.GONE
            binding.btUndoLogin.visibility = View.GONE
            binding.btUndoPassword.visibility = View.GONE
            binding.btSave.text = getString(R.string.app_bt_addSave)
        } else {
            binding.toolbar.root.title = getString(R.string.ui_ct_editItem)
        }

        if (!blockClosing) {
            binding.toolbar.root.navigationIcon =
                ContextCompat.getDrawable(this, R.drawable.ic_back_arrow)
            setSupportActionBar(binding.toolbar.root)
            binding.toolbar.root.setNavigationOnClickListener { finish() }
        }

        binding.etNote.inputType = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
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

        editTextBehavior(binding.etNote, binding.btUndoNote, originalNote)
        editTextBehavior(binding.etLogin, binding.btUndoLogin, originalLogin)
        editTextBehavior(binding.etPassword, binding.btUndoPassword, originalPassword)

        binding.cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            binding.etPassword.transformationMethod = if (isChecked)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        binding.btSave.setOnClickListener { returnNewData() }

        canBeSavedCheck()
    }

    private fun editTextBehavior(editText: EditText, button: Button, originalVal: String) {
        editText.setText(originalVal)

        editText.doAfterTextChanged { x ->
            button.visibility =
                when {
                    editMode && x.toString() != originalVal -> View.VISIBLE
                    editMode -> View.INVISIBLE
                    else -> View.GONE
                }
            canBeSavedCheck()
        }

        button.setOnClickListener {
            editText.setText(originalVal)
            editText.setSelection(editText.text.length)
            button.visibility = View.INVISIBLE
        }
    }

    private fun canBeSavedCheck() {
        if (binding.etNote.text.isEmpty() &&
            (binding.etLogin.text.isEmpty() || binding.etPassword.text.isEmpty())
        ) {
            binding.btSave.isEnabled = false
            binding.tvErrMsg.visibility = View.VISIBLE
        } else {
            binding.btSave.isEnabled = true
            binding.tvErrMsg.visibility = View.INVISIBLE
        }
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
        setTagScale(clickedTag, 1.3F)
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
        setTagScale(when (selectedTag){
            "1" -> binding.btTagRed
            "2" -> binding.btTagGreen
            "3" -> binding.btTagBlue
            "4" -> binding.btTagYellow
            "5" -> binding.btTagPurple
             else -> binding.btTagNone
        }, 1.3F)
    }

    private fun resetAllTags(){
        val initScale = 0.75F

        setTagScale(binding.btTagNone, initScale)
        setTagScale(binding.btTagRed, initScale)
        setTagScale(binding.btTagGreen, initScale)
        setTagScale(binding.btTagBlue, initScale)
        setTagScale(binding.btTagYellow, initScale)
        setTagScale(binding.btTagPurple, initScale)
    }

    private fun setTagScale(imageButton: View, scale: Float) {
        with(imageButton) {
            scaleX = scale
            scaleY = scale
        }
    }

    override fun onBackPressed() {
        if (!blockClosing) super.onBackPressed()
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