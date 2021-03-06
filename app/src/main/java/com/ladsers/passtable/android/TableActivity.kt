package com.ladsers.passtable.android

import DataItem
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

    private val EditActivity_EditMode = 10
    private val EditActivity_AddMode = 11

    private var idForActivityResult = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = intent.getParcelableExtra<Uri>("fileUri")
        if (uri == null) showErrDialog("UriIsNull") //TODO
        else {
            val inputStream = contentResolver.openInputStream(uri)
            cryptData =
                BufferedReader(InputStreamReader(inputStream)).readText() //is protection required?
            uriStr = uri.toString()
            checkFileProcess()
        }
    }

    private fun checkFileProcess() {
        /* Testing for errors in the file. */
        table = DataTableAndroid(uriStr, "/test", cryptData)
        when (table.open()) {
            2 -> showErrDialog("InvalidFileVer") //TODO
            -2 -> showErrDialog("CorruptedFile") //TODO
            else -> askPassword()
        }
    }

    private fun showMsgDialog(text: String, title: String = "") {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(text)
        if (title.isNotEmpty()) builder.setTitle(title)
        builder.setPositiveButton("OK") { _, _ -> }
        builder.show()
    }

    private fun showErrDialog(text: String, title: String = "") {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(text)
        if (title.isNotEmpty()) builder.setTitle(title)
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { _, _ -> finish() }
        builder.show()
    }

    private fun askPassword(isInvalidPassword: Boolean = false) {
        val builder = AlertDialog.Builder(this)
        val binding = DialogAskpasswordBinding.inflate(layoutInflater)
        builder.setView(binding.root)
        builder.setTitle("Enter master password")
        if (isInvalidPassword) binding.tvInvalidPassword.visibility = View.VISIBLE
        var closedViaOk = false
        builder.setPositiveButton("OK") { _, _ ->
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
        table = DataTableAndroid(uriStr, masterPass, cryptData)
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

        mtList = mutableListOf()
        mtList.addAll(table.getData()) // TODO: fix this in library
        adapter = TableAdapter(mtList, { id -> showCard(id) },
            { id -> showPassword(id) })
        binding.rvTable.adapter = adapter
    }

    private fun showCard(id: Int) {
        val builder = AlertDialog.Builder(this)
        val binding = DialogItemBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.tbTag.text = table.getData(id, "t") //TODO: change to colored circles

        val n = table.getData(id, "n")
        val l = table.getData(id, "l")
        val p = table.getData(id, "p")

        if (n.isNotEmpty()) binding.tbNote.text = n
        else {
            binding.tbNote.text = "<no note>" //TODO: change to string const and add text styles
            binding.btCopyNote.visibility = View.INVISIBLE
        }

        if (l.isNotEmpty()) binding.tbLogin.text = l
        else {
            binding.tbLogin.visibility = View.INVISIBLE
            binding.btCopyLogin.visibility = View.INVISIBLE
        }

        if (p.isNotEmpty()) binding.tbPassword.text = "********"
        else {
            binding.tbPassword.visibility = View.INVISIBLE
            binding.btCopyPassword.visibility = View.INVISIBLE
            binding.btShowPassword.visibility = View.INVISIBLE
        }

        binding.btCopyNote.setOnClickListener { toClipboard(id, "n") }
        binding.btCopyLogin.setOnClickListener { toClipboard(id, "l") }
        binding.btCopyPassword.setOnClickListener { toClipboard(id, "p") }
        binding.btShowPassword.setOnClickListener {
            binding.tbPassword.text = if (binding.tbPassword.text == "********") p else "********"
        }

        builder.show().apply {
            this.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            binding.btEdit.setOnClickListener {
                editItem(id)
                this.dismiss()
            }
            binding.btDelete.setOnClickListener { /*TODO*/ }

            binding.btOk.setOnClickListener { this.dismiss() }
        }
    }

    private fun showPassword(id: Int) {
        mtList[id].password = if (mtList[id].password == "/yes") table.getData(id, "p")
        else "/yes"
        adapter.notifyItemChanged(id)
    }

    private fun toClipboard(id: Int, key: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(key, table.getData(id, key))
        clipboard.setPrimaryClip(clip)

        val text = when (key) {
            "n" -> "Note copied" //TODO
            "l" -> "Login copied"
            "p" -> "Password copied"
            else -> return
        }
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    private fun editItem(id: Int) {
        idForActivityResult = id

        val intent = Intent(this, EditActivity::class.java)
        intent.putExtra("dataTag", table.getData(id, "t"))
        intent.putExtra("dataNote", table.getData(id, "n"))
        intent.putExtra("dataLogin", table.getData(id, "l"))
        intent.putExtra("dataPassword", table.getData(id, "p"))

        intent.putExtra("modeEdit", true)
        startActivityForResult(intent, EditActivity_EditMode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            EditActivity_EditMode -> {
                if (resultCode == RESULT_OK) {
                    if (data == null) showMsgDialog("No data received. Not saved.") //TODO
                    else {
                        val newTag = data.getStringExtra("newDataTag")
                        val newNote = data.getStringExtra("newDataNote")
                        val newLogin = data.getStringExtra("newDataLogin")
                        val newPassword = data.getStringExtra("newDataPassword")
                        if (newTag != null && newNote != null &&
                            newLogin != null && newPassword != null
                        )
                            savingEdit(idForActivityResult, newTag, newNote, newLogin, newPassword)
                        else showMsgDialog("Not all data received. Not saved.") //TODO
                        idForActivityResult = -1
                    }
                } else {
                    Toast.makeText(applicationContext, "Changes were not saved", Toast.LENGTH_SHORT)
                        .show() //TODO
                    showCard(idForActivityResult)
                    idForActivityResult = -1
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun savingEdit(
        id: Int,
        newTag: String,
        newNote: String,
        newLogin: String,
        newPassword: String
    ) {
        table.setData(id, "t", newTag)
        table.setData(id, "n", newNote)
        table.setData(id, "l", newLogin)
        table.setData(id, "p", newPassword)

        mtList[id].tag = newTag
        mtList[id].note = newNote
        mtList[id].login = newLogin
        mtList[id].password = newPassword

        adapter.notifyItemChanged(id)
        showCard(id)
        //TODO

        Toast.makeText(applicationContext, "Saved", Toast.LENGTH_SHORT).show() //TODO
    }
}