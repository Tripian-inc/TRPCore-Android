package com.tripian.trpcore.ui.timeline.addplan

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
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

    // Shared ViewModel from parent
    private val sharedVM: AddPlanContainerVM by lazy {
        ViewModelProvider(requireParentFragment())[AddPlanContainerVM::class.java]
    }

    private var dayFilterAdapter: DayFilterAdapter? = null

    // Activity Result Launcher for Starting Point Selection
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

        // Add to Day section
        binding.tvAddToDayLabel.text = getLanguage(LanguageConst.ADD_PLAN_ADD_TO_DAY)

        // City section (commented out - may be needed later)
        // binding.tvCityLabel.text = getLanguage(LanguageConst.ADD_PLAN_CITY)

        // Starting Point section
        binding.tvSelectStartingPointLabel.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_STARTING_POINT)

        // Time section
        binding.tvSelectTimeLabel.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_TIME)
        binding.tvStartTimeLabel.text = getLanguage(LanguageConst.ADD_PLAN_START_TIME)
        binding.tvEndTimeLabel.text = getLanguage(LanguageConst.ADD_PLAN_END_TIME)

        // Travelers section
        binding.tvSelectTravelersLabel.text = getLanguage(LanguageConst.ADD_PLAN_SELECT_TRAVELERS)
        binding.tvTravelersLabel.text = getLanguage(LanguageConst.ADD_PLAN_TRAVELERS)
    }

    private fun setupDayFilterRecyclerView() {
        dayFilterAdapter = DayFilterAdapter { position ->
            sharedVM.selectDay(position)
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
     * Show Compose TimePicker Dialog for start time selection
     */
    private fun showStartTimePicker() {
        val currentTime = sharedVM.startTime.value

        showComposeTimePicker(
            initialTime = currentTime,
            onTimeSelected = { hour, minute ->
                val time24h = MaterialTimePickerHelper.formatTo24h(hour, minute)
                sharedVM.setStartTime(time24h)

                // Clear end time if it's now invalid (before new start time)
                val endTime = sharedVM.endTime.value
                if (endTime != null && !MaterialTimePickerHelper.isEndTimeAfterStartTime(time24h, endTime)) {
                    sharedVM.setEndTime(null)
                    showSnackbar(LanguageConst.ADD_PLAN_END_TIME_CLEARED)
                }
            }
        )
    }

    /**
     * Show Compose TimePicker Dialog for end time selection
     */
    private fun showEndTimePicker() {
        val startTime = sharedVM.startTime.value

        // Validate start time is selected first
        if (startTime == null) {
            showAlert(LanguageConst.ADD_PLAN_SELECT_START_TIME_FIRST)
            return
        }

        val currentTime = sharedVM.endTime.value

        showComposeTimePicker(
            initialTime = currentTime,
            onTimeSelected = { hour, minute ->
                val time24h = MaterialTimePickerHelper.formatTo24h(hour, minute)

                // Validate end time > start time
                if (MaterialTimePickerHelper.isEndTimeAfterStartTime(startTime, time24h)) {
                    sharedVM.setEndTime(time24h)
                } else {
                    showInvalidTimeError()
                }
            }
        )
    }

    /**
     * Show error snackbar for invalid time selection
     */
    private fun showInvalidTimeError() {
        val message = TRPCore.core.miscRepository.getLanguageValueForKey(
            LanguageConst.ADD_PLAN_END_TIME_MUST_BE_AFTER_START
        )
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.trp_error_message))
            .setTextColor(Color.WHITE)
            .show()
    }

    /**
     * Show info snackbar (e.g., end time cleared)
     */
    private fun showSnackbar(languageKey: String) {
        val message = TRPCore.core.miscRepository.getLanguageValueForKey(languageKey)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Show alert for missing start time
     */
    private fun showAlert(languageKey: String) {
        val message = TRPCore.core.miscRepository.getLanguageValueForKey(languageKey)
        android.app.AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupTravelers() {
        binding.btnMinus.setOnClickListener {
            sharedVM.decrementTravelers()
        }

        binding.btnPlus.setOnClickListener {
            sharedVM.incrementTravelers()
        }
    }

    // City selection commented out - may be needed later
    private fun setupCitySelection() {
        // binding.btnCitySelection.setOnClickListener {
        //     val cities = sharedVM.cities.value ?: return@setOnClickListener
        //     val selectedCity = sharedVM.selectedCity.value
        //
        //     CitySelectionBottomSheet.newInstance(
        //         cities = cities,
        //         selectedCity = selectedCity
        //     ) { city ->
        //         sharedVM.selectCity(city)
        //     }.show(childFragmentManager, CitySelectionBottomSheet.TAG)
        // }
    }

    private fun observeViewModel() {
        // Available days
        sharedVM.availableDays.observe(viewLifecycleOwner) { days ->
            dayFilterAdapter?.setDays(days)
        }

        // Selected day index
        sharedVM.selectedDayIndex.observe(viewLifecycleOwner) { index ->
            dayFilterAdapter?.setSelectedPosition(index)
        }

        // Cities (commented out - may be needed later)
        // sharedVM.cities.observe(viewLifecycleOwner) { cities ->
        //     binding.llCitySelection.visibility = if (cities.size > 1) View.VISIBLE else View.GONE
        // }

        // Starting point name (custom)
        sharedVM.startingPointName.observe(viewLifecycleOwner) { name ->
            if (name != null) {
                binding.tvStartingPoint.text = name
            }
        }

        // Starting point default (language key) - show with city name
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

        // Update starting point when city changes (if using default)
        sharedVM.selectedCity.observe(viewLifecycleOwner) { city ->
            // binding.tvSelectedCity.text = city?.name ?: ""  // Commented out - may be needed later
            // Update starting point text if using default (city center)
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

        // Start time
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

        // End time
        sharedVM.endTime.observe(viewLifecycleOwner) { time ->
            val selectText = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_SELECT)
            binding.tvEndTime.text = time?.let { MaterialTimePickerHelper.formatTo12h(it) } ?: selectText
            binding.tvEndTime.setTextColor(
                requireContext().getColor(
                    if (time != null) R.color.trp_text_primary
                    else R.color.trp_fgWeak
                )
            )
        }

        // Travelers count
        sharedVM.travelers.observe(viewLifecycleOwner) { count ->
            binding.tvTravelerCount.text = count.toString()
            // Disable minus button if count is 1
            binding.btnMinus.alpha = if (count <= 1) 0.5f else 1.0f
            binding.btnMinus.isEnabled = count > 1
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
