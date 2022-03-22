package com.ladsers.passtable.android

import DataItem
import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.InputType
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.ladsers.passtable.android.databinding.ActivityTableBinding
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
    private lateinit var msgDialog: MsgDialog
    private lateinit var dataPanel: DataPanel

    private lateinit var nothingFoundDelay: Runnable

    private var editId = -1
    private val tagFilter = MutableList(6) { false }
    private var searchMode = false
    private var saveAsMode = false
    private var afterRemoval = false
    private var escPressed = false
    private var disableElevation = false

    private var quickView = false

    private var isBackgrounded = false
    private var disableLockFileSystem = true
    private var backgroundSecs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ParamStorage.getBool(this, Param.PREVENT_SCREEN_CAPTURE)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        binding = ActivityTableBinding.inflate(layoutInflater)
        setContentView(binding.root)
        msgDialog = MsgDialog(this, window)
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
            { newPath, newPass -> saveToOtherFileProcess(newPath, newPass) }
        )
        fileCreator = FileCreator(
            this,
            contentResolver,
            window
        ) { openFileExplorer() }
        dataPanel = DataPanel(applicationContext, this)

        var uri = intent.getParcelableExtra<Uri>("fileUri")
        intent.action?.let {
            if (it == Intent.ACTION_VIEW && intent.scheme == ContentResolver.SCHEME_CONTENT) {
                uri = intent.data
                quickView = true
            }
        }
        if (uri == null) {
            showCriticalError(getString(R.string.dlg_err_uriIsNull))
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

        nothingFoundDelay =
            Runnable { binding.notificationNothingFound.clInfo.visibility = View.VISIBLE }

        try {
            val inputStream = contentResolver.openInputStream(mainUri)
            cryptData = BufferedReader(InputStreamReader(inputStream)).readText()
        } catch (e: Exception) {
            RecentFiles.remove(this, mainUri)
            showCriticalError(getString(R.string.dlg_err_unableToOpenFile))
            return
        }

        val newFile = intent.getBooleanExtra("newFile", false)
        if (newFile) mpRequester.start(MpRequester.Mode.NEW, mainUri) else checkFileProcess()
    }

    override fun onBackPressed() {
        if (tagFilter.any { it } || searchMode) openSearchPanel() else {
            if (!escPressed) super.onBackPressed()
            else {
                val msg = getString(
                    R.string.ui_msg_closeViaEscape,
                    getString(R.string.app_sh_closeFile)
                )
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
        escPressed = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_table, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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

    private fun creationFileProcess(masterPass: String) {
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
                showCriticalError(getString(R.string.dlg_err_unsupportedFile))
                return
            }
        }

        table = DataTableAndroid(mainUri.toString(), "/test", cryptData, contentResolver)
        when (table.fill()) {
            2 -> showCriticalError(getString(R.string.dlg_err_invalidFileVer))
            -2 -> {
                RecentFiles.remove(this, mainUri)
                showCriticalError(getString(R.string.dlg_err_damagedFile))
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

    private fun showError(error: String, reason: String) {
        msgDialog.create(error, reason)
        msgDialog.addPositiveBtn(
            getString(R.string.app_bt_ok),
            R.drawable.ic_accept
        ) {}
        msgDialog.show()
    }

    private fun showCriticalError(reason: String) {
        msgDialog.create(getString(R.string.dlg_title_criticalError), reason)
        msgDialog.addPositiveBtn(
            getString(R.string.app_bt_closeFile),
            R.drawable.ic_exit
        ) { finish() }
        msgDialog.disableSkip()
        msgDialog.show()
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
                if (!recyclerView.canScrollVertically(-1)) disableElevation = false
                if (!disableElevation) {
                    binding.toolbar.root.elevation =
                        if (!recyclerView.canScrollVertically(-1)) 0f else 7f
                }
            }
        })

        mtList = table.getData()
        adapter = TableAdapter(mtList, { id, resCode -> popupAction(id, resCode) },
            { id -> showPassword(id) })
        binding.rvTable.adapter = adapter
        (binding.rvTable.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        initPanel()
        notifyUser()
    }

    private fun popupAction(id: Int, resCode: Int){
        val tableId = if (mtList[id].id == -1) id else mtList[id].id
        when (resCode){
            1 -> { // show note
                msgDialog.quickDialog(
                    getString(R.string.app_com_note),
                    table.getData(tableId, "n"),
                    { toClipboard(id, "n") },
                    posText = getString(R.string.app_bt_copy),
                    negText = getString(R.string.app_bt_close),
                    posIcon = R.drawable.ic_copy
                )
            }
            2 -> { // show login
                msgDialog.quickDialog(
                    getString(R.string.app_com_login),
                    table.getData(tableId, "l"),
                    { toClipboard(id, "l") },
                    posText = getString(R.string.app_bt_copy),
                    negText = getString(R.string.app_bt_close),
                    posIcon = R.drawable.ic_copy
                )
            }
            3 -> { // show password
                msgDialog.quickDialog(
                    getString(R.string.app_com_password),
                    table.getData(tableId, "p"),
                    { toClipboard(id, "p") },
                    posText = getString(R.string.app_bt_copy),
                    negText = getString(R.string.app_bt_close),
                    posIcon = R.drawable.ic_copy
                )
            }
            4 -> toClipboard(id, "n") // copy note
            5 -> toClipboard(id, "l") // copy login
            6 -> toClipboard(id, "p") // copy password
            7 -> { // edit
                editId = id
                editItem()
            }
            8 -> removeItem(id, table.getData(tableId, "n")) // remove
            9 -> dataPanel.show(table.getData(tableId, "l"), table.getData(tableId, "p"))
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
            showError(
                getString(R.string.dlg_title_changesNotSaved),
                getString(R.string.dlg_err_noDataReceived)
            )
            null
        } else {
            val newTag = data.getStringExtra("newDataTag")
            val newNote = data.getStringExtra("newDataNote")
            val newLogin = data.getStringExtra("newDataLogin")
            val newPassword = data.getStringExtra("newDataPassword")
            if (newTag != null && newNote != null && newLogin != null && newPassword != null)
                listOf(newTag, newNote, newLogin, newPassword)
            else {
                showError(
                    getString(R.string.dlg_title_changesNotSaved),
                    getString(R.string.dlg_err_someDataNull)
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
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

        if (needToLock(result.data)) {
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

            (binding.rvTable.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = true
            adapter.notifyItemChanged(editId)
            binding.rvTable.post {
                (binding.rvTable.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }
        } else {
            mtList.removeAt(editId)
            adapter.notifyItemRemoved(editId)
            adapter.notifyItemRangeChanged(editId, adapter.itemCount)
            notifyUser()
        }
    }

    private fun addItem() {
        val intent = Intent(this, EditActivity::class.java)
        if (tagFilter.count { it } == 1) {
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
            if (needToLock(result.data)) {
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
                notifyUser()
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
    ): Boolean {
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
            2, -2 -> fixSaveErrEncryption()
            -3 -> fixSaveErrWrite()
        }
        return false
    }

    private fun removeItem(id: Int, note: String) {
        val maxChars = 12
        val title = when (true) {
            note.length > maxChars -> getString(R.string.dlg_title_removeItemWithNote, note.take(maxChars - 1) + "…")
            note.isBlank() -> getString(R.string.dlg_title_removeItem)
            else -> getString(R.string.dlg_title_removeItemWithNote, note)
        }

        msgDialog.create(title, getString(R.string.dlg_msg_permanentRemoval))
        msgDialog.addPositiveBtn(
            getString(R.string.app_bt_remove),
            R.drawable.ic_delete
        ) {
            val tableId = if (mtList[id].id == -1) id else mtList[id].id
            table.remove(tableId)

            if (mtList[id].id != -1) { // id correction for search result
                val tl = mtList.toList()
                for (i in id + 1..tl.lastIndex) {
                    mtList[i] =
                        DataItem(tl[i].tag, tl[i].note, tl[i].login, tl[i].password, tl[i].id - 1)
                }
            }

            mtList.removeAt(id)
            adapter.notifyItemRemoved(id)
            adapter.notifyItemRangeChanged(id, adapter.itemCount)
            notifyUser()

            afterRemoval = true
            saving()
        }
        msgDialog.addNegativeBtn(
            getString(R.string.app_bt_cancel),
            R.drawable.ic_close
        ) {}
        msgDialog.show()
    }

    private fun initPanel() {
        binding.btTagRed.setOnClickListener { v -> searchByTag(1, v as MaterialButton) }
        binding.btTagGreen.setOnClickListener { v -> searchByTag(2, v as MaterialButton) }
        binding.btTagBlue.setOnClickListener { v -> searchByTag(3, v as MaterialButton) }
        binding.btTagYellow.setOnClickListener { v -> searchByTag(4, v as MaterialButton) }
        binding.btTagPurple.setOnClickListener { v -> searchByTag(5, v as MaterialButton) }

        var query = ""
        val searchWithDelay = Runnable { searchByData(query) }
        binding.etSearch.doAfterTextChanged { text ->
            binding.etSearch.removeCallbacks(searchWithDelay)
            query = text.toString()
            if (query.isNotEmpty()) binding.etSearch.postDelayed(searchWithDelay, 300)
            else searchByData(query)
        }

        binding.btSearch.setOnClickListener { openSearchPanel() }
    }

    private fun searchByTag(tagCode: Int, btTag: MaterialButton) {
        tagFilter[tagCode] = !tagFilter[tagCode]
        val tf = tagFilter[tagCode]
        when (btTag) {
            binding.btTagRed -> btTag.icon = ContextCompat.getDrawable(
                this,
                if (tf) R.drawable.ic_tag_red_checked else R.drawable.ic_tag_red
            )
            binding.btTagGreen -> btTag.icon = ContextCompat.getDrawable(
                this,
                if (tf) R.drawable.ic_tag_green_checked else R.drawable.ic_tag_green
            )
            binding.btTagBlue -> btTag.icon = ContextCompat.getDrawable(
                this,
                if (tf) R.drawable.ic_tag_blue_checked else R.drawable.ic_tag_blue
            )
            binding.btTagYellow -> btTag.icon = ContextCompat.getDrawable(
                this,
                if (tf) R.drawable.ic_tag_yellow_checked else R.drawable.ic_tag_yellow
            )
            binding.btTagPurple -> btTag.icon = ContextCompat.getDrawable(
                this,
                if (tf) R.drawable.ic_tag_purple_checked else R.drawable.ic_tag_purple
            )
        }

        val mtListOld = mtList.toList()
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
            binding.btSearch.icon = ContextCompat.getDrawable(this, R.drawable.ic_search_off)
        } else {
            mtList.addAll(table.getData())
            binding.btSearch.icon = ContextCompat.getDrawable(this, R.drawable.ic_search)
        }

        notifyUser()
        notifyDataSetChanged(mtListOld)
    }

    private fun openSearchPanel() {
        if (!tagFilter.any { it }) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            searchMode = !searchMode

            binding.clTagButtons.visibility = if (searchMode) View.GONE else View.VISIBLE
            binding.etSearch.visibility = if (searchMode) View.VISIBLE else View.GONE

            if (searchMode) {
                binding.clPanel.setBackgroundColor(
                    MaterialColors.getColor(
                        binding.clPanel,
                        R.attr.editBackground
                    )
                )
                binding.etSearch.requestFocus()
                binding.etSearch.inputType = InputType.TYPE_CLASS_TEXT
                imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
                binding.btSearch.icon =
                    ContextCompat.getDrawable(this, R.drawable.ic_search_off)
            } else {
                binding.clPanel.setBackgroundColor(
                    MaterialColors.getColor(
                        binding.clPanel,
                        R.attr.panelTableBackground
                    )
                )
                binding.etSearch.clearFocus()
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                binding.etSearch.inputType = InputType.TYPE_NULL
                binding.etSearch.text.clear()
                binding.btSearch.icon = ContextCompat.getDrawable(this, R.drawable.ic_search)
            }
        } else {
            // closing tags
            for (i in 1..5) tagFilter[i] = false
            val mtListOld = mtList.toList()
            mtList.clear()
            mtList.addAll(table.getData())
            notifyUser()
            notifyDataSetChanged(mtListOld)

            with(binding) {
                val context = this@TableActivity
                btSearch.icon = ContextCompat.getDrawable(context, R.drawable.ic_search)

                btTagRed.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_red)
                btTagGreen.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_green)
                btTagBlue.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_blue)
                btTagYellow.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_yellow)
                btTagPurple.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_purple)
            }
        }
    }

    private fun searchByData(query: String) {
        val mtListOld = mtList.toList()
        mtList.clear()
        mtList.addAll(if (query.isNotEmpty()) table.searchByData(query) else table.getData())
        notifyUser(query)
        notifyDataSetChanged(mtListOld)
    }

    private fun openFileExplorer() {
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

        if (saveAsMode) mpRequester.start(
            MpRequester.Mode.SAVEAS,
            file
        ) else saveToOtherFileProcess(file, null)
    }

    private fun saveToOtherFileProcess(newPath: Uri, newPassword: String?) {
        if (!saving(newPath.toString(), newPassword)) return
        mainUri = newPath
        RecentFiles.add(this, mainUri)
        this.binding.toolbar.root.title = getFileName(mainUri)
        if (newPassword != null && mpRequester.isNeedToRemember()) {
            BiometricAuth(this, this, { disableLockFileSystem = false }, { }, { }).activateAuth(
                newPassword
            )
        } else disableLockFileSystem = false
    }

    private fun fixSaveErrEncryption() {
        disableLockFileSystem = true
        val mtListOld = mtList.toList()
        mtList.clear()
        mtList.addAll(table.getData())
        notifyUser()
        notifyDataSetChanged(mtListOld)
        if (tagFilter.any { it } || searchMode) openSearchPanel()

        if (!afterRemoval) {
            msgDialog.create(getString(R.string.dlg_title_encryptionError), getString(R.string.dlg_err_saveEncryptionProblem))
            msgDialog.addPositiveBtn(
                getString(R.string.app_bt_tryEditLastItem),
                R.drawable.ic_edit
            ) { editItem(blockClosing = true) }
            msgDialog.addNegativeBtn(
                getString(R.string.app_bt_undoLastAction),
                R.drawable.ic_undo
            ) { fixSaveErrEncryptionUndo() }
            msgDialog.disableSkip()
            msgDialog.show()
        } else {
            msgDialog.create(getString(R.string.dlg_title_encryptionError), getString(R.string.dlg_err_saveEncryptionDelete))
            msgDialog.addPositiveBtn(
                getString(R.string.app_bt_undoLastAction),
                R.drawable.ic_undo
            ) { fixSaveErrEncryptionUndo() }
            msgDialog.disableSkip()
            msgDialog.show()
        }
    }

    private fun fixSaveErrEncryptionUndo() {
        table.fill()
        val mtListOld = mtList.toList()
        mtList.clear()
        mtList.addAll(table.getData())
        notifyUser()
        notifyDataSetChanged(mtListOld)
        disableLockFileSystem = false
    }

    private fun fixSaveErrWrite() {
        disableLockFileSystem = true

        msgDialog.create(
            getString(R.string.dlg_title_writeError),
            getString(R.string.dlg_err_saveWriting)
        )
        msgDialog.addPositiveBtn(
            getString(R.string.app_bt_createNewFile),
            R.drawable.ic_new_file
        ) {
            saveAsMode = false
            fileCreator.askName(getFileName(mainUri), false)
        }
        msgDialog.addNegativeBtn(
            getString(R.string.app_bt_closeFileDataLoss),
            R.drawable.ic_exit
        ) { finish() }
        msgDialog.disableSkip()
        msgDialog.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_F -> {
                if (event?.isCtrlPressed ?: return super.onKeyDown(keyCode, event)) {
                    if (tagFilter.any { it }) openSearchPanel()
                    if (!searchMode) openSearchPanel()
                }
            }
            KeyEvent.KEYCODE_ESCAPE -> escPressed = true
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
            KeyEvent.KEYCODE_MOVE_HOME -> binding.rvTable.smoothScrollToPosition(0)
            KeyEvent.KEYCODE_MOVE_END -> binding.rvTable.smoothScrollToPosition(mtList.lastIndex)
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

    private fun lockFile() = recreate()

    private fun notifyUser(searchQuery: String = "") {
        if (mtList.size == 0) {
            if (tagFilter.any { it } || searchQuery.isNotEmpty()) {
                binding.notificationEmptyCollection.clInfo.visibility = View.GONE
                binding.notificationNothingFound.clInfo.postDelayed(nothingFoundDelay, 300)
            } else {
                binding.notificationEmptyCollection.clInfo.visibility = View.VISIBLE
                binding.notificationNothingFound.clInfo.removeCallbacks(nothingFoundDelay)
                binding.notificationNothingFound.clInfo.visibility = View.GONE
            }
        } else {
            binding.notificationEmptyCollection.clInfo.visibility = View.GONE
            binding.notificationNothingFound.clInfo.visibility = View.GONE
            showInfoItemMenu()
        }
    }

    private fun notifyDataSetChanged(mtListOld: List<DataItem>) {
        DiffUtil.calculateDiff(SearchDiffCallback(mtListOld, mtList), false)
            .dispatchUpdatesTo(adapter)
            binding.toolbar.root.elevation = 0f
            disableElevation = true
        binding.rvTable.post {
            if (!binding.rvTable.canScrollVertically(-1)) disableElevation = false
            else binding.rvTable.smoothScrollToPosition(0)
        }
    }

    private fun showInfoItemMenu() {
        val param = Param.INITIAL_INFO_ITEM_MENU
        if (ParamStorage.getBool(this, param) && mtList.isNotEmpty()) SnackbarManager.showInitInfo(
            this,
            binding.root,
            param,
            getString(R.string.app_info_itemMenu)
        )
    }
}