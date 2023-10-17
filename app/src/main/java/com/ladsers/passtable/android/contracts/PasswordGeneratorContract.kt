package com.ladsers.passtable.android.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.ladsers.passtable.android.activities.PasswordGeneratorActivity
import com.ladsers.passtable.android.components.PasswordGeneratorProcessor

class PasswordGeneratorContract : ActivityResultContract<Boolean, String?>() {
    override fun createIntent(context: Context, input: Boolean): Intent {
        return Intent(context, PasswordGeneratorActivity::class.java).putExtra(
            PasswordGeneratorProcessor.CopyModeKey,
            input
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null
        else intent.getStringExtra(PasswordGeneratorProcessor.ResultKey)
    }
}