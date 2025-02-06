package com.ladsers.passtable.android.enums

enum class ProjectSupportState {
    IDLE,
    RATING_APP,
    IDLE_AFTER_RATING,
    DONATION,
    END;

    companion object {
        fun fromInt(value: Int): ProjectSupportState {
            return entries[value]
        }
    }

    fun toInt(): Int {
        return this.ordinal
    }
}