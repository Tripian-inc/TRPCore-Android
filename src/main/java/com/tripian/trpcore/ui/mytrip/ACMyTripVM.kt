package com.tripian.trpcore.ui.mytrip

import android.os.Bundle
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.ui.createtrip.ACCreateTrip
import com.tripian.trpcore.ui.profile.ACProfile
import com.tripian.trpcore.util.extensions.navigateToFragment
import javax.inject.Inject

class ACMyTripVM @Inject constructor() : BaseViewModel() {

    private var frUpComings: BaseFragment<*,*>? = null
    private var frPast: BaseFragment<*,*>? = null

    private var selectedTab = 0

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

//        navigateToFragment(fragment = FRProfile.newInstance(), viewId = R.id.flProfile, addToBackStack = false)

        onTabSelected(selectedTab)
    }

    fun onTabSelected(position: Int) {
        selectedTab = position

        if (position == 0) {
            frUpComings = FRUpComingsTrip.newInstance()

            navigateToFragment(fragment = frUpComings!!, viewId = R.id.flContainer, addToBackStack = false)
        } else {
            frPast = FRPastTrip.newInstance()

            navigateToFragment(fragment = frPast!!, viewId = R.id.flContainer, addToBackStack = false)
        }
    }

    fun onClickedCreate() {
        startActivity(ACCreateTrip::class)
    }

    fun onClickedProfile() {
        startActivity(ACProfile::class)
    }
}
