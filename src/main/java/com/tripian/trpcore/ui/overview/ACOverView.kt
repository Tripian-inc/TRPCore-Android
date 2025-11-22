package com.tripian.trpcore.ui.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcOverviewBinding
import com.tripian.trpcore.domain.model.Butterfly
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 30.08.2020.
 */
class ACOverView : BaseActivity<AcOverviewBinding, ACOverViewVM>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.rvList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    override fun getViewBinding(): AcOverviewBinding {
        return AcOverviewBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.imNavigation.setOnClickListener { viewModel.onClickedClose() }
        binding.btnDone.setOnClickListener { viewModel.onClickedClose() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab) {
            }

            override fun onTabUnselected(p0: TabLayout.Tab) {
                p0.customView?.findViewById<TextView>(R.id.tvHeader)
                    ?.setTextColor(ContextCompat.getColor(this@ACOverView, R.color.body))
            }

            override fun onTabSelected(p0: TabLayout.Tab) {
                viewModel.onTabSelected(p0.position)

                p0.customView?.findViewById<TextView>(R.id.tvHeader)
                    ?.setTextColor(ContextCompat.getColor(this@ACOverView, R.color.head))
            }
        })
    }

    override fun setReceivers() {
        observe(viewModel.onSetItemsListener) {
            binding.rvList.adapter = object : AdapterOverViewItem(this, it!!) {
                override fun onClickedItem(item: ButterflyItem) {
                    viewModel.onClickedItem(item)
                }
            }
        }

        observe(viewModel.onSetTabItemListener) {
            addTabs(it!!)
        }

        observe(viewModel.onProgressListener) {
            binding.pbProgress.visibility = if (it!!) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private fun addTabs(items: List<Butterfly>) {
        if (items.size <= binding.tabLayout.tabCount) {
            return
        } else {
            for (i in binding.tabLayout.tabCount until items.size) {
                val tab = binding.tabLayout.newTab()

                tab.customView = LayoutInflater.from(this).inflate(R.layout.item_tab_overview_custom, null)
                tab.customView?.findViewById<TextView>(R.id.tvHeader)?.let {
                    it.text = items[i].title
                }
                tab.customView?.findViewById<TextView>(R.id.tvDate)?.let {
                    it.text = items[i].date
                }

                binding.tabLayout.addTab(tab, false)

                if (i == 0) {
                    tab.select()
                }
            }
        }
    }
}