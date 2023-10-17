package com.ladsers.passtable.android.containers

enum class Param(
    val str: String,
    val defBool: Boolean = false,
    val defInt: Int = 0,
    val defStr: String = ""
) {
    /* For Snackbar manager */
    INITIAL_INFO_ITEM_MENU("initialInfoItemMenu", defBool = true),
    INITIAL_INFO_PIN_TO_SCREEN("initialInfoPinToScreen", defBool = true),
    INITIAL_INFO_KEYBOARD_SHORTCUTS("initialInfoKeyboardShortcuts", defBool = true),
    INITIAL_INFO_LICENSE("initialInfoLicense", defBool = true),

    PHYSICAL_KEYBOARD_DETECTED("physicalKeyboardDetected", defBool = false),

    /* Parameters */
    CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT("checkboxRememberPasswordByDefault", defBool = true),
    LOCK_MODE("lockMode", defInt = 0),
    LOCK_SECS("lockSecs", defInt = 120),
    LOCK_ALLOW_WHEN_EDITING("lockAllowWhenEditing", defBool = true),
    REMEMBER_RECENT_FILES("rememberRecentFiles", defBool = true),
    THEME("theme", defInt = 0),
    PREVENT_SCREEN_CAPTURE("preventScreenCapture", defBool = true),

    GENERATOR_PASSWORD_LENGTH("generatorPasswordLength", defInt = 8),
    GENERATOR_LOWERCASE_LETTERS_ALLOW("generatorLowercaseLettersAllow", defBool = true),
    GENERATOR_LOWERCASE_LETTERS_MINIMUM("generatorLowercaseLettersMinimum", defInt = 2),
    GENERATOR_CAPITAL_LETTERS_ALLOW("generatorCapitalLettersAllow", defBool = true),
    GENERATOR_CAPITAL_LETTERS_MINIMUM("generatorCapitalLettersMinimum", defInt = 2),
    GENERATOR_NUMBERS_ALLOW("generatorNumbersAllow", defBool = true),
    GENERATOR_NUMBERS_MINIMUM("generatorNumbersMinimum", defInt = 1),
    GENERATOR_SYMBOLS_ALLOW("generatorSymbolsAllow", defBool = true),
    GENERATOR_SYMBOLS_MINIMUM("generatorSymbolsMinimum", defInt = 1),
    GENERATOR_SYMBOLS_SET("generatorSymbolsSet", defInt = 0),
    GENERATOR_EXCLUDE("generatorExclude", defStr = "")
}