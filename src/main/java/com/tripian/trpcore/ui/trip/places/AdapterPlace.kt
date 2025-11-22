package com.tripian.trpcore.ui.trip.places

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.toSmallUrl
import com.tripian.trpcore.util.widget.ImageView
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterPlace(
    val context: Context,
    val items: List<PlaceItem>,
    private val miscRepository: MiscRepository
) : RecyclerView.Adapter<AdapterPlace.Place>() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedPlace(place: PlaceItem)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Place {
        return Place(inflater.inflate(R.layout.item_place, null))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: Place, position: Int) {
        val item = items[position]

        with(holder) {
            tvTitle.text = item.title

            if (!TextUtils.isEmpty(item.image)) {
                Glide.with(context).load(item.image?.toSmallUrl())
                    .apply(RequestOptions().circleCrop())
                    .placeholder(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.bg_place_holder_image
                        )
                    )
                    .into(imPoi)
            }

            llBottom.visibility = View.VISIBLE
            imCheck.visibility = View.VISIBLE

            if (!TextUtils.isEmpty(item.match)) {

                val matchText =
                    "${item.match}% " + miscRepository.getLanguageValueForKey(LanguageConst.MATCH)
                tvMatch.text = matchText
                tvMatch.visibility = View.VISIBLE
            } else {
                tvMatch.visibility = View.GONE
            }

            if (item.partOfDays.size > 0) {
                val partOfDay = item.partOfDays.joinToString(separator = ", ")
                val partOfDayText =
                    "${miscRepository.getLanguageValueForKey(LanguageConst.PART_OF_DAY)} - $partOfDay"
                tvPartOfDay.text = partOfDayText
                tvPartOfDay.visibility = View.VISIBLE

                if (TextUtils.isEmpty(item.match)) {
                    tvSplitter.visibility = View.GONE
                } else {
                    tvSplitter.visibility = View.VISIBLE
                }
            } else {
                tvPartOfDay.visibility = View.GONE
                tvSplitter.visibility = View.GONE

                if (tvMatch.visibility == View.GONE) {
                    llBottom.visibility = View.GONE
                    imCheck.visibility = View.GONE
                }
            }

            if (TextUtils.isEmpty(item.category)) {
                tvCategory.visibility = View.GONE
            } else {
                tvCategory.visibility = View.VISIBLE
                tvCategory.text = item.category
            }

            root.setOnClickListener { onClickedPlace(item) }
        }
    }

    class Place(vi: View) : RecyclerView.ViewHolder(vi) {
        val root: RelativeLayout = vi.findViewById(R.id.root)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val imPoi: ImageView = vi.findViewById(R.id.imPoi)
        val tvMatch: TextView = vi.findViewById(R.id.tvMatch)
        val imCheck: ImageView = vi.findViewById(R.id.imCheck)
        val tvSplitter: TextView = vi.findViewById(R.id.tvSplitter)
        val tvPartOfDay: TextView = vi.findViewById(R.id.tvPartOfDay)
        val llBottom: LinearLayout = vi.findViewById(R.id.llBottom)
        val tvCategory: TextView = vi.findViewById(R.id.tvCategory)
    }
}