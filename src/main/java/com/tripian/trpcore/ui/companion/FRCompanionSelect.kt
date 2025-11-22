package com.tripian.trpcore.ui.companion

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseDialogFragment
import com.tripian.trpcore.databinding.FrCompanionSelectBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe
import java.io.Serializable

/**
 * Created by semihozkoroglu on 23.08.2020.
 */
class FRCompanionSelect :
    BaseDialogFragment<FrCompanionSelectBinding, FRCompanionSelectVM>(FrCompanionSelectBinding::inflate) {

    companion object {
        fun newInstance(companions: List<com.tripian.one.api.companion.model.Companion>? = null): FRCompanionSelect {
            val fragment = FRCompanionSelect()

            if (!companions.isNullOrEmpty()) {
                val data = Bundle()
                data.putSerializable("companions", companions as Serializable)

                fragment.arguments = data
            }

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvTitleCompanion.text = getLanguageForKey(LanguageConst.TRAVEL_COMPANIONS)
        binding.tvTitleCompanion.text = getLanguageForKey(LanguageConst.TRAVEL_COMPANIONS)
        binding.imBack.setOnClickListener { dismiss() }
        binding.imOk.setOnClickListener {
            viewModel.onClickedOk(
                if (binding.rvList.adapter != null) {
                    (binding.rvList.adapter as AdapterSelectCompanion).getSelectedItems()
                } else {
                    ArrayList()
                }
            )
        }

        binding.tvManage.setOnClickListener { viewModel.onClickedManage() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onShowLoadingListener) {
            binding.pbProgress.visibility = View.VISIBLE
        }

        observe(viewModel.onHideLoadingListener) {
            binding.pbProgress.visibility = View.GONE
        }

        observe(viewModel.onSetCompanionsListener) {
            val adapter = object : AdapterSelectCompanion(requireContext(), ArrayList(it!!.first)) {
//                override fun onClickedItem(companion: com.tripian.one.api.companion.model.Companion) {
//                }

                override fun onClickedItem() {
                }
            }
            adapter.setSelectedItems(it.second)

            binding.rvList.adapter = adapter
        }
    }
}