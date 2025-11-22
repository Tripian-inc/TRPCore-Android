package com.tripian.trpcore.ui.trip.places

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseDialogFragment
import com.tripian.trpcore.databinding.FrSearchPlaceBinding
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.util.AnimatorListener
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.RecyclerViewScrollListener
import com.tripian.trpcore.util.extensions.hideKeyboard
import com.tripian.trpcore.util.extensions.observe
import com.tripian.trpcore.util.extensions.showKeyboard
import kotlin.math.hypot

/**
 * Created by semihozkoroglu on 2.10.2020.
 */
class FRSearch :
    BaseDialogFragment<FrSearchPlaceBinding, FRSearchVM>(FrSearchPlaceBinding::inflate) {

    private lateinit var layoutManager: LinearLayoutManager

    companion object {
        fun newInstance(): FRSearch {
            return FRSearch()
        }
    }

//    override fun getToolbarProperties(): ToolbarProperties? {
//        return ToolbarProperties(title = getString(R.string.page_title_search), type = Navigation.BACK)
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = layoutManager

        binding.appBarLayout.post {
            // get the center for the clipping circle
            val cx = binding.appBarLayout.right
            val cy = binding.appBarLayout.height / 2

            // get the initial radius for the clipping circle
            val initialRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()

            val anim = ViewAnimationUtils.createCircularReveal(
                binding.appBarLayout,
                cx,
                cy,
                0f,
                initialRadius
            )
            binding.appBarLayout.visibility = View.VISIBLE
            anim.addListener(object : AnimatorListener() {
                override fun onAnimationEnd(p0: Animator) {
                    super.onAnimationEnd(p0)

                    binding.etSearch.showKeyboard()
                }
            })
            anim.start()
        }
    }

    override fun setListeners() {
        super.setListeners()
        binding.etSearch.hint = viewModel.getLanguageForKey(LanguageConst.SEARCH)
        binding.etSearch.doAfterTextChanged { viewModel.onSearchEntered(it.toString()) }
        binding.imNavigation.setOnClickListener {
            hideDialog()
        }
        binding.root.setOnClickListener { hideDialog() }

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

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetPlaceListener) {
            if (binding.rvList.adapter == null) {
                binding.rvList.adapter = object : AdapterPlace(
                    requireContext(),
                    it!!,
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

        observe(viewModel.onShowProgressListener) {
            binding.pbProgress.visibility = View.VISIBLE
        }

        observe(viewModel.onHideProgressListener) {
            binding.pbProgress.visibility = View.GONE
        }

        observe(viewModel.onShowErrorListener) {
            binding.tvError.visibility = View.VISIBLE
        }

        observe(viewModel.onHideErrorListener) {
            binding.tvError.visibility = View.GONE
        }
    }

    private fun hideDialog() {
        // get the center for the clipping circle
        val cx = binding.appBarLayout.right
        val cy = binding.appBarLayout.height / 2

        // get the initial radius for the clipping circle
        val initialRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()

        val anim =
            ViewAnimationUtils.createCircularReveal(binding.appBarLayout, cx, cy, initialRadius, 0f)

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)

                binding.appBarLayout.visibility = View.INVISIBLE

                dismiss()
            }
        })

        anim.start()
    }

    override fun dismiss() {
        binding.etSearch.hideKeyboard()

        super.dismiss()
    }
}