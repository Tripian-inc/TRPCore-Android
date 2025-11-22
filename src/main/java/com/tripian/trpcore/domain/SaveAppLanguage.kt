package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.Service
import javax.inject.Inject

class SaveAppLanguage @Inject constructor(val service: Service) : BaseUseCase<Unit, SaveAppLanguage.Params>() {

    class Params(val language: String)

    override fun on(params: Params?) {
        service.setLanguage(params?.language ?: "en")
    }
}