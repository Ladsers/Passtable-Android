package com.ladsers.passtable

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    private lateinit var filePicker: FilePicker
    private lateinit var fileRepository: FileRepository

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        filePicker = FilePicker(this)
        filePicker.configureFlutterChannel(flutterEngine)

        fileRepository = FileRepository(contentResolver)
        fileRepository.configureFlutterChannel(flutterEngine)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        filePicker.onActivityResult(requestCode, resultCode, data)
    }
}