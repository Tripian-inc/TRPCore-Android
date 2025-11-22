package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.trip.model.Answer
import com.tripian.one.api.trip.model.Question
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrAnswerSelectionBottomBinding
import com.tripian.trpcore.util.extensions.observe
import java.io.Serializable


/**
 * Created by semihozkoroglu on 21.08.2020.
 */
class FRAnswerSelectBottom :
    BaseBottomDialogFragment<FrAnswerSelectionBottomBinding, FRAnswerSelectBottomVM>(
        FrAnswerSelectionBottomBinding::inflate
    ) {

    companion object {
        fun newInstance(question: Question?): FRAnswerSelectBottom {
            val fragment = FRAnswerSelectBottom()

            val data = Bundle()
            data.putSerializable("question", question as Serializable)

            fragment.arguments = data

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAnswers.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetAnswersListener) {
            binding.rvAnswers.adapter = object : AdapterSelectAnswerBottom(requireContext(), it!!) {
                override fun onClickedItem(answer: Answer) {
                    viewModel.onClickedItem(answer)
                }
            }
        }

        observe(viewModel.onSetTitleListener) {
            binding.tvTitle.text = it
        }

        observe(viewModel.onDismissListener) {
            dismiss()
        }
    }
}