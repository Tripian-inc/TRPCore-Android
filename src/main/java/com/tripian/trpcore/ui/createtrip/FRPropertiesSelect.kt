package com.tripian.trpcore.ui.createtrip

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.DatePicker
import androidx.core.view.isVisible
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrPropertiesSelectBinding
import com.tripian.trpcore.util.extensions.observe
import java.util.Calendar

/**
 * Created by semihozkoroglu on 21.08.2020.
 */
class FRPropertiesSelect :
    BaseFragment<FrPropertiesSelectBinding, FRPropertiesSelectVM>(FrPropertiesSelectBinding::inflate) {

    companion object {
        fun newInstance(): FRPropertiesSelect {
            return FRPropertiesSelect()
        }
    }

    override fun setListeners() {
        super.setListeners()

        binding.llArrivalDate.setOnClickListener { viewModel.onClickedArrivalDate() }
        binding.llDepartureDate.setOnClickListener { viewModel.onClickedDepartureDate() }
        binding.llArrivalTime.setOnClickListener { viewModel.onClickedArrivalTime() }
        binding.llDepartureTime.setOnClickListener { viewModel.onClickedDepartureTime() }

        binding.llPassengerCount.setOnClickListener { viewModel.onClickedCompanionCount() }
        binding.rlCompanionSelect.setOnClickListener { viewModel.onClickedCompanionSelect() }
        binding.rlHotel.setOnClickListener { viewModel.onClickedSearch() }
        binding.imCancelHotel.setOnClickListener { viewModel.onClickedCancelHotel() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onShowSearchListener) {
            val intent = Intent(context, ACSearchAddress::class.java)

            val data = Bundle()
            data.putSerializable("city", it!!)

            intent.putExtras(data)
//
//            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity as Activity, tiSearchAddress as View, "search")
//            ActivityCompat.startActivityForResult(requireActivity(), intent, 1, options.toBundle())

            startActivity(intent)
        }

        observe(viewModel.onSetPlaceListener) {
            if (TextUtils.isEmpty(it)) {
                binding.llHotel.isVisible = false
                binding.imCancelHotel.isVisible = false
                binding.imArrow.isVisible = true
            } else {
                binding.llHotel.isVisible = true
                binding.imCancelHotel.isVisible = true
                binding.imArrow.isVisible = false

                binding.tvHotel.text = it
            }
        }

        observe(viewModel.onSetCompanionListener) {
            if (TextUtils.isEmpty(it)) {
                binding.llCompanion.visibility = View.GONE
            } else {
                binding.llCompanion.visibility = View.VISIBLE
            }

            binding.tvCompanions.text = it
        }

        observe(viewModel.onSetArrivalDateListener) {
            binding.tvArrivalDay.text = it!!.first
            binding.tvArrivalMonth.text = it.second
            binding.tvArrivalYear.text = it.third
        }

        observe(viewModel.onSetDepartureDateListener) {
            binding.tvDepartureDay.text = it!!.first
            binding.tvDepartureMonth.text = it.second
            binding.tvDepartureYear.text = it.third
        }

        observe(viewModel.onSetArrivalTimeListener) {
            binding.tvArrivalTime.text = it
        }

        observe(viewModel.onSetDepartureTimeListener) {
            binding.tvDepartureTime.text = it
        }

        observe(viewModel.onSetCompanionCountListener) {
            binding.tvPassengerCount.text = it
        }

        observe(viewModel.onOpenArrivalDateListener) {
            showDatePicker({ _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                viewModel.onArrivalDateSelected(dayOfMonth, monthOfYear, year)
            }, it!!.first, it.second, false)
        }

        observe(viewModel.onOpenDepartureDateListener) {
            showDatePicker({ _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                viewModel.onDepartureDateSelected(dayOfMonth, monthOfYear, year)
            }, it!!.first, it.second, true)
        }
    }

    private fun showDatePicker(listener: DatePickerDialog.OnDateSetListener, selectedDate: Long, minDate: Long, depatureDate: Boolean) {
        val currentCalendar = Calendar.getInstance()
        currentCalendar.timeInMillis = selectedDate

        val datePicker = DatePickerDialog(
            requireContext(), listener,
            currentCalendar[Calendar.YEAR], currentCalendar[Calendar.MONTH], currentCalendar[Calendar.DAY_OF_MONTH]
        )

        val minCalendar = Calendar.getInstance()
        minCalendar.timeInMillis = minDate
        datePicker.datePicker.minDate = minCalendar.timeInMillis
        if (depatureDate) {
            val maxCalendar = Calendar.getInstance()
            maxCalendar.timeInMillis = minDate
            maxCalendar.add(Calendar.DAY_OF_MONTH, 11)
            datePicker.datePicker.maxDate = maxCalendar.timeInMillis
        }
        datePicker.show()
        datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
        datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
    }
}