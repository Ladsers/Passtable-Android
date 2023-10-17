package com.ladsers.passtable.android.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.PasswordGeneratorProcessor
import com.ladsers.passtable.android.containers.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.databinding.ActivityPasswordGeneratorBinding
import com.ladsers.passtable.android.databinding.PasswordGeneratorCollectionBinding
import com.ladsers.passtable.android.widgets.EditTextParam
import com.ladsers.passtable.lib.PasswordGenerator

class PasswordGeneratorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordGeneratorBinding
    private lateinit var generator: PasswordGenerator
    private lateinit var errorWindowAnim: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        errorWindowAnim = AnimationUtils.loadAnimation(this, R.anim.error_window_anim)

        binding.toolbar.root.title = getString(R.string.ui_ct_passwordGenerator)
        binding.toolbar.root.navigationIcon =
            ContextCompat.getDrawable(this, R.drawable.ic_back_arrow)
        setSupportActionBar(binding.toolbar.root)
        binding.toolbar.root.setNavigationOnClickListener { finish() }

        binding.svLayout.setOnScrollChangeListener { _, _, y, _, oldY ->
            if (y > oldY || y < oldY) binding.toolbar.root.elevation = 7f
            if (y == 0) binding.toolbar.root.elevation = 0f
        }
        binding.svLayout.postDelayed({ binding.svLayout.fullScroll(ScrollView.FOCUS_DOWN) }, 500)

        generator = PasswordGenerator()

        configureEditParamInt(
            binding.passwordLength.editText,
            binding.passwordLength.textView,
            Param.GENERATOR_PASSWORD_LENGTH
        )
        // special behavior
        binding.passwordLength.editText.doAfterTextChanged { x ->
            if (x.toString().startsWith('0')) binding.passwordLength.editText.setText("")
        }
        configureEditParamExclude()

        configureCollection(
            binding.lowercaseLetters,
            R.string.ui_ct_lowercaseLetters,
            Param.GENERATOR_LOWERCASE_LETTERS_ALLOW,
            Param.GENERATOR_LOWERCASE_LETTERS_MINIMUM
        )
        configureCollection(
            binding.capitalLetters,
            R.string.ui_ct_capitalLetters,
            Param.GENERATOR_CAPITAL_LETTERS_ALLOW,
            Param.GENERATOR_CAPITAL_LETTERS_MINIMUM
        )
        configureCollection(
            binding.numbers,
            R.string.ui_ct_numbers,
            Param.GENERATOR_NUMBERS_ALLOW,
            Param.GENERATOR_NUMBERS_MINIMUM
        )
        configureCollection(
            binding.symbols,
            R.string.ui_ct_symbols,
            Param.GENERATOR_SYMBOLS_ALLOW,
            Param.GENERATOR_SYMBOLS_MINIMUM
        )

        val isCopyMode = intent.getBooleanExtra(PasswordGeneratorProcessor.CopyModeKey, false)
        if (isCopyMode) {
            (binding.result.btOk as MaterialButton).icon =
                ContextCompat.getDrawable(this, R.drawable.ic_copy)
        }

        binding.result.btRefresh.setOnClickListener {
            generate()
        }

        binding.result.btOk.setOnClickListener {
            val intent = Intent().putExtra(
                PasswordGeneratorProcessor.ResultKey,
                binding.result.etPassword.text.toString()
            )
            setResult(RESULT_OK, intent)
            finish()
        }

        generate() // auto generation at startup
    }

    override fun onResume() {
        super.onResume()

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        fun resetEditText(editText: EditText) {
            editText.clearFocus()
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
        }

        resetEditText(binding.passwordLength.editText)
        resetEditText(binding.exclude.editText)

        resetEditText(binding.lowercaseLetters.etMinimumNumber)
        resetEditText(binding.capitalLetters.etMinimumNumber)
        resetEditText(binding.numbers.etMinimumNumber)
        resetEditText(binding.symbols.etMinimumNumber)
    }

    private fun configureCollection(
        bin: PasswordGeneratorCollectionBinding,
        collectionNameResId: Int,
        paramAllow: Param,
        paramMinimum: Param
    ) {
        bin.swCollectionAllow.text = getString(collectionNameResId)
        bin.swCollectionAllow.isChecked = ParamStorage.getBool(this, paramAllow)

        bin.tvMinimumNumber.isClickable = bin.swCollectionAllow.isChecked
        bin.etMinimumNumber.isEnabled = bin.swCollectionAllow.isChecked

        bin.swCollectionAllow.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(this, paramAllow, isChecked)
            bin.tvMinimumNumber.isClickable = isChecked
            bin.etMinimumNumber.isEnabled = isChecked

            if (paramAllow == Param.GENERATOR_SYMBOLS_ALLOW) {
                bin.rbBasicSet.isEnabled = isChecked
                bin.rbFullSet.isEnabled = isChecked
            }

            generate()
        }

        configureEditParamInt(bin.etMinimumNumber, bin.tvMinimumNumber, paramMinimum)

        if (paramAllow != Param.GENERATOR_SYMBOLS_ALLOW) return

        bin.rbSymbolSet.visibility = View.VISIBLE
        bin.rbBasicSet.text = getString(
            R.string.ui_ct_basicSet,
            generator.easySymbolChars.joinToString(separator = " ")
        )
        bin.rbBasicSet.isEnabled = bin.swCollectionAllow.isChecked
        bin.rbFullSet.isEnabled = bin.swCollectionAllow.isChecked
        when (ParamStorage.getInt(this, Param.GENERATOR_SYMBOLS_SET)) {
            0 -> bin.rbBasicSet.isChecked = true
            1 -> bin.rbFullSet.isChecked = true
        }

        bin.rbBasicSet.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) return@setOnCheckedChangeListener
            ParamStorage.set(this, Param.GENERATOR_SYMBOLS_SET, 0)
            generate()
        }
        bin.rbFullSet.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) return@setOnCheckedChangeListener
            ParamStorage.set(this, Param.GENERATOR_SYMBOLS_SET, 1)
            generate()
        }
    }

    private fun configureEditParamInt(editText: EditTextParam, labelView: TextView, param: Param) {
        editText.setText(ParamStorage.getInt(this, param).toString())

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        labelView.setOnClickListener {
            editText.requestFocus()
            imm.showSoftInput(editText, 0)
        }

        editText.setOnKeyListener { v, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                if (editText.text.toString().isNotEmpty()) {
                    ParamStorage.set(this, param, editText.text.toString().toInt())
                    generate()
                }
                v.clearFocus()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        editText.doAfterTextChanged { x ->
            if (x!!.length > 1 && x.toString().startsWith('0')) {
                editText.setText(x.toString().drop(1))
                editText.setSelection(1)
            }
        }
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) return@setOnFocusChangeListener
            editText.setText(ParamStorage.getInt(this, param).toString())
        }
    }

    private fun configureEditParamExclude() {
        val bin = binding.exclude
        bin.editText.setText(ParamStorage.getStr(this, Param.GENERATOR_EXCLUDE))
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        bin.textView.setOnClickListener {
            bin.editText.requestFocus()
            imm.showSoftInput(bin.editText, 0)
        }

        bin.editText.setOnKeyListener { v, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                ParamStorage.set(this, Param.GENERATOR_EXCLUDE, bin.editText.text.toString())
                generate()
                v.clearFocus()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        bin.editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) Toast.makeText(
                this,
                R.string.ui_msg_enterCharsInRowOrSpace,
                Toast.LENGTH_LONG
            ).show()
            else bin.editText.setText(ParamStorage.getStr(this, Param.GENERATOR_EXCLUDE))
        }
    }

    private fun generate() {
        generator.isLowercaseLettersAllowed = binding.lowercaseLetters.swCollectionAllow.isChecked
        generator.isCapitalLettersAllowed = binding.capitalLetters.swCollectionAllow.isChecked
        generator.isNumbersAllowed = binding.numbers.swCollectionAllow.isChecked
        generator.isSymbolsAllowed = binding.symbols.swCollectionAllow.isChecked
        generator.isEasySymbolsMode = binding.symbols.rbBasicSet.isChecked
        generator.blockChars(binding.exclude.editText.text.toString())

        val length = binding.passwordLength.editText.text.toString().toInt()
        val minLowercaseLetters =
            if (generator.isLowercaseLettersAllowed) binding.lowercaseLetters.etMinimumNumber.text.toString()
                .toInt() else 0
        val minSymbolsChars =
            if (generator.isSymbolsAllowed) binding.symbols.etMinimumNumber.text.toString()
                .toInt() else 0
        val minCapitalLetters =
            if (generator.isCapitalLettersAllowed) binding.capitalLetters.etMinimumNumber.text.toString()
                .toInt() else 0
        val minNumbers =
            if (generator.isNumbersAllowed) binding.numbers.etMinimumNumber.text.toString()
                .toInt() else 0

        if (!generator.isLowercaseLettersAllowed && !generator.isCapitalLettersAllowed
            && !generator.isNumbersAllowed && !generator.isSymbolsAllowed
        ) {
            badResult(getString(R.string.ui_ct_noCharSetsGeneration))
            return
        }

        if (!generator.checkGenParams(
                length,
                minLowercaseLetters,
                minSymbolsChars,
                minCapitalLetters,
                minNumbers
            )
        ) {
            badResult(getString(R.string.ui_ct_sumMinNumberExceedsPassLength))
            return
        }

        try {
            binding.result.etPassword.text = generator.generate(
                length,
                minLowercaseLetters,
                minSymbolsChars,
                minCapitalLetters,
                minNumbers
            )
            goodResult()
        } catch (e: IllegalStateException) {
            badResult(getString(R.string.ui_ct_tooManyCharsExcluded))
        }
    }

    private fun goodResult() {
        binding.result.btOk.isEnabled = true
        binding.result.btRefresh.isEnabled = true
        binding.result.clErr.clearAnimation()
        binding.result.clErr.visibility = View.GONE
    }

    private fun badResult(errMsg: String) {
        binding.result.btOk.isEnabled = false
        binding.result.btRefresh.isEnabled = false
        binding.result.etPassword.text = ""
        binding.result.clErr.visibility = View.VISIBLE
        binding.result.clErr.clearAnimation()
        binding.result.clErr.startAnimation(errorWindowAnim)
        binding.result.tvErrMsg.text = errMsg
        binding.svLayout.post { binding.svLayout.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}