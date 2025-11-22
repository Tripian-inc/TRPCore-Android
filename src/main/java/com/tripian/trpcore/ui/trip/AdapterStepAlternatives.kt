package com.tripian.trpcore.ui.trip

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.util.extensions.toSmallUrl
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterStepAlternatives constructor(val context: Context, val items: List<MapStep>) : RecyclerView.Adapter<AdapterStepAlternatives.SelectDay>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(step: MapStep)

    abstract fun onClickedAlternatives(step: MapStep)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectDay {
        return SelectDay(inflater.inflate(R.layout.item_step_alternatives, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SelectDay, position: Int) {
        val item = items[position]

        with(holder) {
            tvTitle.text = item.name

            if (!TextUtils.isEmpty(item.image)) {
                Glide.with(context).load(item.image?.toSmallUrl())
                    .apply(RequestOptions().circleCrop())
                    .placeholder(ContextCompat.getDrawable(context, R.drawable.bg_place_holder_image))
                    .into(imPoi)
            }

            root.setOnClickListener { onClickedItem(item) }
        }
    }

    class SelectDay constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val root: RelativeLayout = vi.findViewById(R.id.root)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val imPoi: ImageView = vi.findViewById(R.id.imPoi)
    }
}