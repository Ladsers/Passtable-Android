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
import android.widget.Toast
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
    private lateinit var recentMps: MutableList<Boolean>
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
        recentMps = mutableListOf()
        adapter = RecentAdapter(
                recentUri,
                recentDate,
                recentMps,
                this,
                { id, flag -> openRecentFile(id, flag) },
                { id, resCode -> popupAction(id, resCode) })
        binding.rvRecent.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        if (afterSelecting) afterSelecting = false
        else refreshRecentList()
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

        var uri = result.data?.data ?: return@registerForActivityResult //TODO: err msg
        val perms = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, perms)

        if (newFile) uri = fileCreator.createFile(uri)

        val intent = Intent(this, TableActivity::class.java)
        intent.putExtra("fileUri", uri)
        intent.putExtra("newFile", newFile)
        startActivity(intent)
    }

    private fun openRecentFile(id: Int, resCode: Int) {
        when (resCode) {
            0 -> { // ok
                val intent = Intent(this, TableActivity::class.java)
                intent.putExtra("fileUri", recentUri[id])
                intent.putExtra("newFile", false)
                startActivity(intent)
            }
            1 -> { // file from gdisk is lost
                refreshRecentList()
                Toast.makeText(
                    this, getString(R.string.ui_msg_recentFilesUpdated), Toast.LENGTH_SHORT
                ).show()
            }
            2 -> { // local file is lost
                val builder = AlertDialog.Builder(this)
                builder.setMessage(getString(R.string.dlg_err_fileLost))
                builder.setPositiveButton(getString(R.string.app_bt_ok)) { _, _ -> }
                builder.setOnDismissListener { removeFromRecentList(id) }
                builder.show()
            }
        }
    }

    private fun popupAction(id: Int, resCode: Int){
        when (resCode){
            1 -> { // remove from list
                removeFromRecentList(id)
            }
            2 -> { // forget password
                RecentFiles.forgetMpEncrypted(this, recentUri[id])
                recentMps[id] = false
                adapter.notifyItemChanged(id)
            }
        }
    }

    private fun refreshRecentList(){
        recentUri.clear()
        recentUri.addAll(RecentFiles.loadUri(this))
        recentDate.clear()
        recentDate.addAll(RecentFiles.loadDate(this))
        recentMps.clear()
        recentMps.addAll(RecentFiles.loadMpsEncrypted(this))
        adapter.notifyDataSetChanged()
    }

    private fun removeFromRecentList(id: Int){
        RecentFiles.remove(this, recentUri[id])
        recentUri.removeAt(id)
        recentDate.removeAt(id)
        recentMps.removeAt(id)
        adapter.notifyItemRemoved(id)
        adapter.notifyItemRangeChanged(id, adapter.itemCount)
    }
}