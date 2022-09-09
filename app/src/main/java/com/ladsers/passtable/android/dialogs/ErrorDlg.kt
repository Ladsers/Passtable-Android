package com.ladsers.passtable.android.dialogs

import android.app.Activity
import com.ladsers.passtable.android.R

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
}