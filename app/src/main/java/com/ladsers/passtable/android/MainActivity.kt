package com.ladsers.passtable.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.ladsers.passtable.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var newFile: Boolean = false
    private var afterSelecting = false

    private lateinit var recentUri: MutableList<Uri>
    private lateinit var recentDate: MutableList<String>
    private lateinit var adapter: RecentAdapter
    private lateinit var askFileName: AskFileName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.root.title = getString(R.string.ui_ct_home)
        //binding.toolbar.root.navigationIcon = //TODO: passtable logo
        setSupportActionBar(binding.toolbar.root)

        binding.btnOpenFile.setOnClickListener { openFileExplorer(false) }
        binding.btNewFile.setOnClickListener { openFileExplorer(true) }
        binding.btAbout.setOnClickListener { }

        binding.rvRecent.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        recentUri = mutableListOf()
        recentDate = mutableListOf()
        adapter =
            RecentAdapter(recentUri, recentDate, this) { id, flag -> openRecentFile(id, flag) }
        binding.rvRecent.adapter = adapter

        askFileName =
            AskFileName(this, window) { uri, fileName -> createAndOpenNewFile(uri, fileName) }
    }

    override fun onResume() {
        super.onResume()

        if (afterSelecting) afterSelecting = false
        else {
            recentUri.clear()
            recentUri.addAll(RecentFiles.loadUri(this))
            recentDate.clear()
            recentDate.addAll(RecentFiles.loadDate(this))
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.btSettings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFileExplorer(newFile: Boolean) {
        this.newFile = newFile

        val intent = if (newFile) Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        else Intent(Intent.ACTION_OPEN_DOCUMENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val docsDir =
                "content://com.android.externalstorage.documents/document/primary:Documents".toUri()
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, docsDir)
        }
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)

        if (!newFile) {
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/octet-stream"
        }

        explorerResult.launch(intent)
    }

    private val explorerResult = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) { result ->
        afterSelecting = true

        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

        val uri = result.data?.data ?: return@registerForActivityResult //TODO: err msg
        val perms = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, perms)

        if (newFile) askFileName.ask(uri)
        else {
            val intent = Intent(this, TableActivity::class.java)
            intent.putExtra("fileUri", uri)
            intent.putExtra("newFile", false)
            startActivity(intent)
        }
    }

    private fun createAndOpenNewFile(uri: Uri, fileName: String) {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)

        val file = DocumentsContract.createDocument(
            contentResolver,
            docUri,
            "application/octet-stream",
            fileName
        )!!

        val intent = Intent(this, TableActivity::class.java)
        intent.putExtra("fileUri", file)
        intent.putExtra("newFile", true)
        startActivity(intent)
    }

    private fun openRecentFile(id: Int, canBeOpened: Boolean) {
        if (canBeOpened) {
            val intent = Intent(this, TableActivity::class.java)
            intent.putExtra("fileUri", recentUri[id])
            intent.putExtra("newFile", false)
            startActivity(intent)
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.dlg_err_fileLost))
            builder.setPositiveButton(getString(R.string.app_bt_ok)) { _, _ -> }
            builder.setOnDismissListener {
                RecentFiles.remove(this, recentUri[id])
                recentUri.removeAt(id)
                recentDate.removeAt(id)
                adapter.notifyItemRemoved(id)
            }
            builder.show()
        }
    }
}