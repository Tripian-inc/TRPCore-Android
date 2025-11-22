package com.tripian.trpcore.domain

import com.tripian.gyg.base.Tripian
import com.tripian.one.api.misc.model.ConfigList
import com.tripian.one.api.misc.model.Providers
import com.tripian.trpcore.base.BaseUseCase
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class FetchConfigList @Inject constructor() : BaseUseCase<ConfigList, FetchConfigList.Params>() {

    class Params

    override fun on(params: Params?) {
        addObservable {
            miscRepository.getConfigList()
        }
    }

    override fun onSendSuccess(t: ConfigList) {
        setGYGApiKey(t.providers)
        super.onSendSuccess(t)
    }

    private fun setGYGApiKey(providers: Providers?) {
        providers?.let {
            val gygApiKey = providers.tourAndTicket?.firstOrNull { it?.name == "gyg" }?.apiKey
            gygApiKey?.let {
                Tripian.init(gygApiKey)
            }
        }
    }
}