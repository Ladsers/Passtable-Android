package com.ladsers.passtable.android.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.settingsModules.*
import com.ladsers.passtable.android.databinding.ActivitySettingsBinding
import com.ladsers.passtable.android.dialogs.MessageDlg

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var messageDlg: MessageDlg

    private val settingsModules: MutableList<ISettingsModule> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        messageDlg = MessageDlg(this, window)

        val settingsMode = intent.getBooleanExtra("settingsMode", true)

        binding.toolbar.root.title =
            getString(if (settingsMode) R.string.ui_ct_settings else R.string.ui_ct_about)

        binding.toolbar.root.navigationIcon =
            ContextCompat.getDrawable(this, R.drawable.ic_back_arrow)
        setSupportActionBar(binding.toolbar.root)
        binding.toolbar.root.setNavigationOnClickListener { finish() }

        binding.svLayout.setOnScrollChangeListener { _, _, y, _, oldY ->
            if (y > oldY || y < oldY) binding.toolbar.root.elevation = 7f
            if (y == 0) binding.toolbar.root.elevation = 0f
        }

        if (settingsMode) registerSettingsModules() else registerAboutModules()

        for (module in settingsModules) {
            module.setVisible()
            module.configure()
            module.attachActionsOnCreate()
        }
    }

    override fun onResume() {
        super.onResume()

        for (module in settingsModules) module.attachActionsOnResume()
    }


    private fun registerSettingsModules() {
        settingsModules.add(ThemeSettingsModule(this, binding, messageDlg))
        settingsModules.add(LockFileSettingsModule(this, binding, messageDlg))
        settingsModules.add(SecuritySettingsModule(this, binding, messageDlg))
        settingsModules.add(RecentFilesSettingsModule(this, binding, messageDlg))
        settingsModules.add(BiometricAuthSettingsModule(this, binding, messageDlg))
    }

    private fun registerAboutModules() {
        settingsModules.add(WebSettingsModule(this, binding, messageDlg))
        settingsModules.add(AboutSettingsModule(this, binding, messageDlg))
    }
}
