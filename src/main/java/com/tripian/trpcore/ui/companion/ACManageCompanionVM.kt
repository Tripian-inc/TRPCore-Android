package com.tripian.trpcore.ui.companion

import android.os.Bundle
import com.tripian.one.api.companion.model.Companion
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.util.extensions.getSerializableCompat
import com.tripian.trpcore.util.extensions.navigateToFragment
import javax.inject.Inject

class ACManageCompanionVM @Inject constructor() : BaseViewModel() {

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        val selectedCompanion: ArrayList<Companion> =
            arguments?.getSerializableCompat<ArrayList<Companion>>("companions") ?: arrayListOf()

        val mode: CompanionMode =
            arguments?.getSerializableCompat<CompanionMode>("mode") ?: CompanionMode.TRIP

        navigateToFragment(
            fragment = FRCompanions.newInstance(
                selectedCompanion,
                mode,
                onCreateCompanion = {}), addToBackStack = false
        )
    }

    fun onClickedClose() {
        finishActivity()
    }

    fun onClickedAdd() {
        navigateToFragment(fragment = FRNewCompanion.newInstance())
    }
}