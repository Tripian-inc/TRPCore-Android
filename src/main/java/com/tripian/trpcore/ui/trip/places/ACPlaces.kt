package com.tripian.trpcore.ui.trip.places

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcPlacesBinding
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.RecyclerViewScrollListener
import com.tripian.trpcore.util.extensions.hideKeyboard
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 27.09.2020.
 */
class ACPlaces : BaseActivity<AcPlacesBinding, ACPlacesVM>() {
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(R.anim.anim_slide_in_up, R.anim.anim_slide_out_up)
    }

    override fun onPause() {
        super.onPause()

        overridePendingTransition(R.anim.anim_slide_in_down, R.anim.anim_slide_out_down)
    }

    override fun getViewBinding(): AcPlacesBinding {
        return AcPlacesBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.tvTitle.text = getLanguageForKey(LanguageConst.PLACES)
        binding.btnCancel.text = getLanguageForKey(LanguageConst.CANCEL)
        binding.tvError.text = getLanguageForKey(LanguageConst.PLEASE_SELECT_CATEGORY)
        binding.etSearch.hint = getLanguageForKey(LanguageConst.SEARCH)
        binding.tvCategoryPlaceholder.hint = getLanguageForKey(LanguageConst.SELECT_CATEGORY)
        binding.btnCancel.setOnClickListener {
            binding.etSearch.setText("")
            hideSearch()
        }

        binding.etSearch.doAfterTextChanged {
            if (it.toString().isNotEmpty()) {
                binding.btnCancel.visibility = View.VISIBLE
            } else {
                binding.btnCancel.visibility = View.GONE
            }

            viewModel.onSearchEntered(it.toString())
        }

        binding.imNavigation.setOnClickListener { viewModel.onClickedBack() }
        binding.llCategory.setOnClickListener { viewModel.onClickedCategories() }

        layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.rvList.layoutManager = layoutManager

        binding.rvList.addOnScrollListener(object : RecyclerViewScrollListener(layoutManager) {
            override fun isLastPage(): Boolean {
                return viewModel.isLastPage()
            }

            override fun loadMoreItems() {
                viewModel.loadMoreItems()
            }

            override fun isLoading(): Boolean {
                return viewModel.isLoading()
            }
        })
    }

    private fun hideSearch() {
        hideSearchProgress()
        hideKeyboard()

        binding.etSearch.text?.clear()
        binding.etSearch.clearFocus()
    }

    override fun setReceivers() {
        observe(viewModel.onSetCategoriesListener) {
            binding.tvCategoryPlaceholder.isVisible = it.isNullOrEmpty()
            binding.tvCategory.isVisible = it.isNullOrEmpty().not()
            binding.tvCategory.text = it
        }

        observe(viewModel.onShowProgressListener) {
            showSearchProgress()
        }

        observe(viewModel.onHideProgressListener) {
            hideSearchProgress()
        }

        observe(viewModel.onShowErrorListener) {
            if (it == true) {
                binding.tvError.text = getLanguageForKey(LanguageConst.PLEASE_SELECT_CATEGORY)
            }
            binding.tvError.isVisible = it == true
        }

        observe(viewModel.onShowListErrorListener) {
            showListEmptyError(it == true)
        }

        observe(viewModel.onSetPlaceListener) {
            showListEmptyError(it?.second.isNullOrEmpty())
            if (binding.rvList.adapter == null || it!!.first) {
                binding.rvList.adapter = object : AdapterPlace(
                    context = this,
                    items = it!!.second,
                    miscRepository = viewModel.miscRepository
                ) {
                    override fun onClickedPlace(place: PlaceItem) {
                        viewModel.onClickedPlace(place)
                    }
                }
            } else {
                binding.rvList.adapter?.notifyDataSetChanged()
            }
        }

        observe(viewModel.onSetSearchPlaceListener) {
            binding.rvList.adapter = object : AdapterPlace(
                context = this,
                items = it!!,
                miscRepository = viewModel.miscRepository
            ) {
                override fun onClickedPlace(place: PlaceItem) {
                    viewModel.onClickedPlace(place)
                }
            }
        }
    }

    private fun showListEmptyError(show: Boolean) {
        if (show) {
            binding.tvError.text = getLanguageForKey(LanguageConst.NO_RESULT_FOUND)
        }
        binding.tvError.isVisible = show
    }

    fun showSearchProgress() {
        binding.pbProgress.visibility = View.VISIBLE
    }

    fun hideSearchProgress() {
        binding.pbProgress.visibility = View.GONE
    }
}