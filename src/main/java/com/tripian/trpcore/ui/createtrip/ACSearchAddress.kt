package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcSearchAddressBinding
import com.tripian.trpcore.domain.model.PlaceAutocomplete
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.hideKeyboard
import com.tripian.trpcore.util.extensions.observe
import com.tripian.trpcore.util.extensions.showKeyboard

class ACSearchAddress : BaseActivity<AcSearchAddressBinding, ACSearchAddressVM>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.rvList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    override fun getViewBinding(): AcSearchAddressBinding {
        return AcSearchAddressBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.tvTitle.text = getLanguageForKey(LanguageConst.WHERE_START)
        binding.tvError.text = getLanguageForKey(LanguageConst.ENTER_START)
        binding.btnCancel.text = getLanguageForKey(LanguageConst.CANCEL)
        binding.etSearch.hint = getLanguageForKey(LanguageConst.SEARCH)
        binding.imNavigation.setOnClickListener {
            binding.etSearch.hideKeyboard()
            viewModel.onClickedBack()
        }

        binding.etSearch.showKeyboard()
        binding.etSearch.doAfterTextChanged { viewModel.find(it.toString()) }
    }

    override fun setReceivers() {
        observe(viewModel.onSetPlacesListener) {
            if (it.isNullOrEmpty()) {
                binding.tvError.visibility = View.VISIBLE
                binding.rvList.visibility = View.GONE
            } else {
                binding.tvError.visibility = View.GONE
                binding.rvList.visibility = View.VISIBLE
            }

            binding.rvList.adapter = object : AdapterSelectPlace(this, it!!) {
                override fun onClickedItem(place: PlaceAutocomplete) {
                    hideKeyboard()

                    viewModel.onClickedItem(place)
                }
            }
        }

        observe(viewModel.onShowProgressListener) {
            binding.pbProgress.visibility = View.VISIBLE
        }

        observe(viewModel.onHideProgressListener) {
            binding.pbProgress.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        binding.etSearch.hideKeyboard()
        super.onBackPressed()
    }
}