package com.tripian.trpcore.ui.trip.favorite

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcFavoriteBinding
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.ui.trip.places.AdapterPlace
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 3.10.2020.
 */
class ACFavorite : BaseActivity<AcFavoriteBinding, ACFavoriteVM>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.anim_slide_in_up,
                R.anim.anim_slide_out_up
            )
        } else {
            overridePendingTransition(R.anim.anim_slide_in_up, R.anim.anim_slide_out_up)
        }

        binding.rvList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    override fun onPause() {
        super.onPause()

        overridePendingTransition(R.anim.anim_slide_in_down, R.anim.anim_slide_out_down)
    }

    override fun getViewBinding(): AcFavoriteBinding {
        return AcFavoriteBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.imNavigation.setOnClickListener { viewModel.onClickedBack() }
        binding.tvTitle.text = getLanguageForKey(LanguageConst.FAVORITES)
        binding.tvError.text = getLanguageForKey(LanguageConst.NO_FAVORITES)
    }

    override fun setReceivers() {
        observe(viewModel.onSetPlaceListener) {
            if (it.isNullOrEmpty()) {
                binding.tvError.visibility = View.VISIBLE
                binding.rvList.visibility = View.GONE
            } else {
                binding.tvError.visibility = View.GONE
                binding.rvList.visibility = View.VISIBLE

                binding.rvList.adapter = object : AdapterPlace(this, it, viewModel.miscRepository) {
                    override fun onClickedPlace(place: PlaceItem) {
                        viewModel.onClickedPlace(place)
                    }
                }
            }
        }
    }
}