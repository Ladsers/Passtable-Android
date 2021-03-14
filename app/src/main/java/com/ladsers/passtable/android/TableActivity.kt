package com.ladsers.passtable.android

import DataItem
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.ladsers.passtable.android.databinding.ActivityTableBinding
import com.ladsers.passtable.android.databinding.DialogAskpasswordBinding
import com.ladsers.passtable.android.databinding.DialogItemBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class TableActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTableBinding
    private lateinit var table: DataTableAndroid

    private lateinit var uriStr: String
    private lateinit var cryptData: String

    private lateinit var adapter: TableAdapter
    private lateinit var mtList: MutableList<DataItem>

    private var editId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = intent.getParcelableExtra<Uri>("fileUri")
        if (uri == null) showErrDialog(getString(R.string.dlg_err_uriIsNull))
        else {
            binding.toolbar.root.title = getFileName(uri) ?: getString(R.string.app_info_appName)
            binding.toolbar.root.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_close)
            setSupportActionBar(binding.toolbar.root)
            binding.toolbar.root.setNavigationOnClickListener { finish() }

            val inputStream = contentResolver.openInputStream(uri)
            cryptData =
                BufferedReader(InputStreamReader(inputStream)).readText() //is protection required?
            uriStr = uri.toString()
            checkFileProcess()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_table, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.btAdd -> {
                addItem()
                true
            }
            R.id.btSaveAs -> {
                //TODO
                true
            }
            R.id.btClone -> {
                //TODO
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkFileProcess() {
        /* Testing for errors in the file. */
        table = DataTableAndroid(uriStr, "/test", cryptData, contentResolver)
        when (table.open()) {
            2 -> showErrDialog(getString(R.string.dlg_err_invalidFileVer))
            -2 -> showErrDialog(getString(R.string.dlg_err_corruptedFile))
            else -> askPassword()
        }
    }

    private fun showMsgDialog(text: String, title: String = "") {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(text)
        if (title.isNotEmpty()) builder.setTitle(title)
        builder.setPositiveButton(getString(R.string.app_bt_ok)) { _, _ -> }
        builder.show()
    }

    private fun showErrDialog(text: String, title: String = "") {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(text)
        if (title.isNotEmpty()) builder.setTitle(title)
        builder.setCancelable(false)
        builder.setPositiveButton(getString(R.string.app_bt_ok)) { _, _ -> finish() }
        builder.show()
    }

    private fun askPassword(isInvalidPassword: Boolean = false) {
        val builder = AlertDialog.Builder(this)
        val binding = DialogAskpasswordBinding.inflate(layoutInflater)
        builder.setView(binding.root)
        builder.setTitle(getString(R.string.dlg_title_enterMasterPassword))
        if (isInvalidPassword) binding.tvInvalidPassword.visibility = View.VISIBLE
        var closedViaOk = false
        builder.setPositiveButton(getString(R.string.app_bt_ok)) { _, _ ->
            openProcess(binding.etPassword.text.toString())
            closedViaOk = true
        }
        builder.setOnDismissListener { if (!closedViaOk) finish() }

        builder.show().apply {
            this.setCanceledOnTouchOutside(false)

            this.window!!.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            )
            binding.etPassword.requestFocus()

            val button = this.getButton(AlertDialog.BUTTON_POSITIVE)
            button.isEnabled = false
            binding.etPassword.doAfterTextChanged { x ->
                button.isEnabled = x.toString().isNotEmpty()
            }
        }

    }

    private fun openProcess(masterPass: String) {
        table = DataTableAndroid(uriStr, masterPass, cryptData, contentResolver)
        when (table.open()) {
            0 -> workWithRecyclerView()
            3 -> askPassword(true)
        }
    }

    private fun workWithRecyclerView() {
        binding.rvTable.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )

        mtList = table.getData()
        adapter = TableAdapter(mtList, { id -> showCard(id) },
            { id -> showPassword(id) })
        binding.rvTable.adapter = adapter
    }

    private fun showCard(id: Int) {
        val builder = AlertDialog.Builder(this)
        val binding = DialogItemBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.vTag.setBackgroundColor(getColor(colorSelectionByTagCode(table.getData(id, "t"))))

        val n = table.getData(id, "n")
        val l = table.getData(id, "l")
        val p = table.getData(id, "p")

        if (n.isNotEmpty()) binding.tbNote.text = n
        else {
            binding.tbNote.text = getString(R.string.app_com_itemHasNoNote) //TODO: add text styles
            binding.btCopyNote.visibility = View.INVISIBLE
        }

        if (l.isNotEmpty()) binding.tbLogin.text = l
        else {
            binding.tbLogin.visibility = View.INVISIBLE
            binding.btCopyLogin.visibility = View.INVISIBLE
        }

        if (p.isNotEmpty()) binding.tbPassword.text = getString(R.string.app_com_passwordSecret)
        else {
            binding.tbPassword.visibility = View.INVISIBLE
            binding.btCopyPassword.visibility = View.INVISIBLE
            binding.btShowPassword.visibility = View.INVISIBLE
        }

        binding.btCopyNote.setOnClickListener { toClipboard(id, "n") }
        binding.btCopyLogin.setOnClickListener { toClipboard(id, "l") }
        binding.btCopyPassword.setOnClickListener { toClipboard(id, "p") }
        binding.btShowPassword.setOnClickListener {
            binding.tbPassword.text = if (binding.tbPassword.text ==
                getString(R.string.app_com_passwordSecret)) p else getString(R.string.app_com_passwordSecret)
        }

        builder.show().apply {
            this.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            binding.btEdit.setOnClickListener {
                editId = id
                editItem()
                this.dismiss()
            }
            binding.btDelete.setOnClickListener {
                removeItem(id, this)
            }

            binding.btOk.setOnClickListener { this.dismiss() }
        }
    }

    private fun showPassword(id: Int) {
        mtList[id].password = if (mtList[id].password == "/yes") table.getData(id, "p")
        else "/yes"
        adapter.notifyItemChanged(id)
        if (id == mtList.lastIndex) binding.rvTable.scrollToPosition(mtList.lastIndex)
    }

    private fun toClipboard(id: Int, key: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(key, table.getData(id, key))
        clipboard.setPrimaryClip(clip)

        val text = when (key) {
            "n" -> getString(R.string.ui_msg_noteCopied)
            "l" -> getString(R.string.ui_msg_loginCopied)
            "p" -> getString(R.string.ui_msg_passwordCopied)
            else -> return
        }
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    private fun parseDataFromEditActivity(data: Intent?): List<String>? {
        return if (data == null) {
            showMsgDialog(getString(R.string.dlg_err_noDataReceived), getString(R.string.dlg_title_notSaved))
            null
        }
        else {
            val newTag = data.getStringExtra("newDataTag")
            val newNote = data.getStringExtra("newDataNote")
            val newLogin = data.getStringExtra("newDataLogin")
            val newPassword = data.getStringExtra("newDataPassword")
            if (newTag != null && newNote != null && newLogin != null && newPassword != null)
                listOf(newTag, newNote, newLogin, newPassword)
            else {
                showMsgDialog(getString(R.string.dlg_err_someDataNull), getString(R.string.dlg_title_notSaved))
                null
            }
        }
    }

    private fun editItem() {
        val intent = Intent(this, EditActivity::class.java)
        intent.putExtra("dataTag", table.getData(editId, "t"))
        intent.putExtra("dataNote", table.getData(editId, "n"))
        intent.putExtra("dataLogin", table.getData(editId, "l"))
        intent.putExtra("dataPassword", table.getData(editId, "p"))

        intent.putExtra("modeEdit", true)
        editActivityResult.launch(intent)
    }

    private val editActivityResult = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = parseDataFromEditActivity(result.data) ?: return@registerForActivityResult

            table.setData(editId, "t", data[0])
            table.setData(editId, "n", data[1])
            table.setData(editId, "l", data[2])
            table.setData(editId, "p", data[3])

            mtList[editId].tag = data[0]
            mtList[editId].note = data[1]
            mtList[editId].login = data[2]
            mtList[editId].password = if (data[3].isNotEmpty()) "/yes" else "/no"

            adapter.notifyItemChanged(editId)
            showCard(editId)

            saving() // TODO: save error check
        } else {
            showCard(editId)
            Toast.makeText(
                this, getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addItem(){
        val intent = Intent(this, EditActivity::class.java)
        addActivityResult.launch(intent)
    }

    private val addActivityResult = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = parseDataFromEditActivity(result.data) ?: return@registerForActivityResult

            table.add(data[0], data[1], data[2], data[3])

            val hasPassword = if (data[3].isNotEmpty()) "/yes" else "/no"
            mtList.add(DataItem(data[0], data[1], data[2], hasPassword))
            adapter.notifyItemInserted(mtList.lastIndex)
            binding.rvTable.postDelayed(Runnable{
                binding.rvTable.smoothScrollToPosition(mtList.lastIndex)
            }, 500)

            saving() // TODO: save error check
        } else {
            Toast.makeText(
                this, getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saving(): Boolean {
        // save and save as in one fun?
        when (table.save()) {
            0 -> {
                Toast.makeText(applicationContext, getString(R.string.ui_msg_saved), Toast.LENGTH_SHORT).show()
                return true
            }
            2 -> showMsgDialog(getString(R.string.dlg_err_saveIdentity), getString(R.string.dlg_title_saveFailed))
            3 -> {
                showMsgDialog(
                    getString(R.string.dlg_err_saveSpecifiedDir),
                    getString(R.string.dlg_title_saveFailed)
                ) //TODO: remove?
                return true
            }
            -2 -> showMsgDialog(getString(R.string.dlg_err_saveEncryptionProblem), getString(R.string.dlg_title_saveFailed))
            -3 -> showMsgDialog(getString(R.string.dlg_err_saveWriting), getString(R.string.dlg_title_saveFailed)) //TODO: choose a new path
            5 -> {} //TODO
            6 -> {} //TODO
        }
        return false
    }

    private fun removeItem(id: Int, alertDialog: AlertDialog){
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.dlg_msg_permanentRemoval))
        builder.setTitle(getString(R.string.dlg_title_areYouSure))
        builder.setPositiveButton(getString(R.string.app_bt_yes)) { _, _ ->
            alertDialog.dismiss()

            table.delete(id)
            mtList.removeAt(id)
            adapter.notifyItemRemoved(id)

            saving() // TODO: save error check
        }
        builder.setNegativeButton(getString(R.string.app_bt_no)) { _, _ -> }
        builder.show()
    }
}