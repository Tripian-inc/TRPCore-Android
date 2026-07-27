package com.tripian.trpcore.ui.timeline.compose.addplan

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.FrAddPlanCategorySelectionBinding
import com.tripian.trpcore.databinding.FrAddPlanSelectDayBinding
import com.tripian.trpcore.databinding.FrAddPlanTimeTravelersBinding
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.domain.model.timeline.ManualCategory
import com.tripian.trpcore.ui.timeline.addplan.ACStartingPointSelection
import com.tripian.trpcore.ui.timeline.addplan.AddPlanContainerVM
import com.tripian.trpcore.ui.timeline.addplan.MaterialTimePickerHelper
import com.tripian.trpcore.ui.timeline.addplan.SmartCategoryAdapter
import com.tripian.trpcore.ui.timeline.addplan.TimePickerDialogContent
import com.tripian.trpcore.ui.timeline.adapter.DayFilterAdapter
import com.tripian.trpcore.util.LanguageConst
import java.util.Date

private fun getLanguage(key: String): String {
    return TRPCore.core.miscRepository.getLanguageValueForKey(key)
}

private fun updateCardSelection(card: MaterialCardView, selected: Boolean) {
    if (selected) {
        card.strokeColor = card.context.getColor(R.color.trp_text_primary)
        card.strokeWidth = 2
    } else {
        card.strokeColor = card.context.getColor(R.color.trp_lineWeak)
        card.strokeWidth = 1
    }
}

private class SelectDayStepViews(
    val binding: FrAddPlanSelectDayBinding,
    val dayAdapter: DayFilterAdapter
) {
    var lastDays: List<Date>? = null
    var lastMode: AddPlanMode? = null
}

/**
 * Step 1 of the Compose AddPlan wizard. Reuses fr_add_plan_select_day.xml and
 * DayFilterAdapter with the wiring ported from FRSelectDay; city selection
 * opens a Compose sheet instead of the FragmentManager-based one.
 */
@Composable
internal fun AddPlanSelectDayStep(viewModel: AddPlanContainerVM) {
    val availableDays by viewModel.availableDays.observeAsState(emptyList())
    val selectedDayIndex by viewModel.selectedDayIndex.observeAsState(0)
    val cities by viewModel.cities.observeAsState(emptyList())
    val isLoadingCities by viewModel.isLoadingCities.observeAsState(false)
    val selectedCity by viewModel.selectedCity.observeAsState()
    val selectedMode by viewModel.selectedMode.observeAsState(AddPlanMode.NONE)
    val selectedManualCategory by viewModel.selectedManualCategory.observeAsState()
    val travelers by viewModel.travelers.observeAsState(1)
    var showCitySheet by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val binding = FrAddPlanSelectDayBinding.inflate(LayoutInflater.from(ctx))
            binding.tvAddToDay.text = getLanguage(LanguageConst.ADD_PLAN_ADD_TO_DAY)
            binding.tvCityLabel.text = getLanguage(LanguageConst.ADD_PLAN_DESTINATION)
            binding.tvHowToAdd.text = getLanguage(LanguageConst.ADD_PLAN_HOW_TO_ADD)
            binding.tvSmartTitle.text = getLanguage(LanguageConst.ADD_PLAN_SMART_RECOMMENDATIONS)
            binding.tvSmartDesc.text = getLanguage(LanguageConst.ADD_PLAN_SMART_DESC)
            binding.tvManualTitle.text = getLanguage(LanguageConst.ADD_PLAN_ADD_MANUALLY)
            binding.tvManualDesc.text = getLanguage(LanguageConst.ADD_PLAN_MANUAL_DESC)
            binding.tvSelectCategoriesLabel.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_CATEGORIES)
            binding.tvCatActivities.text = getLanguage(LanguageConst.ADD_PLAN_CAT_MANUAL_ACTIVITIES)
            binding.tvCatPlaces.text = getLanguage(LanguageConst.ADD_PLAN_CAT_MANUAL_PLACES)
            binding.tvCatEatDrink.text = getLanguage(LanguageConst.ADD_PLAN_CAT_MANUAL_EAT_DRINK)
            binding.tvSelectTravelersLabel.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_TRAVELERS)
            binding.tvTravelersLabel.text = getLanguage(LanguageConst.ADD_PLAN_TRAVELERS)

            val dayAdapter = DayFilterAdapter { position ->
                viewModel.selectDay(position)
            }.apply { disablePastDays = true }
            binding.rvDays.layoutManager =
                LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            binding.rvDays.adapter = dayAdapter

            binding.cardSmartRecommendations.setOnClickListener {
                viewModel.selectMode(AddPlanMode.SMART_RECOMMENDATIONS)
            }
            binding.cardAddManually.setOnClickListener {
                viewModel.selectMode(AddPlanMode.MANUAL)
            }
            binding.cardManualActivities.setOnClickListener {
                viewModel.selectManualCategory(ManualCategory.ACTIVITIES)
            }
            binding.cardManualPlaces.setOnClickListener {
                viewModel.selectManualCategory(ManualCategory.PLACES_OF_INTEREST)
            }
            binding.cardManualEatDrink.setOnClickListener {
                viewModel.selectManualCategory(ManualCategory.EAT_AND_DRINK)
            }
            binding.btnCitySelection.setOnClickListener {
                showCitySheet = true
            }
            binding.btnTravelersMinus.setOnClickListener { viewModel.decrementTravelers() }
            binding.btnTravelersPlus.setOnClickListener { viewModel.incrementTravelers() }

            binding.root.also { it.tag = SelectDayStepViews(binding, dayAdapter) }
        },
        update = { root ->
            val views = root.tag as SelectDayStepViews
            val b = views.binding
            if (views.lastDays !== availableDays) {
                views.lastDays = availableDays
                views.dayAdapter.setDays(availableDays)
            }
            views.dayAdapter.setSelectedPosition(selectedDayIndex)
            views.dayAdapter.timeZoneId = selectedCity?.timezone

            val shouldShowCity = cities.size > 1 || viewModel.shouldAlwaysShowCitySelection()
            b.llCitySelection.visibility = if (shouldShowCity) View.VISIBLE else View.GONE
            b.btnCitySelection.isEnabled = !isLoadingCities
            b.tvSelectedCity.text = when {
                isLoadingCities -> "..."
                else -> selectedCity?.name ?: getLanguage(LanguageConst.ADD_PLAN_SELECT)
            }

            val smartSelected = selectedMode == AddPlanMode.SMART_RECOMMENDATIONS
            val manualSelected = selectedMode == AddPlanMode.MANUAL
            b.cardSmartRecommendations.isSelected = smartSelected
            b.cardAddManually.isSelected = manualSelected
            updateCardSelection(b.cardSmartRecommendations, smartSelected)
            updateCardSelection(b.cardAddManually, manualSelected)
            val manualVisibility = if (manualSelected) View.VISIBLE else View.GONE
            b.llManualCategories.visibility = manualVisibility
            b.llTravelersSection.visibility = manualVisibility
            if (manualSelected && views.lastMode != selectedMode) {
                b.root.post { (b.root as? NestedScrollView)?.fullScroll(View.FOCUS_DOWN) }
            }
            views.lastMode = selectedMode

            b.cardManualActivities.isSelected = selectedManualCategory == ManualCategory.ACTIVITIES
            b.cardManualPlaces.isSelected =
                selectedManualCategory == ManualCategory.PLACES_OF_INTEREST
            b.cardManualEatDrink.isSelected = selectedManualCategory == ManualCategory.EAT_AND_DRINK
            updateCardSelection(
                b.cardManualActivities, selectedManualCategory == ManualCategory.ACTIVITIES
            )
            updateCardSelection(
                b.cardManualPlaces, selectedManualCategory == ManualCategory.PLACES_OF_INTEREST
            )
            updateCardSelection(
                b.cardManualEatDrink, selectedManualCategory == ManualCategory.EAT_AND_DRINK
            )

            b.tvTravelersCount.text = travelers.toString()
            b.btnTravelersMinus.alpha = if (travelers <= 1) 0.5f else 1.0f
            b.btnTravelersMinus.isEnabled = travelers > 1
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showCitySheet) {
        AddPlanCitySheet(
            cities = cities,
            selectedCity = selectedCity,
            onSelect = { city ->
                viewModel.selectCity(city)
                showCitySheet = false
            },
            onDismiss = { showCitySheet = false }
        )
    }
}

private class TimeTravelersStepViews(
    val binding: FrAddPlanTimeTravelersBinding,
    val dayAdapter: DayFilterAdapter
) {
    var lastDays: List<Date>? = null
}

private data class AddPlanTimePickerRequest(
    val initialTime: String?,
    val minTime: String?,
    val treatMidnightAsEndOfDay: Boolean,
    val isEnd: Boolean
)

/**
 * Step 2 of the Compose AddPlan wizard. Reuses fr_add_plan_time_travelers.xml
 * with the wiring ported from FRTimeAndTravelers; the starting point Activity
 * keeps its result contract and the time pickers reuse TimePickerDialogContent.
 */
@Composable
internal fun AddPlanTimeTravelersStep(viewModel: AddPlanContainerVM) {
    val availableDays by viewModel.availableDays.observeAsState(emptyList())
    val selectedDayIndex by viewModel.selectedDayIndex.observeAsState(0)
    val selectedCity by viewModel.selectedCity.observeAsState()
    val startingPointName by viewModel.startingPointName.observeAsState()
    val startingPointNameKey by viewModel.startingPointNameKey.observeAsState()
    val startTime by viewModel.startTime.observeAsState()
    val endTime by viewModel.endTime.observeAsState()
    val travelers by viewModel.travelers.observeAsState(1)
    var pickerRequest by remember { mutableStateOf<AddPlanTimePickerRequest?>(null) }

    val startingPointLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val lat = data.getDoubleExtra(ACStartingPointSelection.RESULT_COORDINATE_LAT, Double.NaN)
                val lng = data.getDoubleExtra(ACStartingPointSelection.RESULT_COORDINATE_LNG, Double.NaN)
                val name = data.getStringExtra(ACStartingPointSelection.RESULT_NAME) ?: ""
                @Suppress("DEPRECATION")
                val accommodation = data.getSerializableExtra(
                    ACStartingPointSelection.RESULT_ACCOMMODATION
                ) as? Accommodation
                if (!lat.isNaN() && !lng.isNaN()) {
                    val coordinate = Coordinate().apply {
                        this.lat = lat
                        this.lng = lng
                    }
                    viewModel.setStartingPoint(name, coordinate, accommodation)
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val binding = FrAddPlanTimeTravelersBinding.inflate(LayoutInflater.from(ctx))
            binding.tvAddToDayLabel.text = getLanguage(LanguageConst.ADD_PLAN_ADD_TO_DAY)
            binding.tvSelectStartingPointLabel.text =
                getLanguage(LanguageConst.ADD_PLAN_SELECT_STARTING_POINT)
            binding.tvStartTimeLabel.text = getLanguage(LanguageConst.ADD_PLAN_START_TIME)
            binding.tvEndTimeLabel.text = getLanguage(LanguageConst.ADD_PLAN_END_TIME)
            binding.tvSelectTravelersLabel.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_TRAVELERS)
            binding.tvTravelersLabel.text = getLanguage(LanguageConst.ADD_PLAN_TRAVELERS)

            val dayAdapter = DayFilterAdapter { position ->
                viewModel.selectDay(position)
            }.apply {
                disablePastDays = true
                timeZoneId = viewModel.selectedCityTimeZone()
            }
            binding.rvDays.layoutManager =
                LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            binding.rvDays.adapter = dayAdapter

            binding.btnStartingPoint.setOnClickListener {
                val intent = ACStartingPointSelection.launch(
                    context = ctx,
                    city = viewModel.selectedCity.value,
                    bookedActivities = viewModel.getBookedActivities(),
                    favouriteItems = emptyList(),
                    userLocation = viewModel.getUserLocation()
                )
                startingPointLauncher.launch(intent)
            }
            binding.btnStartTime.setOnClickListener {
                pickerRequest = AddPlanTimePickerRequest(
                    initialTime = viewModel.startTime.value
                        ?: viewModel.defaultStartTimeForSelectedDay(),
                    minTime = viewModel.minSelectableTimeForSelectedDay(),
                    treatMidnightAsEndOfDay = false,
                    isEnd = false
                )
            }
            binding.btnEndTime.setOnClickListener {
                val start = viewModel.startTime.value
                val current = viewModel.endTime.value
                val initialTime = when {
                    current == MaterialTimePickerHelper.END_OF_DAY_24H -> "00:00"
                    current != null -> current
                    else -> MaterialTimePickerHelper.addMinutes(start, 60) ?: start
                }
                pickerRequest = AddPlanTimePickerRequest(
                    initialTime = initialTime,
                    minTime = MaterialTimePickerHelper.laterOf(
                        start, viewModel.minSelectableTimeForSelectedDay()
                    ),
                    treatMidnightAsEndOfDay = true,
                    isEnd = true
                )
            }
            binding.btnMinus.setOnClickListener { viewModel.decrementTravelers() }
            binding.btnPlus.setOnClickListener { viewModel.incrementTravelers() }

            binding.root.also { it.tag = TimeTravelersStepViews(binding, dayAdapter) }
        },
        update = { root ->
            val views = root.tag as TimeTravelersStepViews
            val b = views.binding
            if (views.lastDays !== availableDays) {
                views.lastDays = availableDays
                views.dayAdapter.setDays(availableDays)
            }
            views.dayAdapter.setSelectedPosition(selectedDayIndex)

            val name = startingPointName
            val key = startingPointNameKey
            b.tvStartingPoint.text = when {
                name != null -> name
                key != null -> {
                    val cityCenterLabel = getLanguage(key)
                    selectedCity?.name?.let { "$it | $cityCenterLabel" } ?: cityCenterLabel
                }
                else -> ""
            }

            val selectText = getLanguage(LanguageConst.ADD_PLAN_SELECT)
            b.tvStartTime.text =
                startTime?.let { MaterialTimePickerHelper.formatTo12h(it) } ?: selectText
            b.tvStartTime.setTextColor(
                b.root.context.getColor(
                    if (startTime != null) R.color.trp_text_primary else R.color.trp_fgWeak
                )
            )
            b.tvEndTime.text =
                endTime?.let { MaterialTimePickerHelper.formatEndTimeTo12h(it) } ?: selectText
            b.tvEndTime.setTextColor(
                b.root.context.getColor(
                    if (endTime != null) R.color.trp_text_primary else R.color.trp_fgWeak
                )
            )

            b.tvTravelerCount.text = travelers.toString()
            b.btnMinus.alpha = if (travelers <= 1) 0.5f else 1.0f
            b.btnMinus.isEnabled = travelers > 1
        },
        modifier = Modifier.fillMaxWidth()
    )

    pickerRequest?.let { request ->
        val initial = request.initialTime?.let { MaterialTimePickerHelper.parseTime24h(it) }
        val min = request.minTime?.let { MaterialTimePickerHelper.parseTime24h(it) }
        TimePickerDialogContent(
            initialHour = initial?.first ?: 10,
            initialMinute = initial?.second ?: 0,
            minHour = min?.first,
            minMinute = min?.second,
            treatMidnightAsEndOfDay = request.treatMidnightAsEndOfDay,
            cancelText = getLanguage(LanguageConst.ADD_PLAN_CANCEL),
            selectText = getLanguage(LanguageConst.ADD_PLAN_SELECT),
            onConfirm = { hour, minute ->
                if (request.isEnd) {
                    viewModel.setEndTime(MaterialTimePickerHelper.formatEndTimeTo24h(hour, minute))
                } else {
                    val time24h = MaterialTimePickerHelper.formatTo24h(hour, minute)
                    viewModel.setStartTime(time24h)
                    val currentEnd = viewModel.endTime.value
                    if (currentEnd != null &&
                        !MaterialTimePickerHelper.isEndTimeAfterStartTime(time24h, currentEnd)
                    ) {
                        viewModel.setEndTime(null)
                    }
                }
                pickerRequest = null
            },
            onCancel = { pickerRequest = null }
        )
    }
}

private class CategoryStepViews(
    val binding: FrAddPlanCategorySelectionBinding,
    val adapter: SmartCategoryAdapter
)

/**
 * Step 3 of the Compose AddPlan wizard. Reuses
 * fr_add_plan_category_selection.xml and SmartCategoryAdapter with the wiring
 * ported from FRCategorySelection.
 */
@Composable
internal fun AddPlanCategorySelectionStep(viewModel: AddPlanContainerVM) {
    val selectedSmartCategories by viewModel.selectedSmartCategories.observeAsState(emptyList())

    AndroidView(
        factory = { ctx ->
            val binding = FrAddPlanCategorySelectionBinding.inflate(LayoutInflater.from(ctx))
            binding.tvTitle.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_CATEGORIES)

            val categoryAdapter = SmartCategoryAdapter(
                getLanguageForKey = { key -> getLanguage(key) },
                onCategoryClicked = { category -> viewModel.toggleSmartCategory(category) }
            )
            val spanCount = 3
            val gridLayoutManager = GridLayoutManager(ctx, spanCount)
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val itemCount = categoryAdapter.itemCount
                    val itemsInLastRow = itemCount % spanCount
                    return if (itemsInLastRow == 1 && position == itemCount - 1) spanCount else 1
                }
            }
            binding.rvCategories.layoutManager = gridLayoutManager
            binding.rvCategories.adapter = categoryAdapter

            binding.root.also { it.tag = CategoryStepViews(binding, categoryAdapter) }
        },
        update = { root ->
            val views = root.tag as CategoryStepViews
            views.adapter.updateSelectedCategories(selectedSmartCategories)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Compose replacement for the FragmentManager-based CitySelectionBottomSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPlanCitySheet(
    cities: List<City>,
    selectedCity: City?,
    onSelect: (City) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colorResource(R.color.trp_white)
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                text = getLanguage(LanguageConst.ADD_PLAN_SELECT_CITY),
                color = colorResource(R.color.trp_text_primary),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
            cities.forEach { city ->
                val isSelected = city.id == selectedCity?.id
                Text(
                    text = city.name.orEmpty(),
                    color = colorResource(
                        if (isSelected) R.color.trp_primary else R.color.trp_text_primary
                    ),
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(city) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }
    }
}
