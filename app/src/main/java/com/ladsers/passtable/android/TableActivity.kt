package com.ladsers.passtable.android

import DataItem
import android.app.Activity
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.InputType
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ladsers.passtable.android.databinding.ActivityTableBinding
import com.ladsers.passtable.android.databinding.DialogItemBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*


class TableActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTableBinding
    private lateinit var table: DataTableAndroid

    private lateinit var mainUri: Uri
    private lateinit var cryptData: String

    private lateinit var adapter: TableAdapter
    private lateinit var mtList: MutableList<DataItem>
    private lateinit var biometricAuth: BiometricAuth
    private lateinit var mpRequester: MpRequester
    private lateinit var fileCreator: FileCreator

    private var editId = -1
    private val tagFilter = MutableList(6) {false}
    private var searchMode = false
    private var saveAsMode = false
    private var afterRemoval = false

    private var overlayCard = false
    private var overlayRmWin = false

    private var quickView = false

    private var isBackgrounded = false
    private var disableLockFileSystem = true
    private var backgroundSecs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableBinding.inflate(layoutInflater)
        setContentView(binding.root)
        biometricAuth = BiometricAuth(
            this,
            this,
            { loginSucceeded() },
            { mp -> openProcess(mp) },
            { mpRequester.start(MpRequester.Mode.OPEN, canRememberPass = false) })
        mpRequester = MpRequester(
            this,
            contentResolver,
            this,
            window,
            biometricAuth,
            { password -> creationFileProcess(password) },
            { password -> openProcess(password) },
            { newPath -> saving(newPath) },
            { newPath, newPass -> saving(newPath, newPass) },
            { newPath -> completeSavingToOtherFile(newPath) }
        )
        fileCreator = FileCreator(
            this,
            contentResolver,
            window
        ) { openFileExplorer() }
        turnOnPanel()

        var uri = intent.getParcelableExtra<Uri>("fileUri")
        intent.action?.let {
            if (it == Intent.ACTION_VIEW && intent.scheme == ContentResolver.SCHEME_CONTENT) {
                uri = intent.data
                quickView = true
            }
        }
        if (uri == null) {
            showErrDialog(getString(R.string.dlg_err_uriIsNull))
            return
        }

        mainUri = uri!!
        binding.toolbar.root.title = getFileName(mainUri) ?: getString(R.string.app_info_appName)
        binding.toolbar.root.navigationIcon = ContextCompat.getDrawable(
            this,
            R.drawable.ic_close
        )
        setSupportActionBar(binding.toolbar.root)
        binding.toolbar.root.setNavigationOnClickListener { finish() }

        try {
            val inputStream = contentResolver.openInputStream(mainUri)
            cryptData = BufferedReader(InputStreamReader(inputStream)).readText()
        } catch (e: Exception) {
            RecentFiles.remove(this, mainUri)
            showErrDialog(getString(R.string.dlg_err_unableToOpenFile))
            return
        }

        val newFile = intent.getBooleanExtra("newFile", false)
        if (newFile) mpRequester.start(MpRequester.Mode.NEW, mainUri) else checkFileProcess()
    }

    override fun onBackPressed() {
        if (tagFilter.any { it } || searchMode) openSearchPanel() else super.onBackPressed()
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
                disableLockFileSystem = true
                saveAsMode = true
                fileCreator.askName(getFileName(mainUri))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun creationFileProcess(masterPass: String){
        RecentFiles.add(this, mainUri)
        table = DataTableAndroid(mainUri.toString(), masterPass, cryptData, contentResolver)
        if (!saving(firstSave = true)) return
        if (mpRequester.isNeedToRemember()) biometricAuth.activateAuth(masterPass)
        else loginSucceeded()
    }

    private fun openProcess(masterPass: String) {
        table = DataTableAndroid(mainUri.toString(), masterPass, cryptData, contentResolver)
        when (table.fill()) {
            0 -> {
                if (mpRequester.isNeedToRemember()) biometricAuth.activateAuth(masterPass)
                else loginSucceeded()
            }
            3 -> mpRequester.start(MpRequester.Mode.OPEN, incorrectPassword = true)
        }
    }

    private fun checkFileProcess() {
        /* Testing for errors in the file. */
        val fileExtension = getString(R.string.app_com_fileExtension)
        getFileNameWithExt(mainUri)?.let { it ->
            if (!it.endsWith(fileExtension)) {
                showErrDialog(getString(R.string.dlg_err_unsupportedFile))
                return
            }
        }

        table = DataTableAndroid(mainUri.toString(), "/test", cryptData, contentResolver)
        when (table.fill()) {
            2 -> showErrDialog(getString(R.string.dlg_err_invalidFileVer))
            -2 -> {
                RecentFiles.remove(this, mainUri)
                showErrDialog(getString(R.string.dlg_err_corruptedFile))
            }
            else -> {
                if (!quickView && ParamStorage.getBool(this, Param.REMEMBER_RECENT_FILES)) {
                    RecentFiles.add(this, mainUri)
                    val mpEncrypted = RecentFiles.getLastMpEncrypted(this)
                    if (mpEncrypted.isNullOrBlank()) mpRequester.start(MpRequester.Mode.OPEN)
                    else biometricAuth.startAuth(mpEncrypted)
                } else mpRequester.start(MpRequester.Mode.OPEN, canRememberPass = false)
            }
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

    private fun loginSucceeded() {
        disableLockFileSystem = false
        binding.rvTable.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.rvTable.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                binding.toolbar.root.elevation =
                    if (!recyclerView.canScrollVertically(-1)) 0f else 7f
            }
        })

        mtList = table.getData()
        adapter = TableAdapter(mtList, { id -> if (!overlayCard) showCard(id) },
            { id -> showPassword(id) })
        binding.rvTable.adapter = adapter
    }

    private fun showCard(id: Int) {
        val builder = AlertDialog.Builder(this)
        val binding = DialogItemBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        val tableId = if (mtList[id].id == -1) id else mtList[id].id
        binding.vTag.setBackgroundColor(
            getColor(
                colorSelectionByTagCode(
                    table.getData(
                        tableId,
                        "t"
                    )
                )
            )
        )

        val n = table.getData(tableId, "n")
        val l = table.getData(tableId, "l")
        val p = table.getData(tableId, "p")

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

        if (p.isNotEmpty()) {
            binding.tbPassword.text =
                if (ParamStorage.getBool(this, Param.SHOW_PASSWORD_IN_CARD)) p
                else getString(R.string.app_com_passwordSecret)
        }
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

        overlayCard = true
        builder.setOnDismissListener { overlayCard = false }

        builder.show().apply {
            this.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            binding.btEdit.setOnClickListener {
                editId = id
                editItem()
                this.dismiss()
            }
            binding.btDelete.setOnClickListener {
                if (!overlayRmWin) removeItem(id, this)
            }

            binding.btOk.setOnClickListener { this.dismiss() }
        }
    }

    private fun showPassword(id: Int) {
        val tableId = if (mtList[id].id == -1) id else mtList[id].id
        mtList[id].password = if (mtList[id].password == "/yes") table.getData(tableId, "p")
        else "/yes"
        adapter.notifyItemChanged(id)
        if (id == mtList.lastIndex) binding.rvTable.scrollToPosition(mtList.lastIndex)
    }

    private fun toClipboard(id: Int, key: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val tableId = if (mtList[id].id == -1) id else mtList[id].id
        val clip = ClipData.newPlainText(key, table.getData(tableId, key))
        clipboard.setPrimaryClip(clip)

        val msg = when (key) {
            "n" -> getString(R.string.ui_msg_noteCopied)
            "l" -> getString(R.string.ui_msg_loginCopied)
            "p" -> getString(R.string.ui_msg_passwordCopied)
            else -> return
        }
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun parseDataFromEditActivity(data: Intent?): List<String>? {
        return if (data == null) {
            showMsgDialog(
                getString(R.string.dlg_err_noDataReceived),
                getString(R.string.dlg_title_notSaved)
            )
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
                showMsgDialog(
                    getString(R.string.dlg_err_someDataNull),
                    getString(R.string.dlg_title_notSaved)
                )
                null
            }
        }
    }

    private fun needToLock(intent: Intent?): Boolean {
        return (intent?.getBooleanExtra("needToLock", false)) ?: return false
    }

    private fun editItem(blockClosing: Boolean = false) {
        val tableId = if (mtList[editId].id == -1) editId else mtList[editId].id
        val intent = Intent(this, EditActivity::class.java)
        intent.putExtra("dataTag", table.getData(tableId, "t"))
        intent.putExtra("dataNote", table.getData(tableId, "n"))
        intent.putExtra("dataLogin", table.getData(tableId, "l"))
        intent.putExtra("dataPassword", table.getData(tableId, "p"))

        intent.putExtra("modeEdit", true)
        intent.putExtra("blockClosing", blockClosing)
        disableLockFileSystem = true
        editActivityResult.launch(intent)
    }

    private val editActivityResult = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) { result ->
        disableLockFileSystem = false
        if (result.resultCode == Activity.RESULT_OK) {
            if (needToLock(result.data)){
                Toast.makeText(
                    this, getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
                ).show()
                lockFile()
                return@registerForActivityResult
            }

            val data = parseDataFromEditActivity(result.data) ?: return@registerForActivityResult

            val tableId = if (mtList[editId].id == -1) editId else mtList[editId].id
            table.setData(tableId, data[0], data[1], data[2], data[3])
            saving()

            if (!tagFilter.any { it } || tagFilter[data[0].toInt()]) {
                mtList[editId].tag = data[0]
                mtList[editId].note = data[1]
                mtList[editId].login = data[2]
                mtList[editId].password = if (data[3].isNotEmpty()) "/yes" else "/no"

                adapter.notifyItemChanged(editId)
            } else {
                mtList.removeAt(editId)
                adapter.notifyItemRemoved(editId)
                adapter.notifyItemRangeChanged(editId, adapter.itemCount)
            }

        } else {
            Toast.makeText(
                this, getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addItem(){
        val intent = Intent(this, EditActivity::class.java)
        if (tagFilter.count { it } == 1){
            intent.putExtra("dataTag", tagFilter.indexOf(true).toString())
        }
        if (searchMode) openSearchPanel()
        disableLockFileSystem = true
        addActivityResult.launch(intent)
    }

    private val addActivityResult = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) { result ->
        disableLockFileSystem = false
        if (result.resultCode == Activity.RESULT_OK) {
            if (needToLock(result.data)){
                Toast.makeText(
                    this, getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
                ).show()
                lockFile()
                return@registerForActivityResult
            }

            val data = parseDataFromEditActivity(result.data) ?: return@registerForActivityResult

            table.add(data[0], data[1], data[2], data[3])
            editId = table.getSize() - 1

            if (!tagFilter.any { it } || tagFilter[data[0].toInt()]) {
                val hasPassword = if (data[3].isNotEmpty()) "/yes" else "/no"
                val id = if (!tagFilter.any { it }) -1 else editId
                mtList.add(DataItem(data[0], data[1], data[2], hasPassword, id))
                adapter.notifyItemInserted(mtList.lastIndex)
                binding.rvTable.postDelayed({
                    binding.rvTable.smoothScrollToPosition(mtList.lastIndex)
                }, 500)
            }

            saving()
        } else {
            Toast.makeText(
                this, getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saving(
        newPath: String? = null,
        newPassword: String? = null,
        firstSave: Boolean = false
    ) : Boolean {
        val resCode = when (true) {
            newPath != null && newPassword == null -> table.save(newPath)
            newPath != null && newPassword != null -> table.save(newPath, newPassword)
            else -> table.save()
        }
        when (resCode) {
            0 -> {
                afterRemoval = false
                Toast.makeText(
                    applicationContext,
                    if (firstSave) getString(R.string.ui_msg_fileCreated)
                    else getString(R.string.ui_msg_saved),
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
            2, -2 -> fixSaveErrEncryption(resCode)
            -3 -> fixSaveErrFileCorrupted()
        }
        return false
    }

    private fun removeItem(id: Int, alertDialog: AlertDialog){
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.dlg_msg_permanentRemoval))
        builder.setTitle(getString(R.string.dlg_title_areYouSure))
        builder.setPositiveButton(getString(R.string.app_bt_yes)) { _, _ ->
            alertDialog.dismiss()

            val tableId = if (mtList[id].id == -1) id else mtList[id].id
            table.remove(tableId)

            if (mtList[id].id != -1) { // id correction for search result
                val tl = mtList.toList()
                for (i in id+1..tl.lastIndex){
                    mtList[i] =
                        DataItem(tl[i].tag, tl[i].note, tl[i].login, tl[i].password, tl[i].id - 1)
                }
            }

            mtList.removeAt(id)
            adapter.notifyItemRemoved(id)
            adapter.notifyItemRangeChanged(id, adapter.itemCount)

            afterRemoval = true
            saving()
        }
        builder.setNegativeButton(getString(R.string.app_bt_no)) { _, _ -> }

        overlayRmWin = true
        builder.setOnDismissListener { overlayRmWin = false }

        builder.show()
    }

    private fun turnOnPanel(){
        binding.btTagRed.setOnClickListener { v -> searchByTag(1, v as ImageButton) }
        binding.btTagGreen.setOnClickListener { v -> searchByTag(2, v as ImageButton) }
        binding.btTagBlue.setOnClickListener { v -> searchByTag(3, v as ImageButton) }
        binding.btTagYellow.setOnClickListener { v -> searchByTag(4, v as ImageButton) }
        binding.btTagPurple.setOnClickListener { v -> searchByTag(5, v as ImageButton) }

        binding.etSearch.doAfterTextChanged { text -> searchByData(text.toString())}

        binding.btSearch.setOnClickListener { openSearchPanel() }
    }

    private fun searchByTag(tagCode: Int, btTag: ImageButton) {
        tagFilter[tagCode] = !tagFilter[tagCode]
        if (tagFilter[tagCode]) {
            btTag.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tag_on))
        } else {
            btTag.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_tag_off))
        }

        mtList.clear()
        if (tagFilter.any { it }) {
            mtList.addAll(
                table.searchByTag(
                    tagFilter[1],
                    tagFilter[2],
                    tagFilter[3],
                    tagFilter[4],
                    tagFilter[5]
                )
            )
            binding.btSearch.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_tag_cancel
                )
            )
        } else {
            mtList.addAll(table.getData())
            binding.btSearch.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_search
                )
            )
        }

        adapter.notifyDataSetChanged()
    }

    private fun openSearchPanel(){
        if (!tagFilter.any { it }) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            searchMode = !searchMode

            with(binding) {
                btTagRed.visibility = if (searchMode) View.GONE else View.VISIBLE
                btTagGreen.visibility = if (searchMode) View.GONE else View.VISIBLE
                btTagBlue.visibility = if (searchMode) View.GONE else View.VISIBLE
                btTagYellow.visibility = if (searchMode) View.GONE else View.VISIBLE
                btTagPurple.visibility = if (searchMode) View.GONE else View.VISIBLE
                etSearch.visibility = if (searchMode) View.VISIBLE else View.GONE

                if (searchMode) {
                    etSearch.requestFocus()
                    etSearch.inputType = InputType.TYPE_CLASS_TEXT
                    imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
                    btSearch.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@TableActivity,
                            R.drawable.ic_close
                        )
                    )
                } else {
                    etSearch.clearFocus()
                    imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
                    etSearch.inputType = InputType.TYPE_NULL
                    etSearch.text.clear()
                    btSearch.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@TableActivity,
                            R.drawable.ic_search
                        )
                    )
                }
            }

        }
        else {
            // closing tags
            for (i in 1..5) tagFilter[i] = false
            mtList.clear()
            mtList.addAll(table.getData())
            adapter.notifyDataSetChanged()

            with(binding) {
                val context = this@TableActivity
                btSearch.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_search))

                val dr = R.drawable.ic_tag_off
                btTagRed.setImageDrawable(ContextCompat.getDrawable(context, dr))
                btTagGreen.setImageDrawable(ContextCompat.getDrawable(context, dr))
                btTagBlue.setImageDrawable(ContextCompat.getDrawable(context, dr))
                btTagYellow.setImageDrawable(ContextCompat.getDrawable(context, dr))
                btTagPurple.setImageDrawable(ContextCompat.getDrawable(context, dr))
            }
        }
    }

    private fun searchByData(query: String){
        mtList.clear()
        mtList.addAll(if (query.isNotEmpty()) table.searchByData(query) else table.getData())
        adapter.notifyDataSetChanged()
    }

    private fun openFileExplorer(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val docsDir =
                "content://com.android.externalstorage.documents/document/primary:Documents".toUri()
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, docsDir)
        }
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
        explorerResult.launch(intent)
    }

    private val explorerResult = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            if (!saveAsMode) saving() // if the user canceled the creation of another file, return err message again.
            return@registerForActivityResult
        }

        val tree = result.data?.data ?: return@registerForActivityResult //TODO: err msg
        val perms = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(tree, perms)

        val file = fileCreator.createFile(tree)

        if (saveAsMode) mpRequester.start(MpRequester.Mode.SAVEAS, file) else {
            if (saving(file.toString())) {
                disableLockFileSystem = false
                mainUri = file
                RecentFiles.add(this, mainUri)
                this.binding.toolbar.root.title = getFileName(mainUri)
            }
        }
    }

    private fun completeSavingToOtherFile(newPath: Uri){
        disableLockFileSystem = false
        mainUri = newPath
        RecentFiles.add(this, mainUri)
        this.binding.toolbar.root.title = getFileName(mainUri)
    }

    private fun fixSaveErrEncryption(errCode: Int) {
        disableLockFileSystem = true
        mtList.clear()
        mtList.addAll(table.getData())
        adapter.notifyDataSetChanged()
        if (tagFilter.any { it } || searchMode) openSearchPanel()

        val builder = AlertDialog.Builder(this)
        //TODO: show different err code
        builder.setMessage(getString(R.string.dlg_err_saveEncryptionProblem))
        builder.setTitle(getString(R.string.dlg_title_saveFailed))
        builder.setCancelable(false)
        builder.setPositiveButton(getString(R.string.app_bt_edit)) { _, _ ->
            editItem(blockClosing = true)
        }
        builder.setNegativeButton(getString(R.string.app_bt_undo)) { _, _ ->
            table.fill()
            mtList.clear()
            mtList.addAll(table.getData())
            adapter.notifyDataSetChanged()
            disableLockFileSystem = false
        }
        builder.show().apply {
            if (afterRemoval) this.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
    }

    private fun fixSaveErrFileCorrupted() {
        disableLockFileSystem = true
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.dlg_err_saveWriting))
        builder.setTitle(getString(R.string.dlg_title_saveFailed))
        builder.setCancelable(false)
        builder.setPositiveButton(getString(R.string.app_bt_ok)) { _, _ ->
            saveAsMode = false
            fileCreator.askName(getFileName(mainUri), false)}
        builder.show()
    }

   override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode){
            KeyEvent.KEYCODE_F -> {
                if (event?.isCtrlPressed ?: return super.onKeyDown(keyCode, event)) {
                    if (tagFilter.any { it }) openSearchPanel()
                    if (!searchMode) openSearchPanel()
                }
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                searchMode = true
                openSearchPanel()
            }
            KeyEvent.KEYCODE_N -> {
                if (event?.isCtrlPressed ?: return super.onKeyDown(keyCode, event)) {
                    addItem()
                }
            }
            KeyEvent.KEYCODE_Q -> {
                if (event?.isCtrlPressed ?: return super.onKeyDown(keyCode, event)) {
                    finish()
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()

        if (!disableLockFileSystem) {
            isBackgrounded = true
            backgroundSecs = Date().time / 1000
        }
    }

    override fun onResume() {
        super.onResume()

        if (isBackgrounded) {
            isBackgrounded = false

            when (ParamStorage.getInt(this, Param.LOCK_MODE)) {
                0 -> {
                    val secs = Date().time / 1000
                    val allowedTime = ParamStorage.getInt(this, Param.LOCK_SECS)
                    if (secs - backgroundSecs >= allowedTime) lockFile()
                }
                1 -> lockFile()
            }
        }
    }

    private fun lockFile() {
        intent.putExtra("fileUri", mainUri)
        intent.putExtra("newFile", false)
        finish()
        startActivity(intent)
    }
}