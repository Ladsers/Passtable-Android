package com.ladsers.passtable.android.containers

enum class Param(
    val str: String,
    val defBool: Boolean = false,
    val defInt: Int = 0,
    val defStr: String = ""
) {
    INITIAL_INFO_ITEM_MENU("initialInfoItemMenu", defBool = true),
    INITIAL_INFO_PIN_TO_SCREEN("initialInfoPinToScreen", defBool = true),
    INITIAL_INFO_KEYBOARD_SHORTCUTS("initialInfoKeyboardShortcuts", defBool = true),
    INITIAL_INFO_LICENSE("initialInfoLicense", defBool = true),

    PHYSICAL_KEYBOARD_DETECTED("physicalKeyboardDetected", defBool = false),

    CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT("checkboxRememberPasswordByDefault", defBool = true),
    LOCK_MODE("lockMode", defInt = 0),
    LOCK_SECS("lockSecs", defInt = 120),
    LOCK_ALLOW_WHEN_EDITING("lockAllowWhenEditing", defBool = true),
    REMEMBER_RECENT_FILES("rememberRecentFiles", defBool = true),
    THEME("theme", defInt = 0),
    PREVENT_SCREEN_CAPTURE("preventScreenCapture", defBool = true)
}