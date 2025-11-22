package com.tripian.trpcore.ui.trip_detail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.util.extensions.toLargeUrl
import com.tripian.trpcore.util.widget.ImageView


/**
 * Created by semihozkoroglu on 2020-03-07.
 */
class AdapterImages(val context: Context, val items: List<String>) : PagerAdapter() {

    var inflater: LayoutInflater = LayoutInflater.from(context)

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
        container.removeView(view as View)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val vi = inflater.inflate(R.layout.item_images, container, false) as ImageView

        Glide.with(context).load(items[position].toLargeUrl())
            .apply(RequestOptions().transform(CenterCrop()))
            .into(vi)

        container.addView(vi)
        return vi
    }
}