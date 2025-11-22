package com.tripian.trpcore.util

import com.tripian.trpcore.util.dialog.DGContent

/**
 * Created by semihozkoroglu on 2019-08-06.
 */
interface UseCaseListener {

    fun showDialog(dgContent: DGContent)

    fun showSnackBarMessage(message: String)

    fun refreshTokenError()
}