package com.tripian.trpcore.ui.user

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrEditProfileBinding
import com.tripian.trpcore.ui.companion.AdapterCompanionQuestions
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe
import java.util.Calendar

/**
 * Created by semihozkoroglu on 24.08.2020.
 */
class FREditProfile : BaseFragment<FrEditProfileBinding, FREditProfileVM>(FrEditProfileBinding::inflate) {

    var selectedDate: Long = 0

    private var adapterQuestions: AdapterCompanionQuestions? = null

    companion object {
        fun newInstance(): FREditProfile {
            return FREditProfile()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setListeners() {
        super.setListeners()

        binding.etMail.hint = getLanguageForKey(LanguageConst.EMAIL_PLACEHOLDER)
        binding.etAge.hint = getLanguageForKey(LanguageConst.DATE_OF_BIRTH)
        binding.tvChangePassword.text = getLanguageForKey(LanguageConst.CHANGE_PSW)
        binding.btnDeleteAccount.text = getLanguageForKey(LanguageConst.DELETE_MY_ACCOUNT)
        binding.btnCreate.text = getLanguageForKey(LanguageConst.UPDATE)

        binding.btnCreate.setOnClickListener {
            viewModel.onClickedUpdate(
                firstName = binding.etFirstName.text.toString(),
                lastName = binding.etLastName.text.toString(),
                dateOfBirth = binding.etAge.text.toString(),
                answers = adapterQuestions?.getSelectedItems()
            )
        }

        binding.tvChangePassword.setOnClickListener { viewModel.onClickedChangePassword() }

        binding.etAge.setOnClickListener {
            showDatePicker { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                viewModel.onBirthDaySelected(dayOfMonth, monthOfYear, year)

                val calendar = Calendar.getInstance()

                calendar[Calendar.YEAR] = year
                calendar[Calendar.MONTH] = monthOfYear
                calendar[Calendar.DAY_OF_MONTH] = dayOfMonth

                selectedDate = calendar.timeInMillis
            }
        }

        binding.btnDeleteAccount.setOnClickListener { viewModel.onClickedDeleteUser() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetFirstNameListener) {
            binding.etFirstName.setText(it)
        }

        observe(viewModel.onSetLastNameListener) {
            binding.etLastName.setText(it)
        }

        observe(viewModel.onSetEmailListener) {
            binding.etMail.setText(it)
        }

        observe(viewModel.onSetAgeListener) {
            binding.etAge.setText(it)
        }

        observe(viewModel.onSetQuestionsListener) {
            adapterQuestions = object : AdapterCompanionQuestions(requireContext(), it!!.first, it.second) {
                override fun notified() {
                }
            }

            binding.rvList.adapter = adapterQuestions
        }
    }

    private fun showDatePicker(listener: DatePickerDialog.OnDateSetListener) {

        if (selectedDate == 0L) {
            selectedDate = Calendar.getInstance().timeInMillis
        }

        val currentCalendar = Calendar.getInstance()
        currentCalendar.timeInMillis = selectedDate

        val datePicker = DatePickerDialog(
            requireContext(), R.style.datePicker, listener,
            currentCalendar[Calendar.YEAR], currentCalendar[Calendar.MONTH], currentCalendar[Calendar.DAY_OF_MONTH]
        )

        val maxCalendar = Calendar.getInstance()
        datePicker.datePicker.maxDate = maxCalendar.timeInMillis

        datePicker.show()
        datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
        datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
    }
}