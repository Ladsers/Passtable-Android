package com.ladsers.passtable.android.dialogs

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.BackupManager

object ErrorDlg {
    fun show(messageDlg: MessageDlg, error: String, reason: String) {
        messageDlg.create(error, reason)
        messageDlg.addPositiveBtn(
            messageDlg.context.getString(R.string.app_bt_ok),
            R.drawable.ic_accept
        ) {}
        messageDlg.show()
    }

    fun showCritical(messageDlg: MessageDlg, activity: Activity, reason: String) {
        messageDlg.create(activity.getString(R.string.dlg_title_criticalError), reason)
        messageDlg.addPositiveBtn(
            activity.getString(R.string.app_bt_closeFile),
            R.drawable.ic_exit
        ) { activity.finish() }
        messageDlg.disableSkip()
        messageDlg.show()
    }

    fun showRestoreBackup(
        messageDlg: MessageDlg, activity: Activity, backupFileName: String, uriToWrite: Uri
    ) {
        messageDlg.create(
            activity.getString(R.string.dlg_title_criticalError),
            activity.getString(R.string.dlg_err_fileDamagedRestoreBackup)
        )
        messageDlg.addPositiveBtn(
            activity.getString(R.string.app_bt_restore),
            R.drawable.ic_restore
        ) {
            try {
                BackupManager(activity).restore(backupFileName, uriToWrite)
                activity.intent.putExtra("restored", true)
                activity.recreate()
            } catch (e: Exception) {
                activity.deleteFile(backupFileName)
                Toast.makeText(
                    activity,
                    activity.getString(R.string.ui_msg_failedToRestoreBackup),
                    Toast.LENGTH_LONG
                ).show()
                activity.finish()
            }
        }

        messageDlg.addNegativeBtn(
            activity.getString(R.string.app_bt_closeFile),
            R.drawable.ic_exit
        ) { activity.finish() }

        messageDlg.disableSkip()
        messageDlg.show()
    }
}