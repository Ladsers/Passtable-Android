package com.ladsers.passtable.android

fun colorSelectionByTagCode(tagCode: String): Int {
    return when (tagCode) {
        "1" -> R.color.tagRed
        "2" -> R.color.tagGreen
        "3" -> R.color.tagBlue
        "4" -> R.color.tagYellow
        "5" -> R.color.tagPurple
        else -> R.color.tagNone
    }
}