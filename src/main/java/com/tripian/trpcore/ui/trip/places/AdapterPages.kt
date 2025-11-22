package com.tripian.trpcore.ui.trip.places

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.util.LanguageConst

/**
 * Created by semihozkoroglu on 21.10.2020.
 */
class AdapterPages(
    val context: Context,
    manager: FragmentManager,
    val places: Array<Place>,
    private val miscRepository: MiscRepository
) :
    FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        if (position == 4) {
            return FRMustTry.newInstance()
        }

        return FRPlaces.newInstance(places[position])
    }

    override fun getCount(): Int {
        return places.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> miscRepository.getLanguageValueForKey(LanguageConst.ATTRACTIONS)
            1 -> miscRepository.getLanguageValueForKey(LanguageConst.RESTAURANTS)
            2 -> miscRepository.getLanguageValueForKey(LanguageConst.CAFES)
            3 -> miscRepository.getLanguageValueForKey(LanguageConst.NIGHTLIFE)
            4 -> miscRepository.getLanguageValueForKey(LanguageConst.MUST_TRY)
            else -> miscRepository.getLanguageValueForKey(LanguageConst.ATTRACTIONS)
        }
    }
}