package com.ladsers.passtable

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class FilePicker(private val activity: Activity) {
    private val CHANNEL = "file_picker_channel"
    private var result: MethodChannel.Result? = null
    private val activityForResultCode = 100

    fun configureFlutterChannel(flutterEngine: FlutterEngine) {
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "pickFile" -> {
                    this.result = result
                    openFileExplorer(pickDirectory = false)
                }

                "pickDirectory" -> {
                    this.result = result
                    openFileExplorer(pickDirectory = true)
                }

                else -> result.notImplemented()
            }
        }
    }

    private fun openFileExplorer(pickDirectory: Boolean) {
        val docsDir =
            Uri.parse("content://com.android.externalstorage.documents/document/primary:Documents")

        val intent = if (pickDirectory) Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, docsDir)
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
        else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, docsDir)
            putExtra("android.content.extra.SHOW_ADVANCED", true)
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }

        activity.startActivityForResult(intent, activityForResultCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != activityForResultCode) {
            result?.success(null)
            return
        }

        data?.data?.let { uri ->
            val permissions = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            activity.contentResolver.takePersistableUriPermission(uri, permissions)
            result?.success(uri.toString())
        } ?: run {
            result?.success(null)
        }
    }
}