package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrTripQuestionBinding
import com.tripian.trpcore.domain.model.Pace
import com.tripian.trpcore.ui.companion.AdapterPace
import com.tripian.trpcore.ui.companion.AdapterQuestions
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 21.08.2020.
 */
class FRTripQuestion :
    BaseFragment<FrTripQuestionBinding, FRTripQuestionVM>(FrTripQuestionBinding::inflate) {

    private var adapterQuestions: AdapterQuestions? = null

    companion object {
        fun newInstance(): FRTripQuestion {
            return FRTripQuestion()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvPaceList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetQuestionsListener) {
            adapterQuestions = object : AdapterQuestions(requireContext(), it!!.first, it.second) {
                override fun notified() {
                    viewModel.onAnswerUpdated(adapterQuestions!!.getSelectedItems())
                }
            }

            binding.rvList.adapter = adapterQuestions
        }

        observe(viewModel.onSetPaceListener) {
            binding.tvPaceTitle.visibility = View.VISIBLE

            binding.rvPaceList.adapter = object : AdapterPace(requireContext(), it!!.first, it.second) {
                override fun onItemSelected(pace: Pace) {
                    viewModel.onPaceSelected(pace)
                }
            }
        }
    }

    fun onClickedNext() {
        if (adapterQuestions!!.isAnswerOK()) {
            viewModel.onClickedNext()
        } else {
            viewModel.onShowError()
        }
    }
}