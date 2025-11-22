package com.tripian.trpcore.util.dialog

import com.tripian.trpcore.domain.model.BaseModel

class DGContent(
    var title: String? = "",
    var content: String? = "",
    var positiveBtn: String? = "",
    var negativeBtn: String? = "",
    var positiveListener: DGActionListener? = null,
    var negativeListener: DGActionListener? = null
) : BaseModel()