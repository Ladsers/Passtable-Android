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

    private var newFile = false
    private var afterSelecting = false

    private lateinit var recentUri: MutableList<Uri>
    private lateinit var recentDate: MutableList<String>
    private lateinit var adapter: RecentAdapter
    private lateinit var fileCreator: FileCreator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.root.title = getString(R.string.ui_ct_home)
        //binding.toolbar.root.navigationIcon = //TODO: passtable logo
        setSupportActionBar(binding.toolbar.root)

        fileCreator =
            FileCreator(this, contentResolver, window) { openFileExplorer(true) }

        binding.btnOpenFile.setOnClickListener { openFileExplorer(false) }
        binding.btNewFile.setOnClickListener { fileCreator.askName() }
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
    }

    override fun onResume() {
        super.onResume()

        if (afterSelecting) afterSelecting = false
        else refreshRecentList()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.btRefresh)?.isVisible = checkLostFiles()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.btSettings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.btRefresh -> {
                refreshRecentList()
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

        var uri = result.data?.data ?: return@registerForActivityResult //TODO: err msg
        val perms = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, perms)

        if (newFile) uri = fileCreator.createFile(uri)

        val intent = Intent(this, TableActivity::class.java)
        intent.putExtra("fileUri", uri)
        intent.putExtra("newFile", newFile)
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
                invalidateOptionsMenu()
            }
            builder.show()
        }
    }

    private fun checkLostFiles(): Boolean {
        for (r in recentUri) if (getFileName(r) == null) return true
        return false
    }

    private fun refreshRecentList(){
        recentUri.clear()
        recentUri.addAll(RecentFiles.loadUri(this))
        recentDate.clear()
        recentDate.addAll(RecentFiles.loadDate(this))
        adapter.notifyDataSetChanged()
        invalidateOptionsMenu()
    }
}