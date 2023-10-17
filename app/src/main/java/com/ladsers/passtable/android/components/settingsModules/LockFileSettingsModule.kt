package com.ladsers.passtable.android.components.settingsModules

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.ladsers.passtable.android.containers.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import com.ladsers.passtable.android.dialogs.MessageDlg

class LockFileSettingsModule(
    override val activity: Activity,
    override val binding: ActivitySettingsBinding,
    override val messageDlg: MessageDlg
) : ISettingsModule {

    override val rootView: View
        get() = binding.lockFile.root


    override fun configure() {
        val lockMode = ParamStorage.getInt(activity, Param.LOCK_MODE)
        when (lockMode) {
            0 -> binding.lockFile.rbLockModeTimePeriod.isChecked = true
            1 -> binding.lockFile.rbLockModeAlways.isChecked = true
            2 -> binding.lockFile.rbLockModeNever.isChecked = true
        }

        if (lockMode == 2) binding.lockFile.swLockAllowWhenEditing.isEnabled = false
        binding.lockFile.swLockAllowWhenEditing.isChecked =
            ParamStorage.getBool(activity, Param.LOCK_ALLOW_WHEN_EDITING)

        if (lockMode != 0) binding.lockFile.etLockSecs.isEnabled = false
        binding.lockFile.etLockSecs.setText(
            ParamStorage.getInt(activity, Param.LOCK_SECS).toString()
        )
    }

    override fun attachActionsOnCreate() {
        binding.lockFile.rbLockModeTimePeriod.setOnClickListener {
            if (ParamStorage.getInt(activity, Param.LOCK_MODE) != 0) {
                ParamStorage.set(activity, Param.LOCK_MODE, 0)
                binding.lockFile.swLockAllowWhenEditing.isEnabled = true
                binding.lockFile.etLockSecs.isEnabled = true
                binding.lockFile.etLockSecs.clearFocus()
            }
        }
        binding.lockFile.rbLockModeAlways.setOnClickListener {
            ParamStorage.set(activity, Param.LOCK_MODE, 1)
            binding.lockFile.swLockAllowWhenEditing.isEnabled = true
            binding.lockFile.etLockSecs.isEnabled = false
        }
        binding.lockFile.rbLockModeNever.setOnClickListener {
            ParamStorage.set(activity, Param.LOCK_MODE, 2)
            binding.lockFile.swLockAllowWhenEditing.isEnabled = false
            binding.lockFile.etLockSecs.isEnabled = false
        }
        binding.lockFile.swLockAllowWhenEditing.setOnCheckedChangeListener { _, isChecked ->
            ParamStorage.set(activity, Param.LOCK_ALLOW_WHEN_EDITING, isChecked)
        }
        binding.lockFile.clLockSecs.setOnClickListener {
            binding.lockFile.etLockSecs.requestFocus()
            val imm =
                activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.lockFile.etLockSecs, 0)
        }

        binding.lockFile.etLockSecs.setOnKeyListener { v, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                val imm =
                    activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                if (binding.lockFile.etLockSecs.text.toString().isNotEmpty()) ParamStorage.set(
                    activity,
                    Param.LOCK_SECS,
                    binding.lockFile.etLockSecs.text.toString().toInt()
                )
                v.clearFocus()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        binding.lockFile.etLockSecs.doAfterTextChanged { x ->
            if (x.toString().startsWith('0')) binding.lockFile.etLockSecs.setText("")
        }
        binding.lockFile.etLockSecs.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) return@setOnFocusChangeListener
            binding.lockFile.etLockSecs.setText(
                ParamStorage.getInt(activity, Param.LOCK_SECS).toString()
            )
        }
    }

    override fun attachActionsOnResume() {
        binding.lockFile.etLockSecs.clearFocus()
        val imm =
            activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.lockFile.etLockSecs.windowToken, 0)
    }
}