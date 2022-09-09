package com.ladsers.passtable.android.activities

import android.app.Activity
import android.content.*
import android.content.res.Configuration
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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.adapters.TableAdapter
import com.ladsers.passtable.android.callbacks.SearchDiffCallback
import com.ladsers.passtable.android.components.BiometricAuth
import com.ladsers.passtable.android.components.DataPanel
import com.ladsers.passtable.android.components.SnackbarManager
import com.ladsers.passtable.android.containers.DataTableAndroid
import com.ladsers.passtable.android.containers.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.containers.RecentFiles
import com.ladsers.passtable.android.databinding.ActivityTableBinding
import com.ladsers.passtable.android.dialogs.ErrorDlg
import com.ladsers.passtable.android.dialogs.FileCreatorDlg
import com.ladsers.passtable.android.dialogs.PrimaryPasswordDlg
import com.ladsers.passtable.android.dialogs.MessageDlg
import com.ladsers.passtable.android.extensions.getFileName
import com.ladsers.passtable.android.extensions.getFileNameWithExt
import com.ladsers.passtable.lib.DataItem
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
    private lateinit var primaryPasswordDlg: PrimaryPasswordDlg
    private lateinit var fileCreatorDlg: FileCreatorDlg
    private lateinit var messageDlg: MessageDlg
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

    private enum class ClipboardKey { NOTE, USERNAME, PASSWORD }

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
        messageDlg = MessageDlg(this, window)
        biometricAuth = BiometricAuth(
            this,
            this,
            { loginSucceeded() },
            { mp -> openProcess(mp) },
            { primaryPasswordDlg.show(PrimaryPasswordDlg.Mode.OPEN, canRememberPass = false) })
        primaryPasswordDlg = PrimaryPasswordDlg(
            this,
            contentResolver,
            this,
            window,
            biometricAuth,
            { password -> creationFileProcess(password) },
            { password -> openProcess(password) },
            { newPath, newPass -> saveToOtherFileProcess(newPath, newPass) }
        )
        fileCreatorDlg = FileCreatorDlg(
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
            ErrorDlg.showCritical(messageDlg, this, getString(R.string.dlg_err_filePathNotReceived))
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
            ErrorDlg.showCritical(messageDlg, this, getString(R.string.dlg_err_unableToOpenFile))
            return
        }

        val newFile = intent.getBooleanExtra("newFile", false)
        if (newFile) primaryPasswordDlg.show(PrimaryPasswordDlg.Mode.NEW, mainUri) else checkFileProcess()
    }

    override fun onBackPressed() {
        if (tagFilter.any { it } || searchMode) openSearchPanel() else {
            if (!escPressed) super.onBackPressed()
            else {
                Toast.makeText(this, getString(R.string.ui_msg_ctrlQToClose), Toast.LENGTH_SHORT)
                    .show()
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
                fileCreatorDlg.askName(getFileName(mainUri))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun creationFileProcess(masterPass: String) {
        RecentFiles.add(this, mainUri)
        table = DataTableAndroid(mainUri.toString(), masterPass, cryptData, contentResolver)
        if (!saving(firstSave = true)) return
        if (primaryPasswordDlg.isNeedRememberPassword) biometricAuth.activateAuth(masterPass)
        else loginSucceeded()
    }

    private fun openProcess(masterPass: String) {
        table = DataTableAndroid(mainUri.toString(), masterPass, cryptData, contentResolver)
        when (table.fill()) {
            0 -> {
                if (primaryPasswordDlg.isNeedRememberPassword) biometricAuth.activateAuth(masterPass)
                else loginSucceeded()
            }
            3 -> primaryPasswordDlg.show(PrimaryPasswordDlg.Mode.OPEN, incorrectPassword = true)
        }
    }

    private fun checkFileProcess() {
        /* Testing for errors in the file. */
        val fileExtension = getString(R.string.app_com_fileExtension)
        getFileNameWithExt(mainUri)?.let { it ->
            if (!it.endsWith(fileExtension)) {
                ErrorDlg.showCritical(messageDlg, this, getString(R.string.dlg_err_fileTypeUnsupported))
                return
            }
        }

        fun badResult(isNeedUpdate: Boolean = false) {
            RecentFiles.remove(this, mainUri)
            val msg =
                getString(if (isNeedUpdate) R.string.dlg_err_needAppUpdate else R.string.dlg_err_fileDamaged)
            ErrorDlg.showCritical(messageDlg, this, msg)
        }

        fun goodResult(){
            if (!quickView && ParamStorage.getBool(this, Param.REMEMBER_RECENT_FILES)) {
                RecentFiles.add(this, mainUri)
                val mpEncrypted = RecentFiles.getLastPasswordEncrypted(this)
                if (mpEncrypted.isNullOrBlank()) primaryPasswordDlg.show(PrimaryPasswordDlg.Mode.OPEN)
                else biometricAuth.startAuth(mpEncrypted)
            } else primaryPasswordDlg.show(PrimaryPasswordDlg.Mode.OPEN, canRememberPass = false)
        }

        table = DataTableAndroid(mainUri.toString(), "/test", cryptData, contentResolver)
        try {
            when (table.fill()) {
                2 -> badResult(true)
                -2 -> badResult()
                else -> goodResult()
            }
        } catch (E: Exception) {
            badResult()
        }
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
        showInfoKeyboardShortcuts()
        notifyUser()
    }

    private fun popupAction(id: Int, resCode: Int){
        val tableId = if (mtList[id].id == -1) id else mtList[id].id

        fun show(strResource: Int, data: String, key: ClipboardKey) {
            messageDlg.quickDialog(
                getString(strResource),
                data,
                { toClipboard(id, key) },
                posText = getString(R.string.app_bt_copy),
                negText = getString(R.string.app_bt_close),
                posIcon = R.drawable.ic_copy
            )
        }

        when (resCode){
            1 -> show(R.string.app_com_note, table.getNote(tableId), ClipboardKey.NOTE)
            2 -> show(R.string.app_com_username, table.getUsername(tableId), ClipboardKey.USERNAME)
            3 -> show(R.string.app_com_password, table.getPassword(tableId), ClipboardKey.PASSWORD)
            4 -> toClipboard(id, ClipboardKey.NOTE)
            5 -> toClipboard(id, ClipboardKey.USERNAME)
            6 -> toClipboard(id, ClipboardKey.PASSWORD)
            7 -> { // edit
                editId = id
                editItem()
            }
            8 -> deleteItem(id, table.getNote(tableId))
            9 -> dataPanel.show(table.getUsername(tableId), table.getPassword(tableId))
        }
    }

    private fun showPassword(id: Int) {
        val tableId = if (mtList[id].id == -1) id else mtList[id].id
        mtList[id].password = if (mtList[id].password == "/yes") table.getPassword(tableId)
        else "/yes"
        adapter.notifyItemChanged(id)
        if (id == mtList.lastIndex) binding.rvTable.scrollToPosition(mtList.lastIndex)
    }

    private fun toClipboard(id: Int, key: ClipboardKey) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val tableId = if (mtList[id].id == -1) id else mtList[id].id

        val data = when (key) {
            ClipboardKey.NOTE -> table.getNote(tableId)
            ClipboardKey.USERNAME -> table.getUsername(tableId)
            ClipboardKey.PASSWORD -> table.getPassword(tableId)
        }

        val clip = ClipData.newPlainText("data", data)
        clipboard.setPrimaryClip(clip)

        val msg = when (key) {
            ClipboardKey.NOTE -> getString(R.string.ui_msg_noteCopied)
            ClipboardKey.USERNAME -> getString(R.string.ui_msg_usernameCopied)
            ClipboardKey.PASSWORD  -> getString(R.string.ui_msg_passwordCopied)
        }
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun parseDataFromEditActivity(data: Intent?): List<String>? {
        return if (data == null) {
            ErrorDlg.show(
                messageDlg,
                getString(R.string.dlg_title_changesNotSaved),
                getString(R.string.dlg_err_couldNotReadData)
            )
            null
        } else {
            val newTag = data.getStringExtra("newDataTag")
            val newNote = data.getStringExtra("newDataNote")
            val newLogin = data.getStringExtra("newDataUsername")
            val newPassword = data.getStringExtra("newDataPassword")
            if (newTag != null && newNote != null && newLogin != null && newPassword != null)
                listOf(newTag, newNote, newLogin, newPassword)
            else {
                ErrorDlg.show(
                    messageDlg,
                    getString(R.string.dlg_title_changesNotSaved),
                    getString(R.string.dlg_err_couldNotReadData)
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
        intent.putExtra("dataTag", table.getTag(tableId))
        intent.putExtra("dataNote", table.getNote(tableId))
        intent.putExtra("dataUsername", table.getUsername(tableId))
        intent.putExtra("dataPassword", table.getPassword(tableId))

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
            mtList[editId].username = data[2]
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

    private fun deleteItem(id: Int, note: String) {
        val maxChars = 12
        val title = when (true) {
            note.length > maxChars -> getString(R.string.dlg_title_deleteItemFormat, note.take(maxChars - 1) + "…")
            note.isBlank() -> getString(R.string.dlg_title_deleteItem)
            else -> getString(R.string.dlg_title_deleteItemFormat, note)
        }

        messageDlg.create(title, getString(R.string.dlg_msg_permanentAction))
        messageDlg.addPositiveBtn(
            getString(R.string.app_bt_delete),
            R.drawable.ic_delete
        ) {
            val tableId = if (mtList[id].id == -1) id else mtList[id].id
            table.delete(tableId)

            if (mtList[id].id != -1) { // id correction for search result
                val tl = mtList.toList()
                for (i in id + 1..tl.lastIndex) {
                    mtList[i] =
                        DataItem(tl[i].tag, tl[i].note, tl[i].username, tl[i].password, tl[i].id - 1)
                }
            }

            mtList.removeAt(id)
            adapter.notifyItemRemoved(id)
            adapter.notifyItemRangeChanged(id, adapter.itemCount)
            notifyUser()

            afterRemoval = true
            saving()
        }
        messageDlg.addNegativeBtn(
            getString(R.string.app_bt_cancel),
            R.drawable.ic_close
        ) {}
        messageDlg.show()
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
            if (query.isNotEmpty()) binding.etSearch.postDelayed(searchWithDelay, 500)
            else searchByData(query)
        }

        binding.btSearch.setOnClickListener { openSearchPanel() }
    }

    private fun searchByTag(tagCode: Int, btTag: MaterialButton) {
        tagFilter[tagCode] = !tagFilter[tagCode]
        val tf = tagFilter[tagCode]

        fun getIcon(tag: Int, tagChecked: Int) =
            ContextCompat.getDrawable(this, if (tf) tagChecked else tag)

        btTag.icon = when (btTag) {
            binding.btTagRed -> getIcon(R.drawable.ic_tag_red, R.drawable.ic_tag_red_checked)
            binding.btTagGreen -> getIcon(R.drawable.ic_tag_green, R.drawable.ic_tag_green_checked)
            binding.btTagBlue -> getIcon(R.drawable.ic_tag_blue, R.drawable.ic_tag_blue_checked)
            binding.btTagYellow -> getIcon(R.drawable.ic_tag_yellow, R.drawable.ic_tag_yellow_checked)
            binding.btTagPurple -> getIcon(R.drawable.ic_tag_purple, R.drawable.ic_tag_purple_checked)
            else -> null
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

        if (result.data?.data == null) {
            Toast.makeText(this, getString(R.string.dlg_err_filePathNotReceived), Toast.LENGTH_LONG).show()
            if (!saveAsMode) saving() // return err message again.
            return@registerForActivityResult
        }

        val tree = result.data?.data!!
        val perms = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(tree, perms)

        val file = fileCreatorDlg.createFile(tree)

        if (saveAsMode) primaryPasswordDlg.show(
            PrimaryPasswordDlg.Mode.SAVE_AS,
            file
        ) else saveToOtherFileProcess(file, null)
    }

    private fun saveToOtherFileProcess(newPath: Uri, newPassword: String?) {
        if (!saving(newPath.toString(), newPassword)) return
        mainUri = newPath
        RecentFiles.add(this, mainUri)
        this.binding.toolbar.root.title = getFileName(mainUri)
        if (newPassword != null && primaryPasswordDlg.isNeedRememberPassword) {
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
            messageDlg.create(getString(R.string.dlg_title_encryptionError), getString(R.string.dlg_err_unsupportedChar))
            messageDlg.addPositiveBtn(
                getString(R.string.app_bt_tryEditLastItem),
                R.drawable.ic_edit
            ) { editItem(blockClosing = true) }
            messageDlg.addNegativeBtn(
                getString(R.string.app_bt_undoLastAction),
                R.drawable.ic_undo
            ) { fixSaveErrEncryptionUndo() }
            messageDlg.disableSkip()
            messageDlg.show()
        } else {
            messageDlg.create(getString(R.string.dlg_title_encryptionError), getString(R.string.dlg_err_tryAddDelete))
            messageDlg.addPositiveBtn(
                getString(R.string.app_bt_undoLastAction),
                R.drawable.ic_undo
            ) { fixSaveErrEncryptionUndo() }
            messageDlg.disableSkip()
            messageDlg.show()
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

        messageDlg.create(
            getString(R.string.dlg_title_writeError),
            getString(R.string.dlg_err_noPermissionsToWriteFile)
        )
        messageDlg.addPositiveBtn(
            getString(R.string.app_bt_createNewFile),
            R.drawable.ic_new_file
        ) {
            saveAsMode = false
            fileCreatorDlg.askName(getFileName(mainUri), false)
        }
        messageDlg.addNegativeBtn(
            getString(R.string.app_bt_ignoreAndCloseFile),
            R.drawable.ic_exit
        ) { finish() }
        messageDlg.disableSkip()
        messageDlg.show()
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
            binding.notificationNothingFound.clInfo.removeCallbacks(nothingFoundDelay)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        showInfoKeyboardShortcuts()
    }

    private fun showInfoKeyboardShortcuts() {
        val param = Param.INITIAL_INFO_KEYBOARD_SHORTCUTS
        if (ParamStorage.getBool(this, param) &&
            resources.configuration.keyboard == Configuration.KEYBOARD_QWERTY
        ) {
            val info = getString(R.string.app_info_keyboardShortcuts)
            SnackbarManager.showInitInfo(this, binding.root, param, info)
        }
    }
}