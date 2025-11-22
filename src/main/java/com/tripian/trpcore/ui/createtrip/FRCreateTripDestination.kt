package com.tripian.trpcore.ui.createtrip

import android.app.DatePickerDialog
import android.graphics.Color
import android.widget.DatePicker
import androidx.core.view.isVisible
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrCreateTripDestinationBinding
import com.tripian.trpcore.util.CreateTripSteps
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.ToolbarProperties
import com.tripian.trpcore.util.extensions.observe
import java.util.Calendar

/**
 * Created by cemcaygoz on 10.01.2023.
 */
class FRCreateTripDestination :
    BaseFragment<FrCreateTripDestinationBinding, FRCreateTripDestinationVM>(
        FrCreateTripDestinationBinding::inflate
    ) {

    companion object {
        fun newInstance(): FRCreateTripDestination {
            return FRCreateTripDestination()
        }
    }

    override fun getToolbarProperties(): ToolbarProperties {
        return ToolbarProperties(createTripStep = CreateTripSteps.DESTINATION)
    }

    override fun setListeners() {
        super.setListeners()
        binding.tvWhereAreYouGoing.text = viewModel.getLanguageForKey(LanguageConst.WHERE_YOU_GO)
        binding.tvDestinationPlaceholder.text = viewModel.getLanguageForKey(LanguageConst.DESTINATION_PLACEHOLDER)
        binding.tvSelectDates.text = viewModel.getLanguageForKey(LanguageConst.SELECT_DATES)
        binding.tvSelectHours.text = viewModel.getLanguageForKey(LanguageConst.SELECT_HOURS)
        binding.tvDateArrival.text = viewModel.getLanguageForKey(LanguageConst.ARRIVAL)
        binding.tvDateDeparture.text = viewModel.getLanguageForKey(LanguageConst.DEPARTURE)
        binding.tvHourArrival.text = viewModel.getLanguageForKey(LanguageConst.ARRIVAL)
        binding.tvHourDeparture.text = viewModel.getLanguageForKey(LanguageConst.DEPARTURE)

        binding.llDestination.setOnClickListener { viewModel.onClickedDestination() }

        binding.llArrivalDate.setOnClickListener { viewModel.onClickedArrivalDate() }
        binding.llDepartureDate.setOnClickListener { viewModel.onClickedDepartureDate() }
        binding.llArrivalTime.setOnClickListener { viewModel.onClickedArrivalTime(binding.tvArrivalTime.text.toString()) }
        binding.llDepartureTime.setOnClickListener { viewModel.onClickedDepartureTime(binding.tvDepartureTime.text.toString()) }

    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetArrivalDateListener) {
            it?.let {
                val date = "${it.first} ${it.second} ${it.third}"
                binding.tvArrivalDate.text = date
            }
        }

        observe(viewModel.onSetDepartureDateListener) {
            it?.let {
                val date = "${it.first} ${it.second} ${it.third}"
                binding.tvDepartureDate.text = date
            }
        }

        observe(viewModel.onSetArrivalTimeListener) {
            binding.tvArrivalTime.text = it
        }

        observe(viewModel.onSetDepartureTimeListener) {
            binding.tvDepartureTime.text = it
        }

        observe(viewModel.onOpenArrivalDateListener) {
            it?.let {
                showDatePicker({ _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                    viewModel.onArrivalDateSelected(dayOfMonth, monthOfYear, year)
                }, it.first, it.second, departureDate = false)
            }
        }

        observe(viewModel.onOpenDepartureDateListener) {
            it?.let {
                val currentMinimumPair = it.first
                showDatePicker({ _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                    viewModel.onDepartureDateSelected(dayOfMonth, monthOfYear, year)
                }, currentMinimumPair.first, currentMinimumPair.second, it.second, true)
            }
        }

        observe(viewModel.onSetDestinationListener) {
            if (!it.isNullOrEmpty()) {
                binding.tvDestinationPlaceholder.isVisible = false
                binding.tvDestination.isVisible = true
            }
            binding.tvDestination.text = it
        }
    }

    private fun showDatePicker(
        listener: DatePickerDialog.OnDateSetListener,
        selectedDate: Long,
        minDate: Long,
        maxDays: Int = 3,
        departureDate: Boolean
    ) {
        val currentCalendar = Calendar.getInstance()
        currentCalendar.timeInMillis = selectedDate

        val datePicker = DatePickerDialog(
            requireContext(),
            listener,
            currentCalendar[Calendar.YEAR],
            currentCalendar[Calendar.MONTH],
            currentCalendar[Calendar.DAY_OF_MONTH]
        )

        val minCalendar = Calendar.getInstance()
        minCalendar.timeInMillis = minDate
        datePicker.datePicker.minDate = minCalendar.timeInMillis
        if (departureDate) {
            val maxCalendar = Calendar.getInstance()
            maxCalendar.timeInMillis = minDate
            maxCalendar.add(Calendar.DAY_OF_MONTH, maxDays)
            datePicker.datePicker.maxDate = maxCalendar.timeInMillis
        }
        datePicker.show()
        datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
        datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
    }
}