package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.trip.model.Question
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrTripQuestionBinding
import com.tripian.trpcore.util.CreateTripSteps
import com.tripian.trpcore.util.ToolbarProperties
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 21.08.2020.
 */
class FRCreateTripItineraryProfile : BaseFragment<FrTripQuestionBinding, FRCreateTripItineraryProfileVM>(FrTripQuestionBinding::inflate) {

    private var adapterQuestions: AdapterCreateTripItineraryQuestions? = null

    companion object {
        fun newInstance(): FRCreateTripItineraryProfile {
            return FRCreateTripItineraryProfile()
        }
    }

    override fun getToolbarProperties(): ToolbarProperties {
        return ToolbarProperties(createTripStep = CreateTripSteps.ITINERARY_PROFILE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvPaceList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetQuestionsListener) {
            adapterQuestions = object : AdapterCreateTripItineraryQuestions(requireContext(), it!!.first, it.second) {
                override fun notified() {
                    viewModel.onAnswerUpdated(adapterQuestions!!.getSelectedItems())
                }

                override fun onClickedSpinnerItem(question: Question) {
                    viewModel.showSpinnerAnswerSelection(question)

                }
            }

            binding.rvList.adapter = adapterQuestions
        }

        observe(viewModel.onSpinnerAnswerSelected) { answers ->
            answers?.let {
                adapterQuestions?.answers = answers
                adapterQuestions?.notifyDataSetChanged()
            }
        }
    }

    fun canClickNext(): Boolean {
        return if (adapterQuestions!!.isAnswerOK()) {
            true
        } else {
            viewModel.onShowError()
            false
        }
    }
}