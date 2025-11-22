package com.tripian.trpcore.ui.mytrip

import com.google.android.material.tabs.TabLayout
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcMyTripBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * Created by semihozkoroglu on 15.08.2020.
 */
class ACMyTrip : BaseActivity<AcMyTripBinding, ACMyTripVM>() {

    override fun getViewBinding(): AcMyTripBinding {
        return AcMyTripBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.imAdd.setOnClickListener { viewModel.onClickedCreate() }
        binding.imProfile.setOnClickListener { viewModel.onClickedProfile() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab) {
            }

            override fun onTabUnselected(p0: TabLayout.Tab) {
            }

            override fun onTabSelected(p0: TabLayout.Tab) {
                viewModel.onTabSelected(p0.position)
            }
        })
        binding.tabLayout.getTabAt(0)?.text = viewModel.getLanguageForKey(LanguageConst.UPCOMING_TRIPS)
        binding.tabLayout.getTabAt(1)?.text = viewModel.getLanguageForKey(LanguageConst.PAST_TRIPS)
    }

    override fun setReceivers() {
    }
}