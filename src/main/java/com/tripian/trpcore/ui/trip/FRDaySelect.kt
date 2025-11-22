package com.tripian.trpcore.ui.trip

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrDaySelectionBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 23.08.2020.
 */
class FRDaySelect : BaseBottomDialogFragment<FrDaySelectionBinding, FRDaySelectVM>(FrDaySelectionBinding::inflate) {

    companion object {
        fun newInstance(): FRDaySelect {
            return FRDaySelect()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvDays.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setListeners() {
        super.setListeners()
        binding.tvTitle.text = viewModel.getLanguageForKey(LanguageConst.CHANGE_DAY)
        binding.btnCancel.text = viewModel.getLanguageForKey(LanguageConst.CANCEL)
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetDaysListener) {
            val adapter = object : AdapterSelectDay(requireContext(), it!!) {
                override fun onClickedItem(position: Int) {
                    viewModel.onClickedItem(position)
                }
            }

            binding.rvDays.adapter = adapter
        }

        observe(viewModel.onDismissListener) {
            binding.rvDays.postDelayed({ dismiss() }, 500)
        }
    }
}