package com.tripian.trpcore.ui.trip.places

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.pois.model.Taste
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrPlacesBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.Navigation
import com.tripian.trpcore.util.ToolbarProperties
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 30.09.2020.
 */
class FRMustTry : BaseFragment<FrPlacesBinding, FRMustTryVM>(FrPlacesBinding::inflate) {

    private lateinit var layoutManager: LinearLayoutManager

    companion object {
        fun newInstance(): FRMustTry {
            return FRMustTry()
        }
    }

    override fun getToolbarProperties(): ToolbarProperties {
        return ToolbarProperties(
            title = getLanguageForKey(LanguageConst.PLACES),
            type = Navigation.CLOSE
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = layoutManager
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetTasteListener) {
            if (it.isNullOrEmpty()) {
                binding.tvError.visibility = View.VISIBLE
                binding.rvList.visibility = View.GONE

                binding.tvError.text = getLanguageForKey(LanguageConst.NO_RESULT_FOUND)
            } else {
                binding.tvError.visibility = View.GONE
                binding.rvList.visibility = View.VISIBLE

                binding.rvList.adapter = object : AdapterMustTry(requireContext(), it) {
                    override fun onClickedTaste(taste: Taste) {
                        viewModel.onClickedTaste(taste)
                    }
                }
            }
        }
    }
}