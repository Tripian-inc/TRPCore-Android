package com.tripian.trpcore.ui.login

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrRegisterBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe
import com.tripian.trpcore.util.widget.ImageView
import java.util.Calendar

/**
 * Created by semihozkoroglu on 29.05.2021.
 */
class FRRegister : BaseFragment<FrRegisterBinding, FRRegisterVM>(FrRegisterBinding::inflate) {


    companion object {
        fun newInstance(): FRRegister {
            return FRRegister()
        }
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvTitle.text = getLanguageForKey(LanguageConst.REGISTER)
        binding.etFirstName.hint = getLanguageForKey(LanguageConst.ENTER_FIRST_NAME)
        binding.etLastName.hint = getLanguageForKey(LanguageConst.ENTER_LAST_NAME)
        binding.etMail.hint = getLanguageForKey(LanguageConst.EMAIL_PLACEHOLDER)
        binding.etPassword.hint = getLanguageForKey(LanguageConst.ENTER_NEW_PASSWORD)
        binding.etPassword2.hint = getLanguageForKey(LanguageConst.RETYPE_NEW_PASSWORD)
        binding.etBirthDate.hint = getLanguageForKey(LanguageConst.DATE_OF_BIRTH)
        binding.btnRegister.text = getLanguageForKey(LanguageConst.SIGN_UP_NOW)

        binding.imNavigation.setOnClickListener { viewModel.onClickedBack() }

        binding.etBirthDate.setOnClickListener {
            showDatePicker { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                viewModel.onBirthDaySelected(dayOfMonth, monthOfYear, year)
            }
        }

        binding.chkToe.setOnClickListener {
            if (viewModel.isToeChecked) {
                binding.chkToe.setBackgroundResource(R.drawable.ic_check_empty_new)
            } else {
                binding.chkToe.setBackgroundResource(R.drawable.ic_check_new)
            }
            viewModel.isToeChecked = !viewModel.isToeChecked
        }

        binding.btnRegister.setOnClickListener {
            viewModel.onClickedRegister(
                binding.etFirstName.text.toString(),
                binding.etLastName.text.toString(),
                binding.etMail.text.toString(),
                binding.etPassword.text.toString(),
                binding.etPassword2.text.toString(),
                binding.etBirthDate.text.toString()
            )
        }

        setupPasswordShowHide(binding.imShowPassword, binding.etPassword)
        setupPasswordShowHide(binding.imShowPassword2, binding.etPassword2)

        setupToeTextView()
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetDateListener) {
            binding.etBirthDate.setText(it)
        }
    }

    private fun setupPasswordShowHide(im: ImageView, et: EditText) {
        im.setOnClickListener {
            if (im.tag == null || im.tag == false) {
                et.transformationMethod = HideReturnsTransformationMethod.getInstance()
                im.tag = true
                im.setImageResource(R.drawable.ic_eye_on)
            } else {
                et.transformationMethod = PasswordTransformationMethod.getInstance()
                im.tag = false
                im.setImageResource(R.drawable.ic_eye_off)
            }
        }
    }

    private fun showDatePicker(listener: DatePickerDialog.OnDateSetListener) {

        if (viewModel.selectedDate == 0L) {
            viewModel.selectedDate = Calendar.getInstance().timeInMillis
        }

        val currentCalendar = Calendar.getInstance()
        currentCalendar.timeInMillis = viewModel.selectedDate

        val datePicker = DatePickerDialog(
            requireContext(), R.style.TrpDatePicker, listener,
            currentCalendar[Calendar.YEAR], currentCalendar[Calendar.MONTH], currentCalendar[Calendar.DAY_OF_MONTH]
        )

        val maxCalendar = Calendar.getInstance()
        datePicker.datePicker.maxDate = maxCalendar.timeInMillis

        datePicker.show()
        datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
        datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
    }

    private fun setupToeTextView() {
//        val ss = SpannableString("I accept Terms of Use")
        val ss = SpannableString("By checking this, i agree to the Terms of Use")
        val clickableSpan: ClickableSpan = object : ClickableSpan() {

            override fun onClick(textView: View) {
                viewModel.openToeWebPage()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(requireContext(), R.color.trp_primary)
                ds.isUnderlineText = true
            }
        }
        ss.setSpan(clickableSpan, 33, 45, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val boldSpan = StyleSpan(Typeface.BOLD)
        ss.setSpan(boldSpan, 33, 45, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvToe.text = ss
        binding.tvToe.movementMethod = LinkMovementMethod.getInstance()
        binding.tvToe.highlightColor = Color.TRANSPARENT
    }
}