package com.tripian.trpcore.ui.createtrip

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ActivityMyTripsBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.consumeSystemBarPadding
import com.tripian.trpcore.util.extensions.observe

/**
 * "My Trips" — lists the user's existing not-past timelines when the SDK is
 * opened with no reservations and the host allows creating trips from scratch.
 * Reached from [com.tripian.trpcore.ui.splash.ACSplash]; it is the root of the
 * SDK task in this path, so back (header + device) CLOSES the SDK.
 *
 * Tapping a card opens that timeline ([ACTimeline]); the FAB launches the
 * create-trip flow ([ACCitySelection]) in the same task so back returns here.
 */
class ACMyTrips : BaseActivity<ActivityMyTripsBinding, ACMyTripsVM>() {

    private var adapter: MyTripsAdapter? = null

    override fun getViewBinding() = ActivityMyTripsBinding.inflate(layoutInflater)

    private fun lang(key: String, fallback: String): String =
        getLanguageForKey(key).let { if (it.isBlank() || it == key) fallback else it }

    override fun setListeners() {
        binding.root.consumeSystemBarPadding(top = true, bottom = true)
        binding.tvTitle.text = lang(LanguageConst.MY_PLANS, "My Trips")

        adapter = MyTripsAdapter { trip -> TRPCore.core.startTimeline(this, trip.tripHash) }
        binding.rvTrips.layoutManager = LinearLayoutManager(this)
        binding.rvTrips.adapter = adapter

        binding.ivBack.setOnClickListener { TRPCore.closeSDK() }
        binding.fabAdd.setOnClickListener { openCreateFlow() }
    }

    override fun setReceivers() {
        observe(viewModel.trips) { list ->
            val trips = list ?: emptyList()
            adapter?.submitList(trips)
            binding.tvEmpty.visibility = if (trips.isEmpty()) View.VISIBLE else View.GONE
            binding.rvTrips.visibility = if (trips.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh so a trip just created via the FAB shows up on return.
        viewModel.refresh()
    }

    override fun backPressed() {
        TRPCore.closeSDK()
    }

    private fun openCreateFlow() {
        // Launch in the same task (no NEW_TASK) so ACCitySelection stacks on top
        // of My Trips and back returns here.
        startActivity(
            ACCitySelection.newIntent(
                context = this,
                language = intent.getStringExtra("language") ?: "en",
                currency = intent.getStringExtra("currency") ?: "EUR",
                uniqueId = intent.getStringExtra("uniqueId"),
                canBack = intent.getBooleanExtra("canBack", true)
            )
        )
    }

    companion object {
        fun newIntent(
            context: Context,
            language: String,
            currency: String,
            uniqueId: String?,
            canBack: Boolean
        ): Intent = Intent(context, ACMyTrips::class.java).apply {
            putExtra("language", language)
            putExtra("currency", currency)
            putExtra("uniqueId", uniqueId)
            putExtra("canBack", canBack)
        }
    }
}
