package com.tripian.trpcore.ui.timeline.addplan

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.FetchPlace
import com.tripian.trpcore.domain.SearchAddress
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.domain.model.timeline.SavedItem
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Job
import javax.inject.Inject

/**
 * ViewModel for Starting Point Selection screen
 * iOS Reference: AddPlanPOISelectionViewModel.swift
 */
class ACStartingPointSelectionVM @Inject constructor(
    private val searchAddressUseCase: SearchAddress,
    private val fetchPlaceUseCase: FetchPlace
) : BaseViewModel() {

    // =====================
    // LIVEDATA
    // =====================

    private val _searchResults = MutableLiveData<List<PlaceAutocomplete>>()
    val searchResults: LiveData<List<PlaceAutocomplete>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isUserInCity = MutableLiveData<Boolean>()
    val isUserInCity: LiveData<Boolean> = _isUserInCity

    private val _filteredSavedItems = MutableLiveData<List<SavedItem>>()
    val filteredSavedItems: LiveData<List<SavedItem>> = _filteredSavedItems

    private val _selectedPlace = MutableLiveData<SelectedLocation?>()
    val selectedPlace: LiveData<SelectedLocation?> = _selectedPlace

    private val _cityCenterDisplayName = MutableLiveData<String>()
    val cityCenterDisplayName: LiveData<String> = _cityCenterDisplayName

    // =====================
    // STATE
    // =====================

    private var city: City? = null
    private var bookedActivities: List<TimelineSegment> = emptyList()
    private var favouriteItems: List<SegmentFavoriteItem> = emptyList()
    private var userLocation: Coordinate? = null

    private val disposables = CompositeDisposable()
    private var searchJob: Job? = null
    private val searchDebounceMs = 650L

    // =====================
    // INITIALIZATION
    // =====================

    fun initialize(
        city: City?,
        bookedActivities: List<TimelineSegment>,
        favouriteItems: List<SegmentFavoriteItem>,
        userLocation: Coordinate?
    ) {
        this.city = city
        this.bookedActivities = bookedActivities
        this.favouriteItems = favouriteItems
        this.userLocation = userLocation

        // Update city center display name
        updateCityCenterDisplayName()

        // Filter items by city
        filterItemsByCity()

        // Check if user is in city
        checkUserLocationInCity()
    }

    // =====================
    // CITY CENTER
    // =====================

    private fun updateCityCenterDisplayName() {
        val cityName = city?.name ?: return
        var cityCenterLabel = getLanguageForKey(LanguageConst.ADD_PLAN_CITY_CENTER)
        if (cityCenterLabel.isNullOrEmpty() || cityCenterLabel == LanguageConst.ADD_PLAN_CITY_CENTER) {
            cityCenterLabel = "City Center"
        }
        _cityCenterDisplayName.value = "$cityName | $cityCenterLabel"
    }

    fun getCityCenterLocation(): Coordinate? {
        return city?.coordinate
    }

    fun getCityCenterName(): String {
        return _cityCenterDisplayName.value ?: ""
    }

    // =====================
    // SAVED ITEMS FILTERING
    // =====================

    private fun filterItemsByCity() {
        val items = mutableListOf<SavedItem>()
        val cityId = city?.id
        val cityName = city?.name

        // Filter booked activities by city
        bookedActivities.forEach { segment ->
            if (matchesCityFilter(segment.cityId, null, cityId, cityName)) {
                items.add(SavedItem.BookedActivity(segment))
            }
        }

        // Filter favourite items by city
        favouriteItems.forEach { item ->
            if (matchesCityFilter(item.cityId, item.cityName, cityId, cityName)) {
                items.add(SavedItem.FavouriteActivity(item))
            }
        }

        _filteredSavedItems.value = items
    }

    private fun matchesCityFilter(
        itemCityId: Int?,
        itemCityName: String?,
        filterCityId: Int?,
        filterCityName: String?
    ): Boolean {
        // If no filter criteria, show all
        if (filterCityId == null && filterCityName == null) return true

        // Filter by cityId if available
        filterCityId?.let { filterId ->
            itemCityId?.let { itemId ->
                if (filterId == itemId) return true
            }
        }

        // Fallback to city name matching
        filterCityName?.let { filterName ->
            itemCityName?.let { itemName ->
                if (filterName.equals(itemName, ignoreCase = true)) return true
            }
        }

        return false
    }

    fun hasSavedItems(): Boolean = (_filteredSavedItems.value?.size ?: 0) > 0

    // =====================
    // USER LOCATION CHECK
    // =====================

    private fun checkUserLocationInCity() {
        val userLoc = userLocation ?: run {
            _isUserInCity.value = false
            return
        }

        val cityCoord = city?.coordinate ?: run {
            _isUserInCity.value = false
            return
        }

        // Check using city boundaries if available
        val boundary = city?.boundary
        if (boundary != null && boundary.size >= 4) {
            val south = boundary[0]
            val north = boundary[1]
            val west = boundary[2]
            val east = boundary[3]

            val inLat = userLoc.lat in minOf(south, north)..maxOf(south, north)
            val inLon = userLoc.lng in minOf(west, east)..maxOf(west, east)

            _isUserInCity.value = inLat && inLon
        } else {
            // Fallback: Check distance from city center (50km radius)
            val distance = calculateDistance(
                userLoc.lat, userLoc.lng,
                cityCoord.lat, cityCoord.lng
            )
            _isUserInCity.value = distance < 50000 // 50km in meters
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun getUserLocation(): Coordinate? = userLocation

    // =====================
    // GOOGLE PLACES SEARCH
    // =====================

    fun searchAddress(text: String) {
        val searchText = text.trim()
        if (searchText.isEmpty()) {
            clearSearchResults()
            return
        }

        val currentCity = city ?: return

        _isLoading.value = true

        searchAddressUseCase.on(
            params = SearchAddress.Params(currentCity, searchText),
            success = { results ->
                _isLoading.value = false
                _searchResults.value = results
            },
            error = { errorModel ->
                _isLoading.value = false
                _searchResults.value = emptyList()
            }
        )
    }

    fun fetchPlaceDetails(place: PlaceAutocomplete) {
        _isLoading.value = true

        fetchPlaceUseCase.on(
            params = FetchPlace.Params(place),
            success = { googlePlace ->
                _isLoading.value = false
                googlePlace?.let {
                    val coordinate = Coordinate().apply {
                        it.location?.let { latLng ->
                            lat = latLng.latitude
                            lng = latLng.longitude
                        } ?: run {
                            lat = 0.0
                            lng = 0.0
                        }
                    }
                    val accommodation = Accommodation().apply {
                        refID = it.id
                        name = it.displayName
                        address = it.formattedAddress
                        this.coordinate = coordinate
                    }
                    _selectedPlace.value = SelectedLocation(
                        coordinate = coordinate,
                        name = place.area?.toString() ?: it.displayName ?: "",
                        accommodation = accommodation
                    )
                }
            },
            error = { errorModel ->
                _isLoading.value = false
                showAlert(AlertType.ERROR, errorModel.errorDesc)
            }
        )
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun clearSelectedPlace() {
        _selectedPlace.value = null
    }

    // =====================
    // SELECTION HELPERS
    // =====================

    fun selectNearMe(coordinate: Coordinate, name: String) {
        _selectedPlace.value = SelectedLocation(
            coordinate = coordinate,
            name = name,
            accommodation = null
        )
    }

    fun selectCityCenter() {
        // Get city coordinate - use city object or fallback
        val coordinate: Coordinate
        val name: String

        if (city != null && city!!.coordinate != null) {
            coordinate = city!!.coordinate!!
            name = getCityCenterName().ifEmpty { city!!.name ?: "City Center" }
        } else {
            // Fallback if city or coordinate is null - still allow selection with default values
            coordinate = Coordinate().apply {
                lat = 0.0
                lng = 0.0
            }
            name = city?.name ?: "City Center"
        }

        _selectedPlace.value = SelectedLocation(
            coordinate = coordinate,
            name = name,
            accommodation = null
        )
    }

    fun selectSavedItem(item: SavedItem) {
        val coordinate = item.coordinate ?: return
        _selectedPlace.value = SelectedLocation(
            coordinate = coordinate,
            name = item.title,
            accommodation = null
        )
    }

    // =====================
    // LIFECYCLE
    // =====================

    override fun onDestroy() {
        disposables.clear()
        searchJob?.cancel()
        super.onDestroy()
    }
}

/**
 * Data class for selected location result
 */
data class SelectedLocation(
    val coordinate: Coordinate,
    val name: String,
    val accommodation: Accommodation?
)
