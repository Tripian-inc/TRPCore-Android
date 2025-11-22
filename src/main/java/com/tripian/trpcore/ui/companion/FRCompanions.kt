package com.tripian.trpcore.ui.companion

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrCompanionsBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe
import java.io.Serializable

/**
 * Created by semihozkoroglu on 24.08.2020.
 */
class FRCompanions :
    BaseBottomDialogFragment<FrCompanionsBinding, FRCompanionsVM>(FrCompanionsBinding::inflate) {

    var adapter: AdapterSelectCompanion? = null
    lateinit var onCreateCompanion: () -> Unit

    companion object {
        fun newInstance(
            selectedCompanions: List<com.tripian.one.api.companion.model.Companion>?,
            mode: CompanionMode,
            onCreateCompanion: () -> Unit
        ): FRCompanions {
            val fragment = FRCompanions()
            fragment.arguments = Bundle().apply {
                selectedCompanions?.let {
                    putSerializable("companions", it as Serializable)
                }

                putSerializable("mode", mode)
            }
            fragment.onCreateCompanion = onCreateCompanion

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvError.text = viewModel.getLanguageForKey(LanguageConst.NO_COMPANION)
        binding.tvAddTraveller.text = viewModel.getLanguageForKey(LanguageConst.ADD_TRAVELERS)
        binding.tvCreateTraveller.text = viewModel.getLanguageForKey(LanguageConst.CREATE_NEW)
        binding.rvList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        binding.llError.setOnClickListener { viewModel.onCreateCompanion() }


        binding.cvCreateTraveller.setOnClickListener {
            onCreateCompanion.invoke()
        }

        binding.cvAddTraveller.setOnClickListener {
            viewModel.onApplySelected(
                if (binding.rvList.adapter != null) {
                    (binding.rvList.adapter as AdapterSelectCompanion).getSelectedItems()
                } else {
                    ArrayList()
                }
            )
        }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetCompanionsListener) {
            if (it!!.second.isEmpty()) {
                binding.rvList.visibility = View.GONE
                binding.llError.visibility = View.VISIBLE
            } else {
                binding.rvList.visibility = View.VISIBLE
                binding.llError.visibility = View.GONE

                adapter = object : AdapterSelectCompanion(requireContext(), ArrayList(it.second)) {
                    override fun onClickedItem() {
                        binding.cvAddTraveller.isVisible =
                            adapter?.getSelectedItems()?.isNotEmpty() ?: false
//                        viewModel.onClickedItem(companion)
                    }

                }
                adapter!!.setSelectedItems(it.first)

                binding.rvList.adapter = adapter
            }
        }
    }
}