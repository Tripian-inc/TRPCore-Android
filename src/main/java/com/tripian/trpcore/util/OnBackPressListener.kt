package com.tripian.trpcore.util

interface OnBackPressListener {

    /**
     * If isBackEnable returns true, the viewModel's onBackPress method can be called.
     * By default, this is set to true by the fragment.
     * This value should be set to false for child fragments that don't want to
     * register with the Activity's setOnBackPressListener method.
     */
    fun isBackEnable(): Boolean

    fun onBackPressed(): Boolean
}