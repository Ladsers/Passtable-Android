package com.ladsers.passtable.android.components.menus

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.ladsers.passtable.android.BuildConfig
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.activities.InfoActivity
import com.ladsers.passtable.android.activities.MainActivity
import com.ladsers.passtable.android.activities.SettingsActivity
import com.ladsers.passtable.android.components.AppStoreProcessor
import com.ladsers.passtable.android.components.ClipboardManager
import com.ladsers.passtable.android.components.PasswordGeneratorProcessor
import com.ladsers.passtable.android.enums.AppStore
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.dialogs.MessageDlg
import com.ladsers.passtable.android.dialogs.UpdateDlg
import com.ladsers.web.update.Channel
import com.ladsers.web.update.Platform
import com.ladsers.web.update.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainMenu(
    private val activity: MainActivity,
    private val messageDlg: MessageDlg,
    private val passwordGeneratorProcessor: PasswordGeneratorProcessor
) {
    private var updateDownloadUrl : String? = null

    fun onCreateOptionsMenu(menu: Menu): Boolean {
        activity.menuInflater.inflate(R.menu.menu_main, menu)
        val color = MaterialColors.getColor(activity.window.decorView, R.attr.notificationTint)
        menu.findItem(R.id.btUpdate).icon?.setTint(color)

        checkUpdate(menu)
        checkKeyboard(menu)

        menu.setItemVisibility(
            R.id.btRateApp,
            AppStoreProcessor.isInstalled(activity, AppStore.RUSTORE)
        )
        menu.setItemVisibility(
            R.id.btSupportDeveloper,
            Locale.getDefault().country.uppercase(Locale.ROOT) == "RU"
        )

        return true
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.btUpdate -> {
                UpdateDlg.show(messageDlg, updateDownloadUrl)
                true
            }
            R.id.btSettings -> {
                val intent = Intent(activity, SettingsActivity::class.java)
                activity.startActivity(intent)
                true
            }
            R.id.btKeyboardShortcuts -> {
                val intent = Intent(activity, InfoActivity::class.java)
                intent.putExtra("title", activity.getString(R.string.app_bt_keyboardShortcuts))
                intent.putExtra("info", activity.getString(R.string.app_info_keyboardShortcutsHelp))
                activity.startActivity(intent)
                true
            }
            R.id.btAppForPc -> {
                ClipboardManager.copy(
                    activity,
                    "https://passtable.com/download",
                    activity.getString(R.string.ui_msg_downloadLinkCopied)
                )
                true
            }
            R.id.btRateApp -> {
                AppStoreProcessor.open(activity, AppStore.RUSTORE)
                true
            }
            R.id.btSupportDeveloper -> {
                val webPage = Uri.parse("https://pay.cloudtips.ru/p/06778fc5")
                activity.startActivity(Intent(Intent.ACTION_VIEW, webPage))
                true
            }
            R.id.btSendFeedback -> {
                val webPage = Uri.parse("https://ladsers.com/passtable/report-android")
                activity.startActivity(Intent(Intent.ACTION_VIEW, webPage))
                true
            }
            R.id.btAbout -> {
                val intent = Intent(activity, SettingsActivity::class.java)
                intent.putExtra("settingsMode", false) // show About activity
                activity.startActivity(intent)
                true
            }

            /* Tools submenu */
            R.id.btPasswordGenerator -> {
                passwordGeneratorProcessor.start(true)
                true
            }

            else -> false
        }
    }

    private fun Menu.setItemVisibility(itemId: Int, isVisible: Boolean) {
        val button = this.findItem(itemId)
        button.isVisible = isVisible
        button.isEnabled = isVisible
    }

    private fun checkUpdate(menu: Menu) {
        // don't ask the update server when developing an app
        if (BuildConfig.DEBUG) return

        activity.lifecycleScope.launch(Dispatchers.IO) {
            var updateVerTag: String? = null

            Updater.getNewVersionTag(
                "passtable",
                Platform.ANDROID,
                BuildConfig.VERSION_NAME,
                Channel.STABLE
            )?.run {
                updateVerTag = this
                activity.window.decorView.post { menu.setItemVisibility(R.id.btUpdate, true) }
            }

            updateVerTag?.let { verTag ->
                updateDownloadUrl = Updater.getDownloadLink(
                    "passtable",
                    Platform.ANDROID,
                    verTag,
                    Channel.STABLE
                )
            }
        }
    }

    private fun checkKeyboard(menu: Menu) {
        if (ParamStorage.getBool(activity, Param.PHYSICAL_KEYBOARD_DETECTED)
            || activity.resources.configuration.keyboard == Configuration.KEYBOARD_QWERTY
        ) {
            ParamStorage.set(activity, Param.PHYSICAL_KEYBOARD_DETECTED, true)
            menu.setItemVisibility(R.id.btKeyboardShortcuts, true)
        }
    }
}