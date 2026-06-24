package com.tripian.trpcore.ui.createtrip

import android.content.Context
import android.content.Intent
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ActivityDateSelectionBinding
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.consumeSystemBarPadding
import com.tripian.trpcore.util.extensions.observe

/**
 * Screen B of the create-trip flow: pick a date range for the chosen city, then
 * create the timeline. Back / "Edit" return to [ACCitySelection] (still on the
 * back stack, with its selection intact).
 */
class ACDateSelection : BaseActivity<ActivityDateSelectionBinding, ACDateSelectionVM>() {

    override fun getViewBinding() = ActivityDateSelectionBinding.inflate(layoutInflater)

    private fun lang(key: String, fallback: String): String =
        getLanguageForKey(key).let { if (it.isBlank() || it == key) fallback else it }

    private fun setupTexts() {
        binding.tvTitle.text = lang(LanguageConst.SELECT_DATES, "Select a date")
        binding.tvDestinationsLabel.text = lang(LanguageConst.EXPERIENCE_DESTINATIONS, "Destinations")
        binding.tvEdit.text = lang(LanguageConst.CREATE_TRIP_EDIT, "Edit")
        binding.btnNext.text = lang(LanguageConst.CREATE_TRIP_NEXT, "Next")
    }

    override fun setListeners() {
        // Pad for status bar (top) + navigation bar (bottom) so the Next button
        // is never hidden behind the device's bottom system bar.
        binding.root.consumeSystemBarPadding(top = true, bottom = true)
        setupTexts()
        @Suppress("DEPRECATION")
        val city = intent.getSerializableExtra(ACDateSelectionVM.ARG_CITY) as? City
        binding.tvCityName.text = city?.name ?: ""

        binding.ivBack.setOnClickListener { finish() }
        binding.tvEdit.setOnClickListener { finish() }

        binding.calendarView.onRangeChanged = { start, _ ->
            setNextEnabled(start != null)
        }

        binding.btnNext.setOnClickListener {
            val start = binding.calendarView.getStart() ?: return@setOnClickListener
            val end = binding.calendarView.getEnd() ?: start
            viewModel.buildAndContinue(start, end, uniqueId())
        }
    }

    override fun setReceivers() {
        observe(viewModel.onBuildItinerary) { itinerary ->
            itinerary?.let {
                // Signal ACCitySelection (our parent in the create chain) that a
                // timeline was created so it finishes too — back from ACTimeline
                // then lands on My Trips (or the host), not the create screens.
                setResult(RESULT_OK)
                startTimeline(it)
            }
            finish()
        }
    }

    private fun startTimeline(itinerary: ItineraryWithActivities) {
        TRPCore.core.startWithItinerary(
            context = this,
            itinerary = itinerary,
            canBack = intent.getBooleanExtra("canBack", true),
            appLanguage = intent.getStringExtra("language") ?: "en",
            appCurrency = intent.getStringExtra("currency") ?: "EUR"
        )
    }

    private fun setNextEnabled(enabled: Boolean) {
        binding.btnNext.isEnabled = enabled
        binding.btnNext.alpha = if (enabled) 1f else 0.4f
    }

    private fun uniqueId(): String? = intent.getStringExtra("uniqueId")

    companion object {
        fun newIntent(
            context: Context,
            city: City,
            language: String,
            currency: String,
            uniqueId: String?,
            canBack: Boolean
        ): Intent = Intent(context, ACDateSelection::class.java).apply {
            putExtra(ACDateSelectionVM.ARG_CITY, city)
            putExtra("language", language)
            putExtra("currency", currency)
            putExtra("uniqueId", uniqueId)
            putExtra("canBack", canBack)
        }
    }
}
