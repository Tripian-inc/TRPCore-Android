package com.tripian.trpcore.ui.trip.my_offers

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.offers.model.Offer
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcMyOffersBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 3.10.2020.
 */
class ACMyOffers : BaseActivity<AcMyOffersBinding, ACMyOffersVM>() {
    private lateinit var items: ArrayList<Offer>

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

    override fun setListeners() {
        binding.imNavigation.setOnClickListener { viewModel.onClickedBack() }
        binding.tvTitle.text = getLanguageForKey(LanguageConst.MY_OFFERS)
        binding.tvError.text = getLanguageForKey(LanguageConst.MY_OFFERS_NO_OFFER)

    }

    override fun setReceivers() {
        observe(viewModel.onSetAdapterListener) {
            if (it.isNullOrEmpty()) {
                binding.tvError.visibility = View.VISIBLE
                binding.rvList.visibility = View.GONE
            } else {
                binding.tvError.visibility = View.GONE
                binding.rvList.visibility = View.VISIBLE

                this.items = it

                binding.rvList.adapter = object : AdapterOffers(
                    this,
                    items,
                    optOutEnable = true,
                    miscRepository = viewModel.miscRepository
                ) {
                    override fun addOffer(pos: Int, item: Offer, claimDate: String) {
                    }

                    override fun removeOffer(pos: Int, item: Offer) {
                        viewModel.removeToOffer(pos, item)
                        items.remove(item)
                    }

                    override fun onItemClicked(item: Offer) {
                        viewModel.onOfferClicked(item)
                    }
                }
            }

//            rvList.adapter = object : AdapterPlace(this, it) {
//                override fun onClickedPlace(place: PlaceItem) {
//                    viewModel.onClickedPlace(place)
//                }
//            }


        }

        observe(viewModel.onNotifyAdapter) { pos ->
            if (::items.isInitialized) {
                binding.rvList.adapter?.notifyItemRemoved(pos!!)
            }
        }
    }

    override fun getViewBinding(): AcMyOffersBinding {
        return AcMyOffersBinding.inflate(layoutInflater)
    }
}