package com.ladsers.passtable.android.activities

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.adapters.TableAdapter
import com.ladsers.passtable.android.callbacks.SearchDiffCallback
import com.ladsers.passtable.android.components.BiometricAuth
import com.ladsers.passtable.android.components.DataPanel
import com.ladsers.passtable.android.components.TagPanel
import com.ladsers.passtable.android.components.tableActivity.TableClipboard
import com.ladsers.passtable.android.components.tableActivity.TableInitInfo
import com.ladsers.passtable.android.containers.DataTableAndroid
import com.ladsers.passtable.android.containers.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.containers.RecentFiles
import com.ladsers.passtable.android.databinding.ActivityTableBinding
import com.ladsers.passtable.android.dialogs.ErrorDlg
import com.ladsers.passtable.android.dialogs.FileCreatorDlg
import com.ladsers.passtable.android.dialogs.MessageDlg
import com.ladsers.passtable.android.dialogs.PrimaryPasswordDlg
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
    private lateinit var itemList: MutableList<DataItem>
    private lateinit var biometricAuth: BiometricAuth
    private lateinit var primaryPasswordDlg: PrimaryPasswordDlg
    private lateinit var fileCreatorDlg: FileCreatorDlg
    private lateinit var messageDlg: MessageDlg
    private lateinit var dataPanel: DataPanel
    private lateinit var tagPanel: TagPanel
    private lateinit var tableClipboard: TableClipboard

    private lateinit var nothingFoundDelay: Runnable

    private var editId = -1
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
        messageDlg = MessageDlg(this, window)
        biometricAuth = BiometricAuth(
            this,
            this,
            { loginSucceeded() },
            { primaryPassword -> openProcess(primaryPassword) },
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
        if (newFile) primaryPasswordDlg.show(
            PrimaryPasswordDlg.Mode.NEW,
            mainUri
        ) else checkFileProcess()
    }

    override fun onBackPressed() {
        if (tagPanel.isAnyTagActive() || tagPanel.searchModeIsActive) tagPanel.switchPanel() else {
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

    private fun creationFileProcess(primaryPassword: String) {
        RecentFiles.add(this, mainUri)
        table = DataTableAndroid(mainUri.toString(), primaryPassword, cryptData, contentResolver)
        if (!saving(firstSave = true)) return
        if (primaryPasswordDlg.isNeedRememberPassword) biometricAuth.activateAuth(primaryPassword)
        else loginSucceeded()
    }

    private fun openProcess(primaryPassword: String) {
        table = DataTableAndroid(mainUri.toString(), primaryPassword, cryptData, contentResolver)
        when (table.fill()) {
            0 -> {
                if (primaryPasswordDlg.isNeedRememberPassword)
                    biometricAuth.activateAuth(primaryPassword)
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
                ErrorDlg.showCritical(
                    messageDlg,
                    this,
                    getString(R.string.dlg_err_fileTypeUnsupported)
                )
                return
            }
        }

        fun badResult(isNeedUpdate: Boolean = false) {
            RecentFiles.remove(this, mainUri)
            val msg =
                getString(if (isNeedUpdate) R.string.dlg_err_needAppUpdate else R.string.dlg_err_fileDamaged)
            ErrorDlg.showCritical(messageDlg, this, msg)
        }

        fun goodResult() {
            if (!quickView && ParamStorage.getBool(this, Param.REMEMBER_RECENT_FILES)) {
                RecentFiles.add(this, mainUri)
                val passEncrypted = RecentFiles.getLastPasswordEncrypted(this)
                if (passEncrypted.isNullOrBlank()) primaryPasswordDlg.show(PrimaryPasswordDlg.Mode.OPEN)
                else biometricAuth.startAuth(passEncrypted)
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

        itemList = table.getData()
        adapter = TableAdapter(itemList, { id, resCode -> popupAction(id, resCode) },
            { id -> showPassword(id) })
        tableClipboard = TableClipboard(this, itemList, table)
        tagPanel = TagPanel(
            this,
            binding,
            itemList,
            table,
            { searchQuery -> notifyUser(searchQuery) },
            { mtListOld -> notifyDataSetChanged(mtListOld) })
        tagPanel.init()
        binding.rvTable.adapter = adapter
        (binding.rvTable.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        TableInitInfo.showKeyboardShortcuts(this, binding)
        notifyUser()
    }

    private fun popupAction(id: Int, resCode: Int) {
        val tableId = if (itemList[id].id == -1) id else itemList[id].id

        fun show(strResource: Int, data: String, key: TableClipboard.Key) {
            messageDlg.quickDialog(
                getString(strResource),
                data,
                { tableClipboard.copy(id, key) },
                posText = getString(R.string.app_bt_copy),
                negText = getString(R.string.app_bt_close),
                posIcon = R.drawable.ic_copy
            )
        }

        val keyNote = TableClipboard.Key.NOTE
        val keyUsername = TableClipboard.Key.USERNAME
        val keyPassword = TableClipboard.Key.PASSWORD
        when (resCode) {
            1 -> show(R.string.app_com_note, table.getNote(tableId), keyNote)
            2 -> show(R.string.app_com_username, table.getUsername(tableId), keyUsername)
            3 -> show(R.string.app_com_password, table.getPassword(tableId), keyPassword)
            4 -> tableClipboard.copy(id, keyNote)
            5 -> tableClipboard.copy(id, keyUsername)
            6 -> tableClipboard.copy(id, keyPassword)
            7 -> { // edit
                editId = id
                editItem()
            }
            8 -> deleteItem(id, table.getNote(tableId))
            9 -> dataPanel.show(table.getUsername(tableId), table.getPassword(tableId))
        }
    }

    private fun showPassword(id: Int) {
        val tableId = if (itemList[id].id == -1) id else itemList[id].id
        itemList[id].password = if (itemList[id].password == "/yes") table.getPassword(tableId)
        else "/yes"
        adapter.notifyItemChanged(id)
        if (id == itemList.lastIndex) binding.rvTable.scrollToPosition(itemList.lastIndex)
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
            val newUsername = data.getStringExtra("newDataUsername")
            val newPassword = data.getStringExtra("newDataPassword")
            if (newTag != null && newNote != null && newUsername != null && newPassword != null)
                listOf(newTag, newNote, newUsername, newPassword)
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
        val tableId = if (itemList[editId].id == -1) editId else itemList[editId].id
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

        val tableId = if (itemList[editId].id == -1) editId else itemList[editId].id
        table.setData(tableId, data[0], data[1], data[2], data[3])
        saving()

        if (!tagPanel.isAnyTagActive() || tagPanel.checkTag(data[0])) {
            itemList[editId].tag = data[0]
            itemList[editId].note = data[1]
            itemList[editId].username = data[2]
            itemList[editId].password = if (data[3].isNotEmpty()) "/yes" else "/no"

            (binding.rvTable.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = true
            adapter.notifyItemChanged(editId)
            binding.rvTable.post {
                (binding.rvTable.itemAnimator as SimpleItemAnimator).supportsChangeAnimations =
                    false
            }
        } else {
            itemList.removeAt(editId)
            adapter.notifyItemRemoved(editId)
            adapter.notifyItemRangeChanged(editId, adapter.itemCount)
            notifyUser()
        }
    }

    private fun addItem() {
        val intent = Intent(this, EditActivity::class.java)
        tagPanel.findActiveTag().let { intent.putExtra("dataTag", it) }
        if (tagPanel.searchModeIsActive) tagPanel.switchPanel()
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

            if (!tagPanel.isAnyTagActive() || tagPanel.checkTag(data[0])) {
                val hasPassword = if (data[3].isNotEmpty()) "/yes" else "/no"
                val id = if (!tagPanel.isAnyTagActive()) -1 else editId
                itemList.add(DataItem(data[0], data[1], data[2], hasPassword, id))
                notifyUser()
                adapter.notifyItemInserted(itemList.lastIndex)
                binding.rvTable.postDelayed({
                    binding.rvTable.smoothScrollToPosition(itemList.lastIndex)
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
            note.length > maxChars -> getString(
                R.string.dlg_title_deleteItemFormat,
                note.take(maxChars - 1) + "…"
            )
            note.isBlank() -> getString(R.string.dlg_title_deleteItem)
            else -> getString(R.string.dlg_title_deleteItemFormat, note)
        }

        messageDlg.create(title, getString(R.string.dlg_msg_permanentAction))
        messageDlg.addPositiveBtn(
            getString(R.string.app_bt_delete),
            R.drawable.ic_delete
        ) {
            val tableId = if (itemList[id].id == -1) id else itemList[id].id
            table.delete(tableId)

            if (itemList[id].id != -1) { // id correction for search result
                val tl = itemList.toList()
                for (i in id + 1..tl.lastIndex) {
                    itemList[i] =
                        DataItem(
                            tl[i].tag,
                            tl[i].note,
                            tl[i].username,
                            tl[i].password,
                            tl[i].id - 1
                        )
                }
            }

            itemList.removeAt(id)
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
            Toast.makeText(this, getString(R.string.dlg_err_filePathNotReceived), Toast.LENGTH_LONG)
                .show()
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
        val mtListOld = itemList.toList()
        itemList.clear()
        itemList.addAll(table.getData())
        notifyUser()
        notifyDataSetChanged(mtListOld)
        if (tagPanel.isAnyTagActive() || tagPanel.searchModeIsActive) tagPanel.switchPanel()

        if (!afterRemoval) {
            messageDlg.create(
                getString(R.string.dlg_title_encryptionError),
                getString(R.string.dlg_err_unsupportedChar)
            )
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
            messageDlg.create(
                getString(R.string.dlg_title_encryptionError),
                getString(R.string.dlg_err_tryAddDelete)
            )
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
        val mtListOld = itemList.toList()
        itemList.clear()
        itemList.addAll(table.getData())
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
                    if (tagPanel.isAnyTagActive()) tagPanel.switchPanel()
                    if (!tagPanel.searchModeIsActive) tagPanel.switchPanel()
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
            KeyEvent.KEYCODE_MOVE_END -> binding.rvTable.smoothScrollToPosition(itemList.lastIndex)
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
        if (itemList.size == 0) {
            if (tagPanel.isAnyTagActive() || searchQuery.isNotEmpty()) {
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
            TableInitInfo.showItemMenu(this, binding, itemList)
        }
    }

    private fun notifyDataSetChanged(mtListOld: List<DataItem>) {
        DiffUtil.calculateDiff(SearchDiffCallback(mtListOld, itemList), false)
            .dispatchUpdatesTo(adapter)
        binding.toolbar.root.elevation = 0f
        disableElevation = true
        binding.rvTable.post {
            if (!binding.rvTable.canScrollVertically(-1)) disableElevation = false
            else binding.rvTable.smoothScrollToPosition(0)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        TableInitInfo.showKeyboardShortcuts(this, binding)
    }
}