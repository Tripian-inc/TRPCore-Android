package com.tripian.trpcore.ui.timeline.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.google.android.material.tabs.TabLayout
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.databinding.ViewTimelineCityTabBinding

/**
 * TimelineCityTabView
 * Tab view for city selection when there are multiple cities
 */
class TimelineCityTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewTimelineCityTabBinding
    private var cities: List<City> = emptyList()
    private var onCitySelectedListener: ((City) -> Unit)? = null

    init {
        binding = ViewTimelineCityTabBinding.inflate(LayoutInflater.from(context), this, true)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    if (position < cities.size) {
                        onCitySelectedListener?.invoke(cities[position])
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    fun setCities(newCities: List<City>) {
        cities = newCities
        binding.tabLayout.removeAllTabs()

        cities.forEach { city ->
            val tab = binding.tabLayout.newTab()
            tab.text = city.name
            binding.tabLayout.addTab(tab)
        }
    }

    fun setSelectedCity(city: City) {
        val index = cities.indexOf(city)
        if (index >= 0 && index < binding.tabLayout.tabCount) {
            binding.tabLayout.getTabAt(index)?.select()
        }
    }

    fun setOnCitySelectedListener(listener: (City) -> Unit) {
        onCitySelectedListener = listener
    }
}
