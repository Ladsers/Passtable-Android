package com.ladsers.passtable.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.ladsers.passtable.android.containers.Param
import com.ladsers.passtable.android.containers.ParamStorage

class EditTextLockSecs : androidx.appcompat.widget.AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setText(ParamStorage.getInt(context, Param.LOCK_SECS).toString())
            clearFocus()
            val imm = context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0) // fixes keyboard flickering when hiding
        }

        return super.onKeyPreIme(keyCode, event)
    }
}