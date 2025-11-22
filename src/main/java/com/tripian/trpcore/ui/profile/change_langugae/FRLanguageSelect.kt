package com.tripian.trpcore.ui.profile.change_langugae

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrLanguageSelectBinding
import com.tripian.trpcore.util.extensions.observe

class FRLanguageSelect :
    BaseBottomDialogFragment<FrLanguageSelectBinding, FRLanguageSelectVM>(FrLanguageSelectBinding::inflate) {

    companion object {
        fun newInstance(): FRLanguageSelect {
            return FRLanguageSelect()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetLanguagesListener) {
            it?.let {
                binding.rvList.adapter = object : AdapterSelectLanguage(requireContext(), it) {
                    override fun onClickedItem(langCode: Pair<String, String>) {
                        viewModel.setLanguage(langCode)
                    }
                }
            }
        }

        observe(viewModel.onDismissListener) {
            dismiss()
        }
    }
}