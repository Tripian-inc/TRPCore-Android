package com.tripian.trpcore.ui.overview

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.Butterfly
import com.tripian.trpcore.domain.model.ButterflyItem
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterOverView(val context: Context, val items: List<Butterfly>) : RecyclerView.Adapter<AdapterOverView.ButterflyRoot>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(item: ButterflyItem)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButterflyRoot {
        return ButterflyRoot(inflater.inflate(R.layout.item_butterfly, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ButterflyRoot, position: Int) {
        val item = items[position]

        with(holder) {
            tvTitle.text = item.title

            if (!TextUtils.isEmpty(item.description)) {
                tvDescription.text = item.description
                tvDescription.visibility = View.VISIBLE
            } else {
                tvDescription.visibility = View.GONE
            }

            if (rvInnerList.adapter == null) {
                val adapter = object : AdapterOverViewItem(context, item.items) {
                    override fun onClickedItem(item: ButterflyItem) {
                        this@AdapterOverView.onClickedItem(item)
                    }
                }

                rvInnerList.adapter = adapter
            } else {
                rvInnerList.adapter?.notifyDataSetChanged()
            }
        }
    }

    class ButterflyRoot constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val tvDescription: TextView = vi.findViewById(R.id.tvDescription)
        val rvInnerList: RecyclerView = vi.findViewById(R.id.rvInnerList)

        init {
//            rvInnerList.isNestedScrollingEnabled
            rvInnerList.layoutManager = LinearLayoutManager(vi.context, RecyclerView.VERTICAL, false)
        }
    }
}