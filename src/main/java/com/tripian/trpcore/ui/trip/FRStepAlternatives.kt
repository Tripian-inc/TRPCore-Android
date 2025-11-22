package com.tripian.trpcore.ui.trip

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseDialogFragment
import com.tripian.trpcore.databinding.FrStepAlternativesBinding
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 27.09.2020.
 */
class FRStepAlternatives :
    BaseDialogFragment<FrStepAlternativesBinding, FRStepAlternativesVM>(FrStepAlternativesBinding::inflate) {

    companion object {
        fun newInstance(step: MapStep): FRStepAlternatives {
            val fragment = FRStepAlternatives()

            val data = Bundle()
            data.putSerializable("step", step)

            fragment.arguments = data

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSteps.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvTitle.text = getLanguageForKey(LanguageConst.ALTERNATIVES)
        binding.imClose.setOnClickListener { dismiss() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetStepListener) {
            binding.rvSteps.adapter = object : AdapterStepAlternatives(requireContext(), it!!) {
                override fun onClickedItem(step: MapStep) {
                    viewModel.onClickedItem(step)
                }

                override fun onClickedAlternatives(step: MapStep) {
                    viewModel.onClickedAlternatives(step)
                }
            }
        }

        observe(viewModel.onShowProgressListener) {
            binding.pbProgress.visibility = View.VISIBLE
            binding.rvSteps.visibility = View.GONE
        }

        observe(viewModel.onHideProgressListener) {
            binding.pbProgress.visibility = View.GONE
            binding.rvSteps.visibility = View.VISIBLE
        }
    }
}