package com.tripian.trpcore.ui.timeline.compose.addplan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.databinding.ActivityStartingPointSelectionBinding
import com.tripian.trpcore.ui.timeline.addplan.ACStartingPointSelection
import com.tripian.trpcore.ui.timeline.addplan.ACStartingPointSelectionVM
import com.tripian.trpcore.ui.timeline.addplan.SavedItemsAdapter
import com.tripian.trpcore.ui.timeline.addplan.SearchResultsAdapter
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineComposeScreen
import com.tripian.trpcore.ui.timeline.compose.core.timelineViewModel
import com.tripian.trpcore.ui.timeline.compose.nav.LocalTimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.StartingPointArgs
import com.tripian.trpcore.ui.timeline.compose.nav.StartingPointResult
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineResults
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.extensions.applyBottomSystemBarInsetPadding
import com.tripian.trpcore.util.widget.SearchBarView

private const val STATE_SAVED_LIST = "state_saved_list"
private const val STATE_SEARCH_LIST = "state_search_list"
private const val STATE_QUERY = "state_query"

/**
 * Compose route of [ACStartingPointSelection]: picks the starting point of a
 * plan (near me, city center, a saved activity or an address) and hands it back
 * to the Timeline as [TimelineResults.STARTING_POINT].
 */
@Composable
internal fun StartingPointScreen(
    args: StartingPointArgs,
    viewModel: ACStartingPointSelectionVM = timelineViewModel()
) {
    val navigator = LocalTimelineNavigator.current
    val context = LocalContext.current
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.selectCurrentLocation(viewModel)
        } else {
            showLocationPermissionDialog(context, viewModel)
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.arguments == null) {
            viewModel.arguments = ACStartingPointSelection.launch(
                context = context,
                city = args.city,
                bookedActivities = args.bookedActivities,
                favouriteItems = args.favouriteItems,
                userLocation = args.userLocation
            ).extras
            viewModel.onViewCreated(null)
            viewModel.initialize(args.city, args.bookedActivities, args.favouriteItems, args.userLocation)
        }
    }

    TimelineComposeScreen(viewModel = viewModel, onExit = { navigator.back() }) {
        BindingHost(
            inflate = ActivityStartingPointSelectionBinding::inflate,
            onSaveViewState = { binding, state ->
                binding.rvSavedActivities.layoutManager?.onSaveInstanceState()
                    ?.let { state.putParcelable(STATE_SAVED_LIST, it) }
                binding.rvSearchResults.layoutManager?.onSaveInstanceState()
                    ?.let { state.putParcelable(STATE_SEARCH_LIST, it) }
                state.putString(STATE_QUERY, binding.searchBar.getText())
            }
        ) { binding, owner, viewState ->
            val savedItemsAdapter = SavedItemsAdapter { item -> viewModel.selectSavedItem(item) }
            val searchResultsAdapter = SearchResultsAdapter { place -> viewModel.fetchPlaceDetails(place) }
            binding.setupLabels(viewModel)
            binding.setupLists(savedItemsAdapter, searchResultsAdapter)
            binding.setupSearchBar(viewModel)
            binding.btnBack.setOnClickListener { navigator.back() }
            binding.nearMeOption.setOnClickListener {
                val granted = ContextCompat.checkSelfPermission(
                    binding.root.context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    fusedLocationClient.selectCurrentLocation(viewModel)
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            binding.cityCenterOption.setOnClickListener { viewModel.selectCityCenter() }
            binding.observe(savedItemsAdapter, searchResultsAdapter, viewModel, owner, viewState) { result ->
                navigator.finishWithResult(TimelineResults.STARTING_POINT, result)
            }
            viewState.getString(STATE_QUERY)?.takeIf { it.isNotEmpty() }?.let { query ->
                viewState.remove(STATE_QUERY)
                binding.searchBar.setText(query)
            }
        }
    }
}

private fun ActivityStartingPointSelectionBinding.setupLabels(viewModel: ACStartingPointSelectionVM) {
    searchBar.setHint(viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SEARCH_POI))
    tvNearMe.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_NEAR_ME)
    tvSectionTitle.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SAVED_ACTIVITIES)
    tvSearchEmpty.text = viewModel.getLanguageForKey(LanguageConst.DESTINATION_SEARCH_NO_RESULTS)
}

private fun ActivityStartingPointSelectionBinding.setupLists(
    savedItemsAdapter: SavedItemsAdapter,
    searchResultsAdapter: SearchResultsAdapter
) {
    rvSavedActivities.applyBottomSystemBarInsetPadding()
    rvSavedActivities.layoutManager = LinearLayoutManager(root.context)
    rvSavedActivities.adapter = savedItemsAdapter
    rvSearchResults.applyBottomSystemBarInsetPadding()
    rvSearchResults.layoutManager = LinearLayoutManager(root.context)
    rvSearchResults.adapter = searchResultsAdapter
}

private fun ActivityStartingPointSelectionBinding.setupSearchBar(viewModel: ACStartingPointSelectionVM) {
    searchBar.setOnTextChangedListener { text ->
        val searching = text.isNotEmpty()
        defaultContentView.visibility = if (searching) View.GONE else View.VISIBLE
        rvSearchResults.visibility = if (searching) View.VISIBLE else View.GONE
        if (!searching) tvSearchEmpty.visibility = View.GONE
    }
    searchBar.setOnQueryChangedListener(SearchBarView.Mode.REMOTE) { query ->
        if (query.isEmpty()) viewModel.clearSearchResults() else viewModel.searchAddress(query)
    }
}

private fun ActivityStartingPointSelectionBinding.observe(
    savedItemsAdapter: SavedItemsAdapter,
    searchResultsAdapter: SearchResultsAdapter,
    viewModel: ACStartingPointSelectionVM,
    owner: LifecycleOwner,
    viewState: Bundle,
    onSelected: (StartingPointResult) -> Unit
) {
    viewModel.searchResults.observe(owner) { results ->
        searchResultsAdapter.submitList(results) { rvSearchResults.restoreListState(viewState, STATE_SEARCH_LIST) }
        updateSearchEmptyState(viewModel)
    }
    viewModel.filteredSavedItems.observe(owner) { items ->
        savedItemsAdapter.submitList(items) { rvSavedActivities.restoreListState(viewState, STATE_SAVED_LIST) }
        tvSectionTitle.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
        rvSavedActivities.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
    }
    viewModel.isLoading.observe(owner) { isLoading ->
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        updateSearchEmptyState(viewModel)
    }
    viewModel.isUserInCity.observe(owner) { isInCity ->
        nearMeOption.visibility = if (isInCity) View.VISIBLE else View.GONE
    }
    viewModel.cityCenterDisplayName.observe(owner) { name ->
        tvCityCenterName.text = name
    }
    viewModel.selectedPlace.observe(owner) { selected ->
        selected ?: return@observe
        viewModel.clearSelectedPlace()
        onSelected(
            StartingPointResult(
                name = selected.name,
                lat = selected.coordinate.lat,
                lng = selected.coordinate.lng,
                accommodation = selected.accommodation
            )
        )
    }
}

/** Shows the no-results text only for a finished, non-empty search that returned nothing. */
private fun ActivityStartingPointSelectionBinding.updateSearchEmptyState(viewModel: ACStartingPointSelectionVM) {
    val showEmpty = searchBar.getText().trim().isNotEmpty() &&
        viewModel.searchResults.value.isNullOrEmpty() &&
        viewModel.isLoading.value != true
    tvSearchEmpty.visibility = if (showEmpty) View.VISIBLE else View.GONE
}

private fun RecyclerView.restoreListState(viewState: Bundle, key: String) {
    val state = BundleCompat.getParcelable(viewState, key, Parcelable::class.java) ?: return
    viewState.remove(key)
    layoutManager?.onRestoreInstanceState(state)
}

@Suppress("MissingPermission")
private fun FusedLocationProviderClient.selectCurrentLocation(viewModel: ACStartingPointSelectionVM) {
    lastLocation.addOnSuccessListener { location ->
        location ?: return@addOnSuccessListener
        val coordinate = Coordinate().apply {
            lat = location.latitude
            lng = location.longitude
        }
        viewModel.selectNearMe(coordinate, viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_NEAR_ME))
    }
}

private fun showLocationPermissionDialog(context: Context, viewModel: ACStartingPointSelectionVM) {
    viewModel.showDialog(
        title = viewModel.getLanguageForKey(LanguageConst.ENABLE_LOCATION_PERMISSION),
        contentText = viewModel.getLanguageForKey(LanguageConst.ERROR_LOCATION_PERMISSION),
        positiveBtn = viewModel.getLanguageForKey(LanguageConst.SETTINGS),
        negativeBtn = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_CANCEL),
        positive = object : DGActionListener {
            override fun onClicked(o: Any?) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        },
        isCloseEnable = true
    )
}
