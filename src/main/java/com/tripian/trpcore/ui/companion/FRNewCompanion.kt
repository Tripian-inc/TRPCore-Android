package com.tripian.trpcore.ui.companion

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrNewCompanionBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 24.08.2020.
 */
class FRNewCompanion :
    BaseBottomDialogFragment<FrNewCompanionBinding, FRNewCompanionVM>(FrNewCompanionBinding::inflate) {

    private var adapterQuestions: AdapterCompanionQuestions? = null

    companion object {
        fun newInstance(companion: com.tripian.one.api.companion.model.Companion? = null): FRNewCompanion {
            val fragment = FRNewCompanion()

            if (companion != null) {
                val data = Bundle()
                data.putSerializable("companion", companion)

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

        binding.tvTitle.text = getLanguageForKey(LanguageConst.CREATE_COMPANION_PROFILE)
        binding.tvSubTitle.text = getLanguageForKey(LanguageConst.COMPANION_SUBTITLE)
        binding.tvCompanionTitle.text = getLanguageForKey(LanguageConst.TITLE)
        binding.cvFamily.text = getLanguageForKey(LanguageConst.FAMILY_MEMBER)
        binding.cvWork.text = getLanguageForKey(LanguageConst.WORK_COLLEAGUE)
        binding.cvFriend.text = getLanguageForKey(LanguageConst.FRIEND)
        binding.tvName.text = getLanguageForKey(LanguageConst.NAME)
        binding.tvAge.text = getLanguageForKey(LanguageConst.AGE)
        binding.btnApply.text = getLanguageForKey(LanguageConst.CREATE_NEW)

        binding.etName.hint = getLanguageForKey(LanguageConst.TYPE_NAME)
        binding.etAge.hint = getLanguageForKey(LanguageConst.TYPE_AGE)


        binding.btnApply.setOnClickListener {
            viewModel.onClickedCreate(
                binding.etName.text.toString(),
                binding.etAge.text.toString(),
                adapterQuestions?.getSelectedItems()
            )
        }

        // 0
        binding.cvFamily.setOnCheckListener {
            binding.cvWork.check(false, true)
            binding.cvFriend.check(false, true)

            viewModel.onTitleSelected(0, it)
        }
        // 1
        binding.cvWork.setOnCheckListener {
            binding.cvFamily.check(false, true)
            binding.cvFriend.check(false, true)

            viewModel.onTitleSelected(1, it)
        }
        // 2
        binding.cvFriend.setOnCheckListener {
            binding.cvFamily.check(false, true)
            binding.cvWork.check(false, true)

            viewModel.onTitleSelected(2, it)
        }
//        btnDelete.setOnClickListener { viewModel.onClickedDelete() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetNameListener) {
            binding.etName.setText(it)
        }

        observe(viewModel.onSetAgeListener) {
            binding.etAge.setText(it)
        }

        observe(viewModel.onShowDeleteListener) {
//            btnCreate.text = getString(R.string.confirm)
//            btnDelete.visibility = View.VISIBLE
        }

        observe(viewModel.onSetQuestionsListener) {
            adapterQuestions = object : AdapterCompanionQuestions(requireContext(), it!!.first, it.second) {
                override fun notified() {
                }
            }

            binding.rvList.adapter = adapterQuestions
            binding.rvList.visibility = View.VISIBLE
        }

        observe(viewModel.onShowQuestionLoading) {
            binding.pbProgress.visibility = if (it!!) View.VISIBLE else View.GONE
        }

        observe(viewModel.onSetTitleListener) {
            when (it) {
                0 -> {
                    binding.cvFamily.check(true)
                }
                1 -> {
                    binding.cvWork.check(true)
                }
                2 -> {
                    binding.cvFriend.check(true)
                }
            }
        }
    }
}