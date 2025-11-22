package com.tripian.trpcore.ui.trip.places

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
import com.tripian.trpcore.util.extensions.toSmallUrl
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView
import com.tripian.one.api.pois.model.Taste

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterMustTry constructor(val context: Context, val items: List<Taste>) : RecyclerView.Adapter<AdapterMustTry.Place>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedTaste(taste: Taste)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Place {
        return Place(inflater.inflate(R.layout.item_must_try, null))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: Place, position: Int) {
        val item = items[position]

        with(holder) {
            tvTitle.text = item.name

            if (!TextUtils.isEmpty(item.image?.url)) {
                Glide.with(context).load(item.image?.url?.toSmallUrl())
                    .apply(RequestOptions().circleCrop())
                    .placeholder(ContextCompat.getDrawable(context, R.drawable.bg_place_holder_image))
                    .into(imPoi)
            }

            root.setOnClickListener { onClickedTaste(item) }
        }
    }

    class Place constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val root: RelativeLayout = vi.findViewById(R.id.root)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val imPoi: ImageView = vi.findViewById(R.id.imPoi)
    }
}