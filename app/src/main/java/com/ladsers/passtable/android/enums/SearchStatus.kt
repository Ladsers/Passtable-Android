package com.ladsers.passtable.android.enums

enum class SearchStatus {
    /**
     * Default state: no search is performed and there is no interaction with the search bar.
     *
     * Previous possible status: -.
     * Next possible status: TEXT_QUERY_EMPTY, TAG_QUERY.
     */
    NONE,

    /**
     * The search bar is open, but it does not contain a query.
     *
     * Previous possible status: NONE.
     * Next possible status: TEXT_QUERY.
     */
    TEXT_QUERY_EMPTY,

    /**
     * Search is performed by note or username.
     *
     * Previous possible status: TEXT_QUERY_EMPTY, NONE.
     * Next possible status: -.
     */
    TEXT_QUERY,

    /**
     * Search is performed by tag.
     *
     * Previous possible status: NONE.
     * Next possible status: TEXT_QUERY_EMPTY (by physical keyboard).
     */
    TAG_QUERY
}