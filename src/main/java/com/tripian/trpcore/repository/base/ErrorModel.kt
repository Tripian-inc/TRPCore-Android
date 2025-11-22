package com.tripian.trpcore.repository.base

import com.tripian.trpcore.util.AlertType

class ErrorModel constructor(
    var errorDesc: String = "",
    var status: Int = 0,
    var type: AlertType = AlertType.DIALOG
) : Throwable()