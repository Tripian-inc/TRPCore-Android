package com.tripian.trpcore.ui.timeline.addplan

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.FrAddPlanTimeTravelersBinding
import com.tripian.trpcore.ui.timeline.adapter.DayFilterAdapter
import com.tripian.trpcore.util.LanguageConst

/**
 * FRTimeAndTravelers
 * Step 2: Starting Point, Time Selection, Travelers
 */
class FRTimeAndTravelers : Fragment() {

    private var _binding: FrAddPlanTimeTravelersBinding? = null
    private val binding get() = _binding!!

    private val sharedVM: AddPlanContainerVM by lazy {
        ViewModelProvider(requireParentFragment())[AddPlanContainerVM::class.java]
    }

    private var dayFilterAdapter: DayFilterAdapter? = null

    private val startingPointLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val lat = data.getDoubleExtra(ACStartingPointSelection.RESULT_COORDINATE_LAT, Double.NaN)
                val lng = data.getDoubleExtra(ACStartingPointSelection.RESULT_COORDINATE_LNG, Double.NaN)
                val name = data.getStringExtra(ACStartingPointSelection.RESULT_NAME) ?: ""
                @Suppress("DEPRECATION")
                val accommodation = data.getSerializableExtra(ACStartingPointSelection.RESULT_ACCOMMODATION) as? Accommodation

                if (!lat.isNaN() && !lng.isNaN()) {
                    val coordinate = Coordinate().apply {
                        this.lat = lat
                        this.lng = lng
                    }
                    sharedVM.setStartingPoint(name, coordinate, accommodation)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FrAddPlanTimeTravelersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLabels()
        setupDayFilterRecyclerView()
        setupStartingPoint()
        setupTimeSelection()
        setupTravelers()
        setupCitySelection()
        observeViewModel()
    }

    /**
     * Set all label texts using language service
     */
    private fun setupLabels() {
        val getLanguage: (String) -> String = { key ->
            TRPCore.core.miscRepository.getLanguageValueForKey(key)
        }

        binding.tvAddToDayLabel.text = getLanguage(LanguageConst.ADD_PLAN_ADD_TO_DAY)

        binding.tvSelectStartingPointLabel.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_STARTING_POINT)

        binding.tvStartTimeLabel.text = getLanguage(LanguageConst.ADD_PLAN_START_TIME)
        binding.tvEndTimeLabel.text = getLanguage(LanguageConst.ADD_PLAN_END_TIME)

        binding.tvSelectTravelersLabel.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_TRAVELERS)
        binding.tvTravelersLabel.text = getLanguage(LanguageConst.ADD_PLAN_TRAVELERS)
    }

    private fun setupDayFilterRecyclerView() {
        dayFilterAdapter = DayFilterAdapter { position ->
            sharedVM.selectDay(position)
        }.apply {
            disablePastDays = true
            timeZoneId = sharedVM.selectedCityTimeZone()
        }
        binding.rvDays.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dayFilterAdapter
        }
    }

    private fun setupStartingPoint() {
        binding.btnStartingPoint.setOnClickListener {
            openStartingPointSelection()
        }
    }

    private fun openStartingPointSelection() {
        val city = sharedVM.selectedCity.value
        val userLocation = sharedVM.getUserLocation()
        val bookedActivities = sharedVM.getBookedActivities()

        val intent = ACStartingPointSelection.launch(
            context = requireContext(),
            city = city,
            bookedActivities = bookedActivities,
            favouriteItems = emptyList(),   // TODO: Pass from parent if available
            userLocation = userLocation
        )

        startingPointLauncher.launch(intent)
    }

    private fun setupTimeSelection() {
        binding.btnStartTime.setOnClickListener {
            showStartTimePicker()
        }

        binding.btnEndTime.setOnClickListener {
            showEndTimePicker()
        }
    }

    /**
     * Show Compose TimePicker Dialog for start time selection.
     * The picker is floored at the city's "now" for the selected day so a past
     * time can't be chosen; with no start chosen, it opens on the earliest
     * selectable slot.
     */
    private fun showStartTimePicker() {
        val currentTime = sharedVM.startTime.value
        val minTime = sharedVM.minSelectableTimeForSelectedDay()

        showComposeTimePicker(
            initialTime = currentTime ?: MaterialTimePickerHelper.addMinutes(minTime, 1),
            minTime = minTime,
            onTimeSelected = { hour, minute ->
                val time24h = MaterialTimePickerHelper.formatTo24h(hour, minute)
                sharedVM.setStartTime(time24h)

                val endTime = sharedVM.endTime.value
                if (endTime != null && !MaterialTimePickerHelper.isEndTimeAfterStartTime(time24h, endTime)) {
                    sharedVM.setEndTime(null)
                }
            }
        )
    }

    /**
     * Show Compose TimePicker Dialog for end time selection.
     * End time can be picked first — when start is set, enforce end > start
     * via the picker's minTime; when start is null, allow any time.
     * A stored [MaterialTimePickerHelper.END_OF_DAY_24H] end (midnight sentinel)
     * reopens the clock at 00:00.
     */
    private fun showEndTimePicker() {
        val startTime = sharedVM.startTime.value
        val currentTime = sharedVM.endTime.value

        val initialTime = when {
            currentTime == MaterialTimePickerHelper.END_OF_DAY_24H -> "00:00"
            currentTime != null -> currentTime
            else -> MaterialTimePickerHelper.addMinutes(startTime, 60) ?: startTime
        }

        showComposeTimePicker(
            initialTime = initialTime,
            minTime = MaterialTimePickerHelper.laterOf(startTime, sharedVM.minSelectableTimeForSelectedDay()),
            treatMidnightAsEndOfDay = true,
            onTimeSelected = { hour, minute ->
                val time24h = MaterialTimePickerHelper.formatEndTimeTo24h(hour, minute)
                sharedVM.setEndTime(time24h)
            }
        )
    }

    private fun setupTravelers() {
        binding.btnMinus.setOnClickListener {
            sharedVM.decrementTravelers()
        }

        binding.btnPlus.setOnClickListener {
            sharedVM.incrementTravelers()
        }
    }

    private fun setupCitySelection() {
    }

    private fun observeViewModel() {
        sharedVM.availableDays.observe(viewLifecycleOwner) { days ->
            dayFilterAdapter?.setDays(days)
        }

        sharedVM.selectedDayIndex.observe(viewLifecycleOwner) { index ->
            dayFilterAdapter?.setSelectedPosition(index)
        }

        sharedVM.startingPointName.observe(viewLifecycleOwner) { name ->
            if (name != null) {
                binding.tvStartingPoint.text = name
            }
        }

        sharedVM.startingPointNameKey.observe(viewLifecycleOwner) { key ->
            if (key != null) {
                val cityName = sharedVM.selectedCity.value?.name
                val cityCenterLabel = TRPCore.core.miscRepository.getLanguageValueForKey(key)
                binding.tvStartingPoint.text = if (cityName != null) {
                    "$cityName | $cityCenterLabel"
                } else {
                    cityCenterLabel
                }
            }
        }

        sharedVM.selectedCity.observe(viewLifecycleOwner) { city ->
            val key = sharedVM.startingPointNameKey.value
            if (key != null && sharedVM.startingPointName.value == null) {
                val cityCenterLabel = TRPCore.core.miscRepository.getLanguageValueForKey(key)
                binding.tvStartingPoint.text = if (city?.name != null) {
                    "${city.name} | $cityCenterLabel"
                } else {
                    cityCenterLabel
                }
            }
        }

        sharedVM.startTime.observe(viewLifecycleOwner) { time ->
            val selectText = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_SELECT)
            binding.tvStartTime.text = time?.let { MaterialTimePickerHelper.formatTo12h(it) } ?: selectText
            binding.tvStartTime.setTextColor(
                requireContext().getColor(
                    if (time != null) R.color.trp_text_primary
                    else R.color.trp_fgWeak
                )
            )
        }

        sharedVM.endTime.observe(viewLifecycleOwner) { time ->
            val selectText = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_SELECT)
            binding.tvEndTime.text = time?.let { MaterialTimePickerHelper.formatEndTimeTo12h(it) } ?: selectText
            binding.tvEndTime.setTextColor(
                requireContext().getColor(
                    if (time != null) R.color.trp_text_primary
                    else R.color.trp_fgWeak
                )
            )
        }

        sharedVM.travelers.observe(viewLifecycleOwner) { count ->
            binding.tvTravelerCount.text = count.toString()
            binding.btnMinus.alpha = if (count <= 1) 0.5f else 1.0f
            binding.btnMinus.isEnabled = count > 1
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
