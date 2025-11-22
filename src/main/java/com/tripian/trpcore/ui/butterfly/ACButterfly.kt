package com.tripian.trpcore.ui.butterfly

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcButterflyBinding
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.util.extensions.observe

//import kotlinx.android.synthetic.main.ac_butterfly.*

/**
 * Created by semihozkoroglu on 30.08.2020.
 */
class ACButterfly : BaseActivity<AcButterflyBinding, ACButterflyVM>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(R.anim.anim_slide_in_up, R.anim.anim_slide_out_up)

        binding.rvList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    override fun setListeners() {
        binding.imClose.setOnClickListener { viewModel.onClickedClose() }
        binding.imCloseWarning.setOnClickListener { viewModel.onClickedCloseWarning() }
    }

    override fun setReceivers() {
        observe(viewModel.onSetAdapterListener) {
            if (binding.rvList.adapter == null) {
                binding.rvList.adapter = object : AdapterButterfly(this, it!!) {
                    override fun onClickedItem(item: ButterflyItem) {
                        viewModel.onClickedItem(item)
                    }

                    override fun onClickedLike(item: ButterflyItem) {
                        viewModel.onClickedLike(item)
                    }

                    override fun onClickedDislike(item: ButterflyItem) {
                        viewModel.onClickedDislike(item)
                    }

                    override fun onClickedUndo(item: ButterflyItem) {
                        viewModel.onClickedUndo(item)
                    }

                    override fun onClickedTellUs(item: ButterflyItem) {
                        viewModel.onClickedTellUs(item)
                    }

                    override fun onClickedClose(item: ButterflyItem) {
                        viewModel.onClickedItemClose(item)
                    }
                }
            } else {
                binding.rvList.adapter?.notifyDataSetChanged()
            }
        }

        observe(viewModel.onCloseWarningListener) {
            binding.rlWarning.visibility = View.GONE
        }
    }

    override fun getViewBinding(): AcButterflyBinding {
        return AcButterflyBinding.inflate(layoutInflater)
    }
}