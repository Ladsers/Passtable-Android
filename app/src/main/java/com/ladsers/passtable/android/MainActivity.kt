package com.ladsers.passtable.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.ladsers.passtable.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var newFile: Boolean = false

    private lateinit var recentList: MutableList<Uri>
    private lateinit var adapter: RecentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //RecentFiles.clear(this)

        binding.toolbar.root.title = getString(R.string.ui_ct_home)
        //binding.toolbar.root.navigationIcon = //TODO: passtable logo
        setSupportActionBar(binding.toolbar.root)

        binding.btnOpenFile.setOnClickListener { fileWorker(false) }
        binding.btNewFile.setOnClickListener { fileWorker(true) }
        binding.btAbout.setOnClickListener { }

        binding.rvRecent.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        recentList = mutableListOf()
        adapter = RecentAdapter(recentList, this) { id -> openRecentFile(id) }
        binding.rvRecent.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        recentList.clear()
        recentList.addAll(RecentFiles.load(this))
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.btSettings -> {
                //TODO
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fileWorker(newFile: Boolean){
        this.newFile = newFile

        val intent = if (newFile) Intent(Intent.ACTION_CREATE_DOCUMENT)
        else Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/*" //TODO: try to catch only passtable files.
        fileActivityResult.launch(intent)
    }

    private val fileActivityResult = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

        var uri = result.data?.data ?: return@registerForActivityResult //TODO: err msg
        if (newFile) {
            val originalName =
                getFileNameWithExt(uri) ?: return@registerForActivityResult //TODO: err msg
            if (!originalName.endsWith(".passtable")) {
                val saveName = "$originalName.passtable"
                uri = DocumentsContract.renameDocument(contentResolver, uri, saveName)!!
            }
        }
        val perms = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, perms)

        val intent = Intent(this, TableActivity::class.java)
        intent.putExtra("fileUri", uri)
        intent.putExtra("newFile", newFile)
        startActivity(intent)
    }

    private fun openRecentFile(id: Int){
        val intent = Intent(this, TableActivity::class.java)
        intent.putExtra("fileUri", recentList[id])
        intent.putExtra("newFile", false)
        startActivity(intent)
    }
}