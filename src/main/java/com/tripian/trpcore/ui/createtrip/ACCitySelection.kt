package com.tripian.trpcore.ui.createtrip

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ActivityCitySelectionBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.consumeSystemBarPadding
import com.tripian.trpcore.util.extensions.observe

/**
 * Screen A of the native "create trip from scratch" flow: pick ONE destination
 * city. Reached from [com.tripian.trpcore.ui.splash.ACSplash] when the host
 * opens the SDK with no reservations. Back/hardware-back CLOSE the SDK.
 */
class ACCitySelection : BaseActivity<ActivityCitySelectionBinding, ACCitySelectionVM>() {

    private var adapter: CityListAdapter? = null

    override fun getViewBinding() = ActivityCitySelectionBinding.inflate(layoutInflater)

    /** Localized string with an English fallback (getLanguageForKey returns the
     *  key itself when missing). Keeps every label translation-driven. */
    private fun lang(key: String, fallback: String): String =
        getLanguageForKey(key).let { if (it.isBlank() || it == key) fallback else it }

    private fun setupTexts() {
        binding.tvTitle.text = lang(LanguageConst.WHERE_YOU_GO, "Where are you travelling?")
        binding.etSearch.hint = lang(LanguageConst.SEARCH, "Search")
        binding.tvNext.text = lang(LanguageConst.CREATE_TRIP_NEXT, "Next")
        binding.tvPopularLabel.text = lang(LanguageConst.POPULAR_CITIES, "Top Destinations")
        binding.tvAllLabel.text = lang(LanguageConst.EXPERIENCE_DESTINATIONS, "Destinations")
        binding.tvEmpty.text = lang(LanguageConst.DESTINATION_SEARCH_NO_RESULTS, "No destinations found")
    }

    override fun setListeners() {
        // Pad for both status bar (top) and navigation bar (bottom) so the
        // header and list never sit behind the device system bars.
        binding.root.consumeSystemBarPadding(top = true, bottom = true)
        setupTexts()
        adapter = CityListAdapter { city -> viewModel.select(city) }
        binding.rvCities.layoutManager = LinearLayoutManager(this)
        binding.rvCities.adapter = adapter

        binding.ivBack.setOnClickListener { TRPCore.closeSDK() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString().orEmpty()
                binding.ivClearSearch.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.search(q)
            }
        })
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { hideKeyboard(); true } else false
        }
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            viewModel.search("")
        }

        binding.tvNext.setOnClickListener { viewModel.proceed() }
    }

    override fun setReceivers() {
        observe(viewModel.cities) { list ->
            val cities = list ?: emptyList()
            adapter?.submitSelection(cities, viewModel.selectedCity.value?.id)
            binding.tvEmpty.visibility = if (cities.isEmpty()) View.VISIBLE else View.GONE
            binding.rvCities.visibility = if (cities.isEmpty()) View.GONE else View.VISIBLE
        }
        observe(viewModel.popularCities) { popular ->
            renderPopular(popular ?: emptyList())
        }
        viewModel.selectedCity.observe(this) { city ->
            adapter?.setSelected(city?.id)
            syncPopularChecked(city?.id)
            setNextEnabled(city != null)
        }
        observe(viewModel.onCitySelectedNext) { city ->
            city?.let { openDateStep(it) }
        }
    }

    override fun backPressed() {
        TRPCore.closeSDK()
    }

    private fun renderPopular(popular: List<City>) {
        binding.popularSection.visibility = if (popular.isEmpty()) View.GONE else View.VISIBLE
        binding.chipGroupPopular.removeAllViews()
        popular.forEach { city ->
            val chip = Chip(this).apply {
                text = city.name ?: ""
                isCheckable = true
                isChecked = viewModel.selectedCity.value?.id == city.id
                chipStrokeWidth = 0f
                setEnsureMinTouchTargetSize(false)
                tag = city.id
                setOnClickListener { viewModel.select(city) }
            }
            applyChipStyle(chip, chip.isChecked)
            binding.chipGroupPopular.addView(chip)
        }
    }

    private fun syncPopularChecked(selectedId: Int?) {
        val group = binding.chipGroupPopular
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            val checked = chip.tag == selectedId
            chip.isChecked = checked
            applyChipStyle(chip, checked)
        }
    }

    private fun applyChipStyle(chip: Chip, checked: Boolean) {
        chip.chipBackgroundColor = getColorStateList(
            if (checked) R.color.trp_timeline_primary else R.color.trp_timeline_background
        )
        chip.setTextColor(getColor(if (checked) R.color.trp_white else R.color.trp_text_primary))
    }

    private fun setNextEnabled(enabled: Boolean) {
        binding.tvNext.isEnabled = enabled
        binding.tvNext.alpha = if (enabled) 1f else 0.4f
    }

    private fun openDateStep(city: City) {
        startActivity(
            ACDateSelection.newIntent(
                context = this,
                city = city,
                language = intent.getStringExtra("language") ?: "en",
                currency = intent.getStringExtra("currency") ?: "EUR",
                uniqueId = intent.getStringExtra("uniqueId"),
                canBack = intent.getBooleanExtra("canBack", true)
            )
        )
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    companion object {
        fun newIntent(
            context: Context,
            language: String,
            currency: String,
            uniqueId: String?,
            canBack: Boolean
        ): Intent = Intent(context, ACCitySelection::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("language", language)
            putExtra("currency", currency)
            putExtra("uniqueId", uniqueId)
            putExtra("canBack", canBack)
        }
    }
}
