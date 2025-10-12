package com.ladsers.passtable

import android.content.ContentResolver
import android.net.Uri
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.*
import android.provider.DocumentsContract

class FileRepository(private val contentResolver: ContentResolver) {
    private val CHANNEL = "file_repository_channel"

    fun configureFlutterChannel(flutterEngine: FlutterEngine) {
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "writeData" -> {
                    val uriString = call.argument<String>("uri")
                    val data = call.argument<String>("data")
                    if (uriString != null && data != null) {
                        result.success(writeData(uriString, data))
                    } else {
                        result.error("INVALID_ARGUMENTS", "uri or data is null", null)
                    }
                }

                "readData" -> {
                    val uriString = call.argument<String>("uri")
                    if (uriString != null) {
                        result.success(readData(uriString))
                    } else {
                        result.error("INVALID_ARGUMENTS", "uri is null", null)
                    }
                }

                "createFile" -> {
                    val treeUriString = call.argument<String>("treeUri")
                    val fileName = call.argument<String>("fileName")
                    if (treeUriString != null && fileName != null) {
                        result.success(createFile(treeUriString, fileName))
                    } else {
                        result.error("INVALID_ARGUMENTS", "treeUri or fileName is null", null)
                    }
                }

                else -> result.notImplemented()
            }
        }
    }

    private fun writeData(uriString: String, data: String): Boolean {
        val gdrivePattern = "content://com.google.android.apps.docs.storage"
        if (uriString.startsWith(gdrivePattern)) return false

        return try {
            val uri = Uri.parse(uriString)
            contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write(data)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readData(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createFile(treeUriString: String, fileName: String): String? {
        return try {
            val treeUri = Uri.parse(treeUriString)

            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

            val createdUri = DocumentsContract.createDocument(
                contentResolver,
                docUri,
                "application/octet-stream",
                fileName
            )
            createdUri.toString()
        } catch (e: Exception) {
            null
        }
    }
}

    