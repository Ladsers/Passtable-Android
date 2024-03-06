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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.adapters.TableAdapter
import com.ladsers.passtable.android.callbacks.ReorderCallback
import com.ladsers.passtable.android.callbacks.SearchDiffCallback
import com.ladsers.passtable.android.components.BackupManager
import com.ladsers.passtable.android.components.BiometricAuth
import com.ladsers.passtable.android.components.Searcher
import com.ladsers.passtable.android.components.menus.DataItemMenu
import com.ladsers.passtable.android.components.tableActivity.TableInitInfo
import com.ladsers.passtable.android.containers.DataTableAndroid
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.containers.RecentFiles
import com.ladsers.passtable.android.databinding.ActivityTableBinding
import com.ladsers.passtable.android.dialogs.ErrorDlg
import com.ladsers.passtable.android.dialogs.FileCreatorDlg
import com.ladsers.passtable.android.dialogs.MessageDlg
import com.ladsers.passtable.android.dialogs.PrimaryPasswordDlg
import com.ladsers.passtable.android.enums.SearchStatus
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
    private lateinit var dataItemMenu: DataItemMenu
    private lateinit var searcher: Searcher
    private lateinit var reorderCallback: ReorderCallback

    private lateinit var nothingFoundDelay: Runnable

    private val smoothAnimItemLimit = 150 // to speed up work with big files

    private var lastEditId = -1
    private var saveAsMode = false
    private var afterRemoval = false
    //private var escPressed = false
    private var disableElevation = false
    private var quickView = false

    /* Vars for lock the file system */
    private var isBackgrounded = false
    private var disableLockFileSystem = true
    private var backgroundSecs = 0L

    public fun getSearchStatus() = searcher.searchStatus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ParamStorage.getBool(this, Param.PREVENT_SCREEN_CAPTURE)) { // prevent screen capture
            val flagSecure = WindowManager.LayoutParams.FLAG_SECURE
            window.setFlags(flagSecure, flagSecure)
        }
        binding = ActivityTableBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
        /* Init essential components */
        messageDlg = MessageDlg(this, window)
        biometricAuth = BiometricAuth(
            this,
            this,
            { loginCompleted() },
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
        dataItemMenu = DataItemMenu(this, messageDlg, { id -> editItem(id = id) },
            { id, note -> deleteItem(id, note) })

        /* Get file path (uri) */
        @Suppress("DEPRECATION") var uri =
            if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra("fileUri", Uri::class.java)
            else intent.getParcelableExtra<Uri>("fileUri")

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

        /* Configure toolbar */
        binding.toolbar.root.title = getFileName(mainUri) ?: getString(R.string.app_info_appName)
        binding.toolbar.root.navigationIcon = ContextCompat.getDrawable(
            this,
            R.drawable.ic_close
        )
        setSupportActionBar(binding.toolbar.root)
        binding.toolbar.root.setNavigationOnClickListener { finish() }

        /* Init runnable vars */
        nothingFoundDelay =
            Runnable { binding.notificationNothingFound.clInfo.visibility = View.VISIBLE }

        /* Try to read file data */
        try {
            val inputStream = contentResolver.openInputStream(mainUri)
            cryptData = BufferedReader(InputStreamReader(inputStream)).readText()
        } catch (e: Exception) {
            RecentFiles.remove(this, mainUri)
            ErrorDlg.showCritical(messageDlg, this, getString(R.string.dlg_err_unableToOpenFile))
            return
        }

        val newFile = intent.getBooleanExtra("newFile", false)
        if (newFile) {
            // ask password for new file
            primaryPasswordDlg.show(PrimaryPasswordDlg.Mode.NEW, mainUri)
        } else checkFileProcess() // further opening steps
    }

    /**
     * Creating a new file. Called after successfully closing the primary password dialog.
     */
    private fun creationFileProcess(primaryPassword: String) {
        RecentFiles.add(this, mainUri)
        table = DataTableAndroid(mainUri.toString(), primaryPassword, cryptData, contentResolver)
        if (!saving(firstSave = true)) return
        if (primaryPasswordDlg.isNeedRememberPassword) biometricAuth.activateAuth(primaryPassword)
        else loginCompleted()
    }

    /**
     * Testing for errors in the file. If everything is fine with the file, request the primary
     * password or start biometric authentication.
     */
    private fun checkFileProcess() {
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
            if (isNeedUpdate) {
                RecentFiles.remove(this, mainUri)
                ErrorDlg.showCritical(messageDlg, this, getString(R.string.dlg_err_needAppUpdate))
            } else {
                //getBooleanExtra for loop protection
                val foundBackup = if (!intent.getBooleanExtra("restored", false))
                    BackupManager(this).find(mainUri) else null

                foundBackup?.let {
                    ErrorDlg.showRestoreBackup(messageDlg, this, it, mainUri)
                } ?: let {
                    RecentFiles.remove(this, mainUri)
                    ErrorDlg.showCritical(messageDlg, this, getString(R.string.dlg_err_fileDamaged))
                }
            }
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

    /**
     * Opening an existing file. Called after successfully closing the primary password dialog or
     * successful biometric authentication.
     */
    private fun openProcess(primaryPassword: String) {
        table = DataTableAndroid(mainUri.toString(), primaryPassword, cryptData, contentResolver)
        when (table.fill()) {
            0 -> {
                if (primaryPasswordDlg.isNeedRememberPassword)
                    biometricAuth.activateAuth(primaryPassword)
                else loginCompleted()
            }
            3 -> primaryPasswordDlg.show(PrimaryPasswordDlg.Mode.OPEN, incorrectPassword = true)
        }
    }

    /**
     * The last step of opening or creating a file. If there are problems with the file, then
     * the activity ends before this method.
     */
    private fun loginCompleted() {
        disableLockFileSystem = false // lock the file system is now active

        // create backup if successfully logged in
        if (!intent.getBooleanExtra("restored", false))
            BackupManager(this).create(mainUri, cryptData)

        /* Configure widget */
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

        /* Configure table adapter */
        itemList = table.getData()
        dataItemMenu.attachData(table, itemList)
        adapter = TableAdapter(itemList, dataItemMenu)
        binding.rvTable.adapter = adapter
        (binding.rvTable.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        /* Init remaining components */
        reorderCallback = ReorderCallback(itemList)
        val touchHelper = ItemTouchHelper(reorderCallback)
        touchHelper.attachToRecyclerView(binding.rvTable)

        searcher = Searcher(
            this,
            binding,
            itemList,
            table,
            { notifyUser() },
            { mtListOld -> notifyDataSetChanged(mtListOld) })

        /* Notify user */
        TableInitInfo.showKeyboardShortcuts(this, binding)
        notifyUser()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_table, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /* Toolbar menu */
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

    private fun parseDataFromEditActivity(data: Intent?): DataItem? {
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
                DataItem(newTag, newNote, newUsername, newPassword)
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

    private fun editItem(id: Int? = null, blockClosing: Boolean = false) {
        id?.let { lastEditId = it }

        val tableId = if (itemList[lastEditId].id == -1) lastEditId else itemList[lastEditId].id
        val intent = Intent(this, EditActivity::class.java)
        intent.putExtra("dataTag", table.getTag(tableId))
        intent.putExtra("dataNote", table.getNote(tableId))
        intent.putExtra("dataUsername", table.getUsername(tableId))
        intent.putExtra("dataPassword", table.getPassword(tableId))

        intent.putExtra("modeEdit", true)
        intent.putExtra("blockClosing", blockClosing)
        disableLockFileSystem = true // for Table activity
        editActivityResult.launch(intent)
    }

    /**
     * Action after closing the Edit activity.
     */
    private val editActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        disableLockFileSystem = false // turn on again
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

        if (needToLock(result.data)) {
            Toast.makeText(
                this, getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
            ).show()
            lockFile()
            return@registerForActivityResult
        }

        val data = parseDataFromEditActivity(result.data) ?: return@registerForActivityResult

        val tableId = if (itemList[lastEditId].id == -1) lastEditId else itemList[lastEditId].id
        table.setData(tableId, data.tag, data.note, data.username, data.password)
        saving()

        if (searcher.checkItemCanBeShown(data)) {
            itemList[lastEditId] = data
            itemList[lastEditId].password = if (data.password.isNotEmpty()) "/yes" else "/no"

            (binding.rvTable.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = true
            adapter.notifyItemChanged(lastEditId)
            binding.rvTable.post {
                (binding.rvTable.itemAnimator as SimpleItemAnimator).supportsChangeAnimations =
                    false
            }
        } else {
            itemList.removeAt(lastEditId)
            adapter.notifyItemRemoved(lastEditId)
            adapter.notifyItemRangeChanged(lastEditId, adapter.itemCount)
            notifyUser()
        }
    }

    private fun addItem() {
        val intent = Intent(this, EditActivity::class.java)
        searcher.getSingleTag()?.let { tag -> intent.putExtra("dataTag", tag.index.toString()) } // preselect tag
        disableLockFileSystem = true // for Table activity
        addActivityResult.launch(intent)
    }

    /**
     * Action after closing the Edit activity.
     */
    private val addActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        disableLockFileSystem = false // turn on again
        if (result.resultCode == Activity.RESULT_OK) {
            if (needToLock(result.data)) {
                Toast.makeText(
                    this, getString(R.string.ui_msg_canceled), Toast.LENGTH_SHORT
                ).show()
                lockFile()
                return@registerForActivityResult
            }

            val data = parseDataFromEditActivity(result.data) ?: return@registerForActivityResult

            table.add(data.tag, data.note, data.username, data.password)
            lastEditId = table.getSize() - 1

            if (searcher.checkItemCanBeShown(data)) {
                data.password = if (data.password.isNotEmpty()) "/yes" else "/no"
                val id = if (getSearchStatus() == SearchStatus.NONE) -1 else lastEditId
                itemList.add(DataItem(data.tag, data.note, data.username, data.password, id))
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

    private fun deleteItem(id: Int, note: String) {
        val maxChars = 12 // limit for name of file
        val title = when (true) {
            (note.length > maxChars) -> getString( // need to crop name
                R.string.dlg_title_deleteItemFormat,
                note.take(maxChars - 1) + "…"
            )
            note.isBlank() -> getString(R.string.dlg_title_deleteItem) // no name
            else -> getString(R.string.dlg_title_deleteItemFormat, note) // no need to crop name
        }

        /* Confirmation dialog */
        messageDlg.create(title, getString(R.string.dlg_msg_permanentAction))
        messageDlg.addPositiveBtn(
            getString(R.string.app_bt_delete),
            R.drawable.ic_delete
        ) {
            val tableId = if (itemList[id].id == -1) id else itemList[id].id
            table.delete(tableId)

            if (itemList[id].id != -1) { // id correction for other elements in search mode
                val tl = itemList.toList()
                for (i in id + 1..tl.lastIndex) { // after the deleted item
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

            afterRemoval = true // if saving is not successful, then it is needed for the fixer
            saving()
        }
        messageDlg.addNegativeBtn(
            getString(R.string.app_bt_cancel),
            R.drawable.ic_close
        ) {}
        messageDlg.show()
    }

    private fun saving(
        newPath: String? = null, // to save to another file
        newPassword: String? = null, // to save to another file
        firstSave: Boolean = false // show special message
    ): Boolean {
        val resCode = when (true) {
            (newPath != null && newPassword == null) -> table.save(newPath)
            (newPath != null && newPassword != null) -> table.save(newPath, newPassword)
            else -> table.save()
        }
        when (resCode) {
            0 -> {
                afterRemoval = false // reset flag
                Toast.makeText(
                    applicationContext,
                    if (firstSave) getString(R.string.ui_msg_fileCreated)
                    else getString(R.string.ui_msg_saved),
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
            2, -2 -> fixSaveErrEncryption() // try to fix it
            -3 -> fixSaveErrWrite() // try to fix it
        }
        return false
    }

    /**
     * For saving data to a new file. Called when "save as" or fixing save errors.
     */
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
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            if (!saveAsMode) saving() // if the user canceled the necessary creation of another file, return err message again.
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
        contentResolver.takePersistableUriPermission(tree, perms) // for recent files

        val file = fileCreatorDlg.createFile(tree)

        if (saveAsMode) primaryPasswordDlg.show( // if "save as" mode, then ask new primary password
            PrimaryPasswordDlg.Mode.SAVE_AS,
            file
        ) else saveToOtherFileProcess(file, null) // no need to ask new primary password
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

    /**
     * Try to fix the error that occurred when writing data to a file
     */
    private fun fixSaveErrEncryption() {
        disableLockFileSystem = true // when correcting errors, the lock should not be triggered
        /* Force reset search mode */
        val mtListOld = itemList.toList()
        itemList.clear()
        itemList.addAll(table.getData())
        notifyUser()
        notifyDataSetChanged(mtListOld)
        searcher.clearSearch()

        if (!afterRemoval) { // occurred after editing the item
            messageDlg.create(
                getString(R.string.dlg_title_encryptionError),
                getString(R.string.dlg_err_unsupportedChar)
            )
            messageDlg.addPositiveBtn(
                getString(R.string.app_bt_tryEditLastItem),
                R.drawable.ic_edit
            ) { editItem(blockClosing = true) } // try to edit item again
            messageDlg.addNegativeBtn(
                getString(R.string.app_bt_undoLastAction),
                R.drawable.ic_undo
            ) { fixSaveErrEncryptionUndo() } // just revert to the last successful save.
            messageDlg.disableSkip()
            messageDlg.show()
        } else { // occurred after deleting the item
            messageDlg.create(
                getString(R.string.dlg_title_encryptionError),
                getString(R.string.dlg_err_tryAddDelete)
            )
            messageDlg.addPositiveBtn(
                getString(R.string.app_bt_undoLastAction),
                R.drawable.ic_undo
            ) { fixSaveErrEncryptionUndo() } // just revert to the last successful save.
            messageDlg.disableSkip()
            messageDlg.show()
        }
    }

    private fun fixSaveErrEncryptionUndo() {
        table.fill() // roll back to the last saved version of the file
        val mtListOld = itemList.toList()
        itemList.clear()
        itemList.addAll(table.getData())
        notifyUser()
        notifyDataSetChanged(mtListOld)
        afterRemoval = false
        disableLockFileSystem = false
    }

    /**
     * Try to fix the error that occurred when writing the file itself. It mainly occurs when
     * the application does not have enough permissions to write to the specified directory.
     */
    private fun fixSaveErrWrite() {
        disableLockFileSystem = true // when correcting errors, the lock should not be triggered

        messageDlg.create(
            getString(R.string.dlg_title_writeError),
            getString(R.string.dlg_err_noPermissionsToWriteFile)
        )
        messageDlg.addPositiveBtn(
            getString(R.string.app_bt_createNewFile),
            R.drawable.ic_new_file
        ) {
            saveAsMode = false // error fixing mode
            fileCreatorDlg.askName(getFileName(mainUri), false) // save to new directory
        }
        messageDlg.addNegativeBtn(
            getString(R.string.app_bt_ignoreAndCloseFile),
            R.drawable.ic_exit
        ) { finish() } // ignore and close the file
        messageDlg.disableSkip()
        messageDlg.show()
    }

    /**
     * Part of lock the file system. Used to check if the lock is triggered while on the
     * Edit activity.
     */
    private fun needToLock(intent: Intent?): Boolean {
        return (intent?.getBooleanExtra("needToLock", false)) ?: return false
    }

    override fun onPause() {
        super.onPause()

        /* Lock the file system */
        if (!disableLockFileSystem) {
            isBackgrounded = true
            backgroundSecs = Date().time / 1000
        }
    }

    override fun onResume() {
        super.onResume()

        /* Lock the file system */
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
        intent.removeExtra("newFile") // if the file was created in this session
        recreate() // ask the primary password again
    }

    /**
     * Show a list of items, "no items", "nothing found" depending on the situation.
     */
    private fun notifyUser() {
        if (itemList.size == 0) {
            if (getSearchStatus() == SearchStatus.TAG_QUERY || getSearchStatus() == SearchStatus.TEXT_QUERY) {
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
            else {
                if (itemList.size < smoothAnimItemLimit) binding.rvTable.smoothScrollToPosition(0)
                else binding.rvTable.scrollToPosition(0) // quick, but ugly
            }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (getSearchStatus() != SearchStatus.NONE) {
                searcher.clearSearch()
                return
            }

            finish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_N -> {
                if (event?.isCtrlPressed ?: return super.onKeyDown(keyCode, event)) {
                    addItem()
                }
            }
            KeyEvent.KEYCODE_ESCAPE -> if (getSearchStatus() == SearchStatus.NONE) finish()
            KeyEvent.KEYCODE_MOVE_HOME -> binding.rvTable.smoothScrollToPosition(0)
            KeyEvent.KEYCODE_MOVE_END -> binding.rvTable.smoothScrollToPosition(itemList.lastIndex)
        }

        searcher.onKeyDown(keyCode, event) // shortcuts for interacting with search

        return super.onKeyDown(keyCode, event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // if the physical keyboard is connected while the app is running, then show hotkeys information
        TableInitInfo.showKeyboardShortcuts(this, binding)
    }
}