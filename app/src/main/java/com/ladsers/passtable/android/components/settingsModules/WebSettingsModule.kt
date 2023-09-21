package com.ladsers.passtable.android.components.settingsModules

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import com.ladsers.passtable.android.dialogs.MessageDlg

class WebSettingsModule(
    override val activity: Activity,
    override val binding: ActivitySettingsBinding,
    override val messageDlg: MessageDlg
) : ISettingsModule {

    override val rootView: View
        get() = binding.web.root


    override fun configure() {
        // not used
    }

    override fun attachActionsOnCreate() {
        binding.web.btWebPage.setOnClickListener {
            val webPage = Uri.parse(activity.getString(R.string.app_info_webPage))
            activity.startActivity(Intent(Intent.ACTION_VIEW, webPage))
        }
        binding.web.btRepo.setOnClickListener {
            val repo = Uri.parse(activity.getString(R.string.app_info_repo))
            activity.startActivity(Intent(Intent.ACTION_VIEW, repo))
        }
    }

    override fun attachActionsOnResume() {
        // not used
    }
}