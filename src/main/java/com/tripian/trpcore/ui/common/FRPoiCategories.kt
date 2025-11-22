package com.tripian.trpcore.ui.common

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.pois.model.PoiCategory
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrPoiCategoriesBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe
import java.io.Serializable

/**
 * Created by cemcaygoz on 24.08.2025.
 */
class FRPoiCategories :
    BaseBottomDialogFragment<FrPoiCategoriesBinding, FRPoiCategoriesVM>(FrPoiCategoriesBinding::inflate) {

    var adapter: AdapterPoiCategories? = null
    lateinit var onSelectItemIds: (List<PoiCategory>) -> Unit
    var onAllItemIds: ((List<PoiCategory>) -> Unit)? = null
    var onClosed: (() -> Unit)? = null

    override fun isDragEnable(): Boolean {
        return false
    }

    override fun isFullscreen(): Boolean {
        return true
    }

    companion object {
        fun newInstance(
            selectedItemIds: List<PoiCategory>?,
            onSelectItemIds: (List<PoiCategory>) -> Unit,
            onAllItemIds: ((List<PoiCategory>) -> Unit)? = null,
            onClosed: (() -> Unit)? = null
        ): FRPoiCategories {
            val fragment = FRPoiCategories()
            fragment.arguments = Bundle().apply {
                selectedItemIds?.let {
                    putSerializable("selectedCategories", it as Serializable)
                }
            }
            fragment.onSelectItemIds = onSelectItemIds
            fragment.onClosed = onClosed
            fragment.onAllItemIds = onAllItemIds

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvError.text = viewModel.getLanguageForKey(LanguageConst.PLEASE_SELECT_CATEGORY)
        binding.btnCancel.text = viewModel.getLanguageForKey(LanguageConst.CANCEL)
        binding.btnSelect.text = viewModel.getLanguageForKey(LanguageConst.SELECT_CATEGORY)
        binding.rvList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)


        binding.btnSelect.setOnClickListener {
            val selectedItems = adapter?.getSelectedItems().orEmpty()
            if (selectedItems.isEmpty()) {
                onAllItemIds?.invoke(viewModel.getAllPoiCategories())
            } else {
                onSelectItemIds.invoke(selectedItems)
            }
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            if (viewModel.selectedCategories.isEmpty()) {
                onAllItemIds?.invoke(viewModel.getAllPoiCategories())
            } else {
                onClosed?.invoke()
            }
            dismiss()
        }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onPoiCategoriesListener) {
            if (it.isNullOrEmpty()) {
                binding.rvList.visibility = View.GONE
                binding.llError.visibility = View.VISIBLE
            } else {
                binding.rvList.visibility = View.VISIBLE
                binding.llError.visibility = View.GONE

                adapter = object : AdapterPoiCategories(
                    context = requireContext(),
                    items = it,
                    selectedCategories = viewModel.selectedCategories
                ) {}

                binding.rvList.adapter = adapter
            }
        }
    }
}