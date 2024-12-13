package com.ladsers.passtable.android.components

import android.app.Activity
import android.content.Intent
import android.view.View
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.activities.InfoActivity
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.containers.RecentFiles
import com.ladsers.passtable.android.dialogs.MessageDlg
import com.ladsers.passtable.android.enums.Param
import java.util.Calendar

object PasswordUserValidator {

    fun isNeedValidate(activity: Activity): Boolean {
        val featureEnabled = ParamStorage.getBool(activity, Param.PASSWORD_USER_VALIDATOR_ENABLED)
        if (!featureEnabled) return false

        val hourInMins = 60;
        val minInMillis = 60 * 1000
        val currentTimeInMins = (System.currentTimeMillis() / minInMillis).toInt()
        val lastVerificationTimeInMins =
            ParamStorage.getInt(activity, Param.PASSWORD_USER_VALIDATOR_TIMESTAMP)
        val hasHourPassed = currentTimeInMins >= (lastVerificationTimeInMins + hourInMins)
        if (!hasHourPassed) return false

        val lastVerificationDate = RecentFiles.getLastVerificationDate(activity) ?: return false
        val currentDate = Calendar.getInstance().time
        val dayInMillis = 24 * 60 * 60 * 1000
        val differenceInMillis = currentDate.time - lastVerificationDate.time
        val differenceInDays = differenceInMillis / dayInMillis
        val hasWeekPassed = differenceInDays >= 7
        if (!hasWeekPassed) return false

        return true
    }

    fun showValidationDialog(
        messageDlg: MessageDlg,
        activity: Activity,
        enterPasswordAction: () -> Unit,
        skipAction: () -> Unit
    ) {
        messageDlg.create(
            activity.getString(R.string.dlg_ct_reminder),
            activity.getString(R.string.dlg_msg_periodicallyEnterPrimaryPassword)
        )
        messageDlg.addPositiveBtn(
            activity.getString(R.string.app_bt_enterPassword),
            R.drawable.ic_enter
        ) {
            enterPasswordAction()
        }
        messageDlg.addNeutralBtn(
            activity.getString(R.string.app_bt_skip),
            R.drawable.ic_navigate_next
        ) {
            blockReminderForApp(activity)
            blockReminderForFile(activity)
            showDisablingInfo(activity)
            skipAction()
        }
        messageDlg.addSkipAction {
            blockReminderForApp(activity)
            blockReminderForFile(activity)
            showDisablingInfo(activity)
            skipAction()
        }
        messageDlg.addNegativeBtn(
            activity.getString(R.string.app_bt_forgotPassword),
            R.drawable.ic_password_reset
        ) {
            blockReminderForApp(activity)
            showRecoveryInfoActivity(activity)
        }
        messageDlg.show()
    }

    fun handleSuccessValidation(activity: Activity) = updateLastVerificationDate(
        activity = activity,
        addDays = 7
    )

    /**
     * Block the appearance of windows for all files for an hour.
     */
    private fun blockReminderForApp(activity: Activity) {
        val minInMillis = 60 * 1000
        val currentTimeInMins = (System.currentTimeMillis() / minInMillis).toInt()
        ParamStorage.set(
            activity,
            Param.PASSWORD_USER_VALIDATOR_TIMESTAMP,
            currentTimeInMins
        )

    }

    /**
     * Block the appearance of windows for this file for a day.
     */
    private fun blockReminderForFile(activity: Activity) = updateLastVerificationDate(
        activity = activity,
        addDays = 1
    )

    private fun updateLastVerificationDate(activity: Activity, addDays: Int) {
        RecentFiles.getLastVerificationDate(activity)?.let { current ->
            val calendar = Calendar.getInstance()
            calendar.time = current
            calendar.add(Calendar.DAY_OF_MONTH, addDays)
            RecentFiles.rememberLastVerificationDate(activity, calendar.time)
        }
    }

    /**
     * Show information about disabling the feature.
     */
    private fun showDisablingInfo(activity: Activity) {
        val param = Param.INITIAL_INFO_PASSWORD_USER_VALIDATOR
        if (!ParamStorage.getBool(activity, param)) return
        val rootView = activity.findViewById<View>(android.R.id.content)
        SnackbarManager.showInitInfo(
            context = activity,
            view = rootView,
            param = param,
            infoText = activity.getString(R.string.app_info_disablePasswordReminders),
            duration = 15000
        )
    }

    private fun showRecoveryInfoActivity(activity: Activity) {
        val intent = Intent(activity, InfoActivity::class.java)
        intent.putExtra("title", activity.getString(R.string.app_info_changePasswordTitle))
        intent.putExtra("info", activity.getString(R.string.app_info_changePasswordSteps))
        activity.startActivity(intent)
        activity.finish()
    }
}