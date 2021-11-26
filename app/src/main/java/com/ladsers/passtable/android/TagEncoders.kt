package com.ladsers.passtable.android

fun colorSelectionByTagCode(tagCode: String): Int {
    return when (tagCode) {
        "1" -> R.attr.tagRed
        "2" -> R.attr.tagGreen
        "3" -> R.attr.tagBlue
        "4" -> R.attr.tagYellow
        "5" -> R.attr.tagPurple
        else -> R.attr.tagNone
    }
}