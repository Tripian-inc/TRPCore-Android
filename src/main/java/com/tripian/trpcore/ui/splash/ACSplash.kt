package com.tripian.trpcore.ui.splash

import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.AcSplashBinding
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.util.extensions.observe

/**
 * SDK entry Activity for host apps (e.g. the Nexus Capacitor bridge starts this
 * via an Intent with booking/reservation extras). It performs a light login
 * (no email/password) through [ACSplashVM], builds an itinerary from the
 * reservations, then hands off to the SDK timeline via startWithItinerary.
 */
class ACSplash : BaseActivity<AcSplashBinding, ACSplashVM>() {

    override fun getViewBinding(): AcSplashBinding =
        AcSplashBinding.inflate(layoutInflater)

    override fun setListeners() {}

    override fun setReceivers() {
        observe(viewModel.onItineraryReady) { itinerary ->
            itinerary?.let { startTimeline(it) }
            finish()
        }
    }

    private fun startTimeline(itinerary: ItineraryWithActivities) {
        // tripHash (if any) travels on itinerary.tripianHash, so the SDK decides
        // fetch vs create; language/currency come from the host app.
        val language = intent.getStringExtra("language") ?: "en"
        val currency = intent.getStringExtra("currency") ?: "EUR"
        TRPCore.core.startWithItinerary(
            context = this,
            itinerary = itinerary,
            appLanguage = language,
            appCurrency = currency
        )
    }
}
