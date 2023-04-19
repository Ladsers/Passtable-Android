package com.ladsers.passtable.android.dialogs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ladsers.passtable.android.BuildConfig
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.PackageFinder
import com.ladsers.passtable.android.components.PackageFinder.isPackageInstalled
import com.ladsers.passtable.lib.updater.Updater

object UpdateDlg {
    fun show(messageDlg: MessageDlg) {

        val context = messageDlg.context

        val isRuStoreInstalled =
            context.packageManager.isPackageInstalled(PackageFinder.packageNameRuStore)

        messageDlg.create(
            context.getString(R.string.dlg_title_updateAvailable),
            context.getString(R.string.dlg_msg_downloadNewVersion) //TODO
        )

        if (isRuStoreInstalled) {
            messageDlg.addPositiveBtn(
                "Open RuStore", //TODO
                R.drawable.ic_download
            ) { openRuStore(context) }

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

    private fun openRuStore(context: Context) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("rustore://apps.rustore.ru/app/${BuildConfig.APPLICATION_ID}")
                )
            )
        } catch (ex: ActivityNotFoundException) {
            /* do nothing */
        }
    }
}