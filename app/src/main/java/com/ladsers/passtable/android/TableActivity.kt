package com.ladsers.passtable.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ladsers.passtable.android.databinding.ActivityTableBinding

class TableActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTableBinding
    private lateinit var dataTable: DataTableAndroid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}