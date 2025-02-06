package com.ladsers.passtable.android.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.dialogs.MessageDlg
import com.ladsers.passtable.android.enums.AppStore
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.android.enums.ProjectSupportState
import java.util.Locale

object ProjectSupportProcessor {

    private const val BOOSTING_COUNTS = 9
    private const val COUNTS_FOR_RATING = 16
    private const val COUNTS_FOR_DONATION = 28

    fun getState(context: Context): ProjectSupportState {
        val paramState = ParamStorage.getInt(context, Param.PROJECT_SUPPORT_STATE)
        val state = ProjectSupportState.fromInt(paramState)
        return if (state == ProjectSupportState.IDLE_AFTER_RATING || state == ProjectSupportState.END)
            ProjectSupportState.IDLE else state
    }

    fun updateState(context: Context) {
        val paramState = ParamStorage.getInt(context, Param.PROJECT_SUPPORT_STATE)
        val state = ProjectSupportState.fromInt(paramState)
        if (state != ProjectSupportState.IDLE && state != ProjectSupportState.IDLE_AFTER_RATING) return

        var counter = ParamStorage.getInt(context, Param.PROJECT_SUPPORT_COUNTER)
        ParamStorage.set(context, Param.PROJECT_SUPPORT_COUNTER, ++counter)

        if (counter == COUNTS_FOR_RATING && state == ProjectSupportState.IDLE) {
            setNextState(context)
            // if the app store is not installed, then immediately proceed to the next state
            if (!AppStoreProcessor.isInstalled(context, AppStore.RUSTORE)) setNextState(context)
        }

        if (counter == COUNTS_FOR_DONATION && state == ProjectSupportState.IDLE_AFTER_RATING) {
            setNextState(context)
            // if the region is not RU, then immediately proceed to the next state
            if (Locale.getDefault().country.uppercase(Locale.ROOT) != "RU") setNextState(context)
        }
    }

    fun boostCounterIfZero(context: Context) {
        val counter = ParamStorage.getInt(context, Param.PROJECT_SUPPORT_COUNTER)
        if (counter == 0) ParamStorage.set(context, Param.PROJECT_SUPPORT_COUNTER, BOOSTING_COUNTS)
    }

    private fun setNextState(context: Context) {
        val paramState = ParamStorage.getInt(context, Param.PROJECT_SUPPORT_STATE)
        if (paramState == ProjectSupportState.END.toInt()) return
        ParamStorage.set(context, Param.PROJECT_SUPPORT_STATE, paramState + 1)
    }

    fun showDialog(
        messageDlg: MessageDlg,
        activity: Activity,
        state: ProjectSupportState
    ) {
        val ratingState = state == ProjectSupportState.RATING_APP
        val title = activity.getString(R.string.dlg_title_importantMessage)
        val message =
            activity.getString(if (ratingState) R.string.dlg_msg_rateApp else R.string.dlg_msg_supportDeveloper)
        val posButtonText =
            activity.getString(if (ratingState) R.string.app_bt_rateApp else R.string.app_bt_supportDeveloper)
        val posButtonIconRes = if (ratingState) R.drawable.ic_star else R.drawable.ic_diamond
        val action = {
            if (ratingState) AppStoreProcessor.open(activity, AppStore.RUSTORE)
            else {
                val webPage = Uri.parse("https://pay.cloudtips.ru/p/06778fc5")
                activity.startActivity(Intent(Intent.ACTION_VIEW, webPage))
            }
        }

        messageDlg.create(title, message)
        messageDlg.addPositiveBtn(posButtonText, posButtonIconRes) {
            action()
            setNextState(activity)
        }
        messageDlg.addNegativeBtn(
            activity.getString(R.string.app_bt_dontShowAgain),
            R.drawable.ic_cancel
        ) {
            setNextState(activity)
        }
        messageDlg.show()
    }
}