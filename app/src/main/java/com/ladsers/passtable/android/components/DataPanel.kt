package com.ladsers.passtable.android.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.activities.TableActivity
import com.ladsers.passtable.android.databinding.PanelDataBinding
import com.ladsers.passtable.android.dialogs.MessageDlg

class DataPanel(
    private val context: Context,
    private val activity: TableActivity
) {
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val binding = PanelDataBinding.inflate(activity.window.layoutInflater)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density

    private var initX: Int = 0
    private var initY: Int = 0
    private var pX: Int = 0
    private var pY: Int = 0

    private var login = ""
    private var password = ""

    private val panelWidthDp = 215
    private val panelHeightDp = 50

    private var isCreated = false

    private val settingsResult = activity.registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) createPanel()
        else Toast.makeText(
            context,
            context.getString(R.string.ui_msg_permissionNotGranted),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun show(login: String, password: String) {
        this.login = login
        this.password = password
        if (Settings.canDrawOverlays(context)) createPanel()
        else {
            MessageDlg(activity, activity.window).quickDialog(
                activity.getString(R.string.dlg_title_permissionRequired),
                activity.getString(R.string.dlg_msg_grantPermissionToDisplayOver),
                { requestPermission() },
                posText = activity.getString(R.string.app_bt_goToSettings),
                posIcon = R.drawable.ic_next_arrow
            )
        }
    }

    private fun requestPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        settingsResult.launch(intent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createPanel() {
        if (isCreated) closePanel()
        positionInit()

        layoutParams = WindowManager.LayoutParams().apply {
            width = (panelWidthDp * density).toInt()
            height = (panelHeightDp * density).toInt()
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

            @Suppress("DEPRECATION")
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_TOAST

            gravity = Gravity.TOP or Gravity.START
            x = initX
            y = initY
        }

        binding.root.setOnTouchListener(onTouchListener)
        binding.btLogin.setOnClickListener { toClipboard("l") }
        binding.btPassword.setOnClickListener { toClipboard("p") }
        binding.btClose.setOnClickListener {
            toClipboard("")
            closePanel()
        }

        windowManager.addView(binding.root, layoutParams)
        isCreated = true
    }

    private val onTouchListener = View.OnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pX = event.rawX.toInt()
                pY = event.rawY.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                val limitX = (screenWidth - panelWidthDp * density).toInt()
                val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                val limitY = (screenHeight - panelHeightDp * density).toInt()

                layoutParams.x += event.rawX.toInt() - pX
                layoutParams.y += event.rawY.toInt() - pY
                if (layoutParams.x > limitX) layoutParams.x = limitX
                if (layoutParams.y > limitY) layoutParams.y = limitY
                pX = event.rawX.toInt()
                pY = event.rawY.toInt()
                windowManager.apply { updateViewLayout(view, layoutParams) }
            }
            MotionEvent.ACTION_UP -> {
                val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                positionSave(screenWidth, screenHeight, layoutParams.x, layoutParams.y)

                view.performClick()
            }
        }
        return@OnTouchListener false
    }

    private fun toClipboard(key: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (key == "l" || key == "p") {
            val clip = ClipData.newPlainText(key, if (key == "l") login else password)
            clipboard.setPrimaryClip(clip)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                val clip = ClipData.newPlainText("", "")
                clipboard.setPrimaryClip(clip)
            }
        }

        val msg = when (key) {
            "l" -> context.getString(R.string.ui_msg_usernameCopied)
            "p" -> context.getString(R.string.ui_msg_passwordCopied)
            else -> context.getString(R.string.ui_msg_clipboardCleared)
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    private fun closePanel() {
        windowManager.removeView(binding.root)
        isCreated = false
    }

    private fun positionSetDefault() {
        val paddingDpX = 16
        val paddingDpY = 10

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels

        val toolbarDefaultHeight = (56 * density).toInt()
        val tv = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
        val toolbarCalcHeight =
            TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics)
        val toolbarHeight =
            if (toolbarCalcHeight > toolbarDefaultHeight) toolbarCalcHeight else toolbarDefaultHeight

        initX = (screenWidth - panelWidthDp * density - paddingDpX * density).toInt()
        initY = (toolbarHeight + paddingDpY * density).toInt()
    }

    private fun positionInit() {
        val currentHeight = Resources.getSystem().displayMetrics.heightPixels
        val currentWidth = Resources.getSystem().displayMetrics.widthPixels
        if (load("screenHeight") == currentHeight && load("screenWidth") == currentWidth) {
            initX = load("posX")
            initY = load("posY")
        } else {
            save("screenHeight", 0)
            save("screenWidth", 0)
            positionSetDefault()
        }
    }

    private fun positionSave(screenWidth: Int, screenHeight: Int, posX: Int, posY: Int) {
        save("screenWidth", screenWidth)
        save("screenHeight", screenHeight)
        save("posX", posX)
        save("posY", posY)
    }

    private fun load(key: String): Int {
        val shPref = context.getSharedPreferences("dataPanelPosition", Context.MODE_PRIVATE)
        return shPref.getInt(key, 0)
    }

    private fun save(key: String, value: Int) {
        val shPref = context.getSharedPreferences("dataPanelPosition", Context.MODE_PRIVATE)
        shPref.edit().putInt(key, value).apply()
    }
}