package com.ladsers.passtable.android.components.settingsModules

import android.app.Activity
import android.content.Intent
import android.view.View
import com.ladsers.passtable.android.BuildConfig
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.activities.InfoActivity
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import com.ladsers.passtable.android.dialogs.MessageDlg
import com.ladsers.passtable.lib.licenseText

class AboutSettingsModule(
    override val activity: Activity,
    override val binding: ActivitySettingsBinding,
    override val messageDlg: MessageDlg
) : ISettingsModule {

    override val rootView: View
        get() = binding.about.root


    override fun configure() {
        // not used
    }

    override fun attachActionsOnCreate() {
        binding.about.btLicense.setOnClickListener {
            val intent = Intent(activity, InfoActivity::class.java)
            intent.putExtra("title", activity.getString(R.string.app_bt_license))
            intent.putExtra("info", licenseText)
            activity.startActivity(intent)
        }
        binding.about.btThirdParty.setOnClickListener {
            val intent = Intent(activity, InfoActivity::class.java)
            intent.putExtra("title", activity.getString(R.string.app_bt_thirdPartyResources))
            intent.putExtra("info", activity.getString(R.string.app_info_thirdPartyResources))
            activity.startActivity(intent)
        }
        binding.about.tvVersion.text = BuildConfig.VERSION_NAME
    }

    override fun attachActionsOnResume() {
        // not used
    }
}