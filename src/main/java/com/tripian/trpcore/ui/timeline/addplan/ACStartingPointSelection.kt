package com.tripian.trpcore.ui.timeline.addplan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.FRWarning
import com.tripian.trpcore.databinding.ActivityStartingPointSelectionBinding
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.widget.SearchBarView
import java.io.Serializable

/**
 * ACStartingPointSelection
 * Full screen activity for selecting a starting point
 * iOS Reference: AddPlanPOISelectionVC.swift
 */
class ACStartingPointSelection : BaseActivity<ActivityStartingPointSelectionBinding, ACStartingPointSelectionVM>() {

    private var savedItemsAdapter: SavedItemsAdapter? = null
    private var searchResultsAdapter: SearchResultsAdapter? = null

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun getViewBinding() = ActivityStartingPointSelectionBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupLabels()
        setupAdapters()
        setupSearchBar()
        setupClickListeners()
        initializeFromIntent()
    }

    /**
     * Set all label texts using language service
     */
    private fun setupLabels() {
        binding.searchBar.setHint(getLanguageForKey(LanguageConst.ADD_PLAN_SEARCH_POI))
        binding.tvNearMe.text = getLanguageForKey(LanguageConst.ADD_PLAN_NEAR_ME)
        binding.tvSectionTitle.text = getLanguageForKey(LanguageConst.ADD_PLAN_SAVED_ACTIVITIES)
        binding.tvSearchEmpty.text = lang(LanguageConst.DESTINATION_SEARCH_NO_RESULTS, "No destinations found")
    }

    private fun lang(key: String, fallback: String): String =
        getLanguageForKey(key).let { if (it.isBlank() || it == key) fallback else it }

    override fun setReceivers() {
        viewModel.searchResults.observe(this) { results ->
            searchResultsAdapter?.submitList(results)
            updateSearchEmptyState()
        }

        viewModel.filteredSavedItems.observe(this) { items ->
            savedItemsAdapter?.submitList(items)
            updateSavedActivitiesSectionVisibility(items.isNotEmpty())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            updateSearchEmptyState()
        }

        viewModel.isUserInCity.observe(this) { isInCity ->
            binding.nearMeOption.visibility = if (isInCity) View.VISIBLE else View.GONE
        }

        viewModel.cityCenterDisplayName.observe(this) { name ->
            binding.tvCityCenterName.text = name
        }

        viewModel.selectedPlace.observe(this) { selectedLocation ->
            selectedLocation?.let {
                returnResult(it.coordinate, it.name, it.accommodation)
                viewModel.clearSelectedPlace()
            }
        }
    }

    private fun setupAdapters() {
        savedItemsAdapter = SavedItemsAdapter { savedItem ->
            viewModel.selectSavedItem(savedItem)
        }
        binding.rvSavedActivities.apply {
            layoutManager = LinearLayoutManager(this@ACStartingPointSelection)
            adapter = savedItemsAdapter
        }

        searchResultsAdapter = SearchResultsAdapter { place ->
            viewModel.fetchPlaceDetails(place)
        }
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(this@ACStartingPointSelection)
            adapter = searchResultsAdapter
        }
    }

    private fun setupSearchBar() {
        binding.searchBar.setOnTextChangedListener { text ->
            if (text.isEmpty()) showDefaultContent() else showSearchResults()
        }
        binding.searchBar.setOnQueryChangedListener(SearchBarView.Mode.REMOTE) { query ->
            if (query.isEmpty()) {
                viewModel.clearSearchResults()
            } else {
                viewModel.searchAddress(query)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.nearMeOption.setOnClickListener {
            handleNearMeTapped()
        }

        binding.cityCenterOption.setOnClickListener {
            viewModel.selectCityCenter()
        }
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    private fun initializeFromIntent() {
        val city = intent.getSerializableExtra(EXTRA_CITY) as? City
        val bookedActivities = intent.getSerializableExtra(EXTRA_BOOKED_ACTIVITIES) as? ArrayList<TimelineSegment> ?: arrayListOf()
        val favouriteItems = intent.getSerializableExtra(EXTRA_FAVOURITE_ITEMS) as? ArrayList<SegmentFavoriteItem> ?: arrayListOf()
        val userLat = intent.getDoubleExtra(EXTRA_USER_LAT, Double.NaN)
        val userLng = intent.getDoubleExtra(EXTRA_USER_LNG, Double.NaN)

        val userLocation = if (!userLat.isNaN() && !userLng.isNaN()) {
            Coordinate().apply {
                lat = userLat
                lng = userLng
            }
        } else {
            null
        }

        viewModel.initialize(city, bookedActivities, favouriteItems, userLocation)
    }

    // =====================
    // UI STATE
    // =====================

    private fun showDefaultContent() {
        binding.defaultContentView.visibility = View.VISIBLE
        binding.rvSearchResults.visibility = View.GONE
        binding.tvSearchEmpty.visibility = View.GONE
    }

    private fun showSearchResults() {
        binding.defaultContentView.visibility = View.GONE
        binding.rvSearchResults.visibility = View.VISIBLE
    }

    /** Shows the no-results text only for a finished, non-empty search that returned nothing. */
    private fun updateSearchEmptyState() {
        val query = binding.searchBar.getText().trim()
        val showEmpty = query.isNotEmpty() &&
                viewModel.searchResults.value.isNullOrEmpty() &&
                viewModel.isLoading.value != true
        binding.tvSearchEmpty.visibility = if (showEmpty) View.VISIBLE else View.GONE
    }

    private fun updateSavedActivitiesSectionVisibility(hasItems: Boolean) {
        binding.tvSectionTitle.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.rvSavedActivities.visibility = if (hasItems) View.VISIBLE else View.GONE
    }

    // =====================
    // NEAR ME HANDLING
    // =====================

    private fun handleNearMeTapped() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> {
                requestCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestCurrentLocation()
        } else {
            showLocationPermissionDialog()
        }
    }

    @Suppress("MissingPermission")
    private fun requestCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val coordinate = Coordinate().apply {
                    lat = it.latitude
                    lng = it.longitude
                }
                val name = getLanguageForKey(LanguageConst.ADD_PLAN_NEAR_ME)
                viewModel.selectNearMe(coordinate, name)
            }
        }
    }

    private fun showLocationPermissionDialog() {
        val dialog = FRWarning.newInstance(
            title = getLanguageForKey(LanguageConst.ENABLE_LOCATION_PERMISSION),
            contentText = getLanguageForKey(LanguageConst.ERROR_LOCATION_PERMISSION),
            positiveBtn = getLanguageForKey(LanguageConst.SETTINGS),
            negativeBtn = getLanguageForKey(LanguageConst.ADD_PLAN_CANCEL),
            isCloseEnable = true
        )
        dialog.positiveListener = object : DGActionListener {
            override fun onClicked(o: Any?) {
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }
        dialog.negativeListener = object : DGActionListener {
            override fun onClicked(o: Any?) {
                dialog.dismiss()
            }
        }
        dialog.show(supportFragmentManager, "LocationPermissionDialog")
    }

    // =====================
    // RESULT HANDLING
    // =====================

    private fun returnResult(coordinate: Coordinate, name: String, accommodation: Accommodation?) {
        val resultIntent = Intent().apply {
            putExtra(RESULT_COORDINATE_LAT, coordinate.lat)
            putExtra(RESULT_COORDINATE_LNG, coordinate.lng)
            putExtra(RESULT_NAME, name)
            accommodation?.let { putExtra(RESULT_ACCOMMODATION, it) }
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_CITY = "city"
        const val EXTRA_BOOKED_ACTIVITIES = "booked_activities"
        const val EXTRA_FAVOURITE_ITEMS = "favourite_items"
        const val EXTRA_USER_LAT = "user_lat"
        const val EXTRA_USER_LNG = "user_lng"

        const val RESULT_COORDINATE_LAT = "coordinate_lat"
        const val RESULT_COORDINATE_LNG = "coordinate_lng"
        const val RESULT_NAME = "name"
        const val RESULT_ACCOMMODATION = "accommodation"

        const val REQUEST_CODE = 2001

        fun launch(
            context: Context,
            city: City?,
            bookedActivities: List<TimelineSegment> = emptyList(),
            favouriteItems: List<SegmentFavoriteItem> = emptyList(),
            userLocation: Coordinate? = null
        ): Intent {
            return Intent(context, ACStartingPointSelection::class.java).apply {
                city?.let { putExtra(EXTRA_CITY, it) }
                putExtra(EXTRA_BOOKED_ACTIVITIES, ArrayList(bookedActivities))
                putExtra(EXTRA_FAVOURITE_ITEMS, ArrayList(favouriteItems))
                userLocation?.let {
                    putExtra(EXTRA_USER_LAT, it.lat)
                    putExtra(EXTRA_USER_LNG, it.lng)
                }
            }
        }
    }
}
