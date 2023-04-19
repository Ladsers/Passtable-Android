package com.ladsers.passtable.android.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.AppStoreProcessor
import com.ladsers.passtable.android.containers.AppStore
import com.ladsers.passtable.lib.updater.Updater

object UpdateDlg {
    fun show(messageDlg: MessageDlg) {

        val context = messageDlg.context

        val isRuStoreInstalled = AppStoreProcessor.isInstalled(context, AppStore.RUSTORE)

        messageDlg.create(
            context.getString(R.string.dlg_title_updateAvailable),
            context.getString(R.string.dlg_msg_downloadNewVersion) //TODO
        )

        if (isRuStoreInstalled) {
            messageDlg.addPositiveBtn(
                "Open RuStore", //TODO
                R.drawable.ic_download
            ) { AppStoreProcessor.open(context, AppStore.RUSTORE) }

            messageDlg.addNeutralBtn(
                context.getString(R.string.app_bt_downloadFromGithub), //TODO
                R.drawable.ic_download
            ) { downloadFromSite(context) }
        } else {
            messageDlg.addPositiveBtn(
                context.getString(R.string.app_bt_downloadFromGithub), //TODO
                R.drawable.ic_download
            ) { downloadFromSite(context) }
        }

        messageDlg.addNegativeBtn(
            context.getString(R.string.app_bt_close),
            R.drawable.ic_close
        ) {}

        messageDlg.show()
    }

    private fun downloadFromSite(context: Context) {
        val url = "https://ladsers.com/wp-content/uploads/Passtable-${Updater.lastVer}.apk"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}