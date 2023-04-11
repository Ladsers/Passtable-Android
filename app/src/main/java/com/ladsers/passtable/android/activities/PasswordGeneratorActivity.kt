package com.ladsers.passtable.android.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.PasswordGeneratorProcessor
import com.ladsers.passtable.android.databinding.ActivityPasswordGeneratorBinding

class PasswordGeneratorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPasswordGeneratorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.root.title = getString(R.string.ui_ct_passwordGenerator)
        binding.toolbar.root.navigationIcon =
            ContextCompat.getDrawable(this, R.drawable.ic_back_arrow)
        setSupportActionBar(binding.toolbar.root)
        binding.toolbar.root.setNavigationOnClickListener { finish() }

        binding.svLayout.setOnScrollChangeListener { _, _, y, _, oldY ->
            if (y > oldY || y < oldY) binding.toolbar.root.elevation = 7f
            if (y == 0) binding.toolbar.root.elevation = 0f
        }

        binding.btOk.setOnClickListener {
            val intent = Intent().putExtra(PasswordGeneratorProcessor.ResultKey, "some test string")
            setResult(RESULT_OK, intent)
            finish()
        }
    }
}