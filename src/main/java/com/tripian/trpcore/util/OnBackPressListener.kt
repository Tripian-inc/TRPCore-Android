package com.tripian.trpcore.util

/**
 * Created by semihozkoroglu on 2019-08-04.
 */
interface OnBackPressListener {

    /**
     * Eger isBackEnable durumunda ise viewModel'in onBackPress methodu cagirilabilir demektir.
     * Default olarak fragment tarafinda true olarak set edilecektir.
     * Activity'nin setOnBackPressListener methoduna register olmak istemeyen ozellikle child
     * fragment'lar icin bu deger false set edilmelidir.
     */
    fun isBackEnable(): Boolean

    fun onBackPressed(): Boolean
}