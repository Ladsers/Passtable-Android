package com.ladsers.passtable.android.components

import android.content.Context
import android.text.Editable
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import com.ladsers.passtable.android.R

object PasswordInput {
    fun widgetBehavior(
        context: Context,
        x: Editable?,
        etPassword: EditText,
        btShow: MaterialButton,
        isVisible: Boolean
    ): Boolean {
        var current = isVisible

        val isNotEmpty = x.toString().isNotEmpty()
        if (!isNotEmpty) current = showHidePassword(context, etPassword, btShow, true)
        etPassword.typeface = ResourcesCompat.getFont(
            context,
            if (isNotEmpty) if (current) R.font.overpassmono_semibold else R.font.passmono_asterisk
            else R.font.manrope
        )
        btShow.visibility = if (isNotEmpty) View.VISIBLE else View.INVISIBLE

        return current
    }

    fun showHidePassword(
        context: Context,
        etPassword: EditText,
        btShow: MaterialButton,
        isVisible: Boolean
    ): Boolean {
        val current = !isVisible

        etPassword.transformationMethod =
            if (current) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
        etPassword.setSelection(etPassword.text.length)

        etPassword.typeface = ResourcesCompat.getFont(
            context,
            if (current) R.font.overpassmono_semibold else R.font.passmono_asterisk
        )

        btShow.icon = ContextCompat.getDrawable(
            context,
            if (current) R.drawable.ic_lock else R.drawable.ic_password_show
        )

        return current
    }
}