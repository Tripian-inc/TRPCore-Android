package com.tripian.trpcore.ui.trip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.mapbox.common.MapboxOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.AcTripModeBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.TransListener
import com.tripian.trpcore.util.extensions.observe
import com.tripian.trpcore.util.extensions.openUrlExt
import kotlinx.coroutines.launch

/**
 * Created by semihozkoroglu on 15.09.2020.
 */
class ACTripMode : BaseActivity<AcTripModeBinding, ACTripModeVM>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        MapboxOptions.accessToken = TRPCore.mapBoxApiKey

        super.onCreate(savedInstanceState)

        viewModel.setLifecycleOwner(this)
    }

    override fun setListeners() {
        binding.tvFavorites.text = getLanguageForKey(LanguageConst.FAVORITES)
        binding.tvItinerary.text = getLanguageForKey(LanguageConst.PLANNER)
        binding.tvExperiences.text = getLanguageForKey(LanguageConst.EXPERIENCES)
        binding.tvPlaces.text = getLanguageForKey(LanguageConst.PLACES)
        binding.tvSearchThisArea.text = getLanguageForKey(LanguageConst.SEARCH_THIS_AREA)
        binding.tvClearResults.text = getLanguageForKey(LanguageConst.CLEAR_SEARCH)

        binding.imBack.setOnClickListener { viewModel.onClickedBack() }
        binding.mapView.setOnMapLoadListener { viewModel.onMapLoaded() }
        binding.mapView.setOnMapClickListener { viewModel.onMapItemClicked(it) }
        binding.mapView.setOnZoomLevelListener { viewModel.onZoomLevelChanged(it) }
        binding.btnAlternative.setOnClickListener { viewModel.onClickedAlternative() }
        binding.llTitle.setOnClickListener { viewModel.onClickedDay() }
        binding.btnLocation.setOnClickListener { viewModel.onClickedLocation() }
        binding.btnReturn.setOnClickListener { viewModel.onClickedBackMap() }
        binding.llSearchThisArea.setOnClickListener {
            viewModel.onClickedSearchThisArea(
                binding.mapView.getBounds(),
                binding.mapView.getDistance()
            )
        }
        binding.llClearSearch.setOnClickListener { viewModel.onClickedClearSearch() }

        binding.llExperiences.setOnClickListener { viewModel.onClickedExperiences() }
        binding.llFavorite.setOnClickListener { viewModel.onClickedFavorite() }
        binding.llSearch.setOnClickListener { viewModel.onClickedPlaces() }
        binding.llItinerary.setOnClickListener { viewModel.onClickedItinerary() }
        binding.imBooking.setOnClickListener { viewModel.onClickedBooking() }
        binding.imMyOffers.setOnClickListener { viewModel.onClickedMyOffers() }

        binding.btnOffers.setOnClickListener { viewModel.onClickedOffers(binding.mapView.getBounds()) }
        binding.btnExportPlan.setOnClickListener { viewModel.onClickedExportPlan() }
    }

    override fun setReceivers() {
        observe(viewModel.onShowAlternativesListener) {
            binding.mapView.showMapIcons(it!!)

//            btnAlternative.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            binding.btnAlternative.setColorFilter(ContextCompat.getColor(this, R.color.blue_dark))
        }

        observe(viewModel.onHideAlternativesListener) {
//            btnAlternative.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pink))
            binding.btnAlternative.setColorFilter(ContextCompat.getColor(this, R.color.black))
        }

        observe(viewModel.onShowSearchListener) {
            binding.mapView.showMapIcons(it!!)

            if (it.isEmpty()) {
                binding.llClearSearch.visibility = View.GONE
            } else {
                binding.llClearSearch.visibility = View.VISIBLE
            }
        }

        observe(viewModel.onHideSearchListener) {
            binding.llSearchThisArea.visibility = View.GONE
        }

        observe(viewModel.onAnimateMapCameraListener) {

            lifecycleScope.launch {
                binding.mapView.moveCameraTo()
            }
        }

        observe(viewModel.onHideMapStepsListener) {
            binding.mapView.clearMap(it)
        }

        observe(viewModel.onClearSearchListener) {
            binding.llClearSearch.visibility = View.GONE
        }

        observe(viewModel.onSetMapPoiListener) {
            binding.mapView.clearMap()

            binding.mapView.showMapIcons(it!!)

            binding.llSearchThisArea.visibility = View.GONE
            binding.llClearSearch.visibility = View.GONE
        }

        observe(viewModel.onShowRouteListener) {
            binding.mapView.showRoute(it!!)
        }

        observe(viewModel.onRedirectRouteListener) {
            binding.mapView.redirectRoute(it!!)
        }

        observe(viewModel.onEnableLocationListener) {
            binding.btnLocation.setColorFilter(ContextCompat.getColor(this, R.color.black))
        }

        observe(viewModel.onGoLocationListener) {
            binding.btnReturn.visibility = View.VISIBLE
            binding.btnLocation.setColorFilter(ContextCompat.getColor(this, R.color.blue_dark))
            binding.mapView.enableLocation()
            binding.mapView.moveCameraTo(it)
        }

        observe(viewModel.onFocusCityListener) {
            binding.mapView.moveCameraTo(it, zoom = 11.0)
        }

        observe(viewModel.onGoCityListener) {
            binding.btnReturn.visibility = View.GONE
            binding.btnLocation.setColorFilter(ContextCompat.getColor(this, R.color.black))
            binding.mapView.disableLocation()
            lifecycleScope.launch {
                binding.mapView.moveCameraTo()
            }
        }

        observe(viewModel.onDisableLocationListener) {
            binding.btnReturn.visibility = View.GONE
            binding.btnLocation.setColorFilter(ContextCompat.getColor(this, R.color.light_grey))
        }

        observe(viewModel.onSetCityNameListener) {
            binding.tvPlanCity.text = it
        }

        observe(viewModel.onSetDateListener) {
            binding.tvPlanDay.text = it
            binding.imDayArrow.visibility = View.VISIBLE
        }

        observe(viewModel.onAnimateAlternativeTextStateListener) {
            binding.cvAlternatives.postDelayed({
                val transition = AutoTransition()
                transition.interpolator = OvershootInterpolator(1.6f)
                transition.duration = 500
                transition.addListener(object : TransListener() {
                    override fun onTransitionEnd(transition: Transition) {
                        binding.cvAlternatives.postDelayed({
                            binding.cvAlternatives.postDelayed({
                                binding.cvAlternatives.visibility = View.INVISIBLE
                            }, 120)

                            TransitionManager.beginDelayedTransition(binding.root, transition)
                            val constraintSet = ConstraintSet()
                            constraintSet.clone(binding.root)
                            constraintSet.connect(
                                R.id.btnAlternative,
                                ConstraintSet.LEFT,
                                binding.cvAlternatives.id,
                                ConstraintSet.LEFT,
                                0
                            )
                            constraintSet.clear(R.id.btnAlternative, ConstraintSet.RIGHT)
                            constraintSet.applyTo(binding.root)
                        }, 1000)
                    }
                })

                binding.cvAlternatives.visibility = View.VISIBLE

                TransitionManager.beginDelayedTransition(binding.root, transition)
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.root)
                constraintSet.connect(
                    R.id.btnAlternative,
                    ConstraintSet.RIGHT,
                    binding.cvAlternatives.id,
                    ConstraintSet.RIGHT,
                    0
                )
                constraintSet.clear(R.id.btnAlternative, ConstraintSet.LEFT)
                constraintSet.applyTo(binding.root)
            }, 1000)
        }

        observe(viewModel.onSearchThisAreaListener) {
            if (it!!) {
                binding.llSearchThisArea.visibility = View.VISIBLE
            } else {
                binding.llSearchThisArea.visibility = View.GONE
            }
        }

        observe(viewModel.onShowOffersListener) {
            binding.mapView.showMapIcons(it!!)

            binding.btnOffers.setColorFilter(ContextCompat.getColor(this, R.color.green))
        }

        observe(viewModel.onHideOffersListener) {
            binding.btnOffers.setColorFilter(ContextCompat.getColor(this, R.color.black))
        }

        observe(viewModel.onExportPlanListener) {
            it?.let { mapsUrl ->
                val mapIntent = Intent(Intent.ACTION_VIEW, mapsUrl.toUri())
                mapIntent.setPackage("com.google.android.apps.maps")
                mapIntent.resolveActivity(packageManager)?.let {
                    startActivity(mapIntent)
                } ?: openUrlExt(mapsUrl)
            }
        }
    }

    override fun getViewBinding(): AcTripModeBinding {
        return AcTripModeBinding.inflate(layoutInflater)
    }
}