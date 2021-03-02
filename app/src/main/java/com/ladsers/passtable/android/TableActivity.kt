package com.ladsers.passtable.android

import DataItem
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.ladsers.passtable.android.databinding.ActivityTableBinding
import com.ladsers.passtable.android.databinding.DialogAskpasswordBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class TableActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTableBinding
    private lateinit var table: DataTableAndroid

    private lateinit var uriStr: String
    private lateinit var cryptData: String

    private lateinit var adapter: TableAdapter
    private lateinit var mtList: MutableList<DataItem>

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
        builder.setOnDismissListener { if(!closedViaOk) finish() }

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

    private fun workWithRecyclerView(){
        binding.rvTable.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )

        mtList = mutableListOf()
        mtList.addAll(table.getData()) // TODO: fix this in library
        adapter = TableAdapter(mtList, {id -> showCard(id)},
            {id -> showPassword(id)})
        binding.rvTable.adapter = adapter
    }

    private fun showCard(id: Int){
        val builder = AlertDialog.Builder(this)
        // TODO: replace with a full layout.
        builder.setTitle(table.getData(id, "n"))
        val msg = table.getData(id, "l") + "\n\n" + table.getData(id, "p")
        builder.setMessage(msg)
        builder.setPositiveButton("OK") { _, _ -> }
        builder.show()
    }

    private fun showPassword(id: Int){
        mtList[id].password = if (mtList[id].password == "/yes") table.getData(id, "p")
        else "/yes"
        adapter.notifyItemChanged(id)
    }
}