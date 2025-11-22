package com.tripian.trpcore.ui.trip_detail

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.mapbox.common.MapboxOptions
import com.tripian.gyg.domain.model.ExperiencesItem
import com.tripian.gyg.ui.AdapterExperiencesItem
import com.tripian.one.api.offers.model.Offer
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.AcTripDetailBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.getPriceSpannableString
import com.tripian.trpcore.util.extensions.observe
import com.tripian.trpcore.util.extensions.openCustomTabExt
import kotlinx.coroutines.launch

/**
 * Created by semihozkoroglu on 3.10.2020.
 */
class ACTripDetail : BaseActivity<AcTripDetailBinding, ACTripDetailVM>() {

    private var disableWindowAnimation = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        MapboxOptions.accessToken = TRPCore.mapBoxApiKey

        super.onCreate(savedInstanceState)

        viewModel.setLifecycleOwner(this)

        overridePendingTransition(R.anim.anim_slide_in_up, R.anim.anim_slide_out_up)

        binding.mapView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> binding.svScroll.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> binding.svScroll.requestDisallowInterceptTouchEvent(
                    false
                )
            }
            binding.mapView.onTouchEvent(event)
        }
    }

    override fun onPause() {
        super.onPause()

        if (!disableWindowAnimation) {
            overridePendingTransition(R.anim.anim_slide_in_down, R.anim.anim_slide_out_down)
        }
    }

    override fun setListeners() {
        binding.btnReservation.text = getLanguageForKey(LanguageConst.MAKE_RESERVATION)
        binding.tvGygTitle.text = getLanguageForKey(LanguageConst.BUY_TICKETS_TOURS)
        binding.llPhone.setOnClickListener {
            if (!TextUtils.isEmpty(binding.tvPhone.text.toString())) {
                disableWindowAnimation = true

                val intent = Intent(
                    Intent.ACTION_DIAL,
                    Uri.fromParts("tel", binding.tvPhone.text.toString(), null)
                )
                startActivity(intent)
            }
        }

        binding.llLink.setOnClickListener {
            if (!TextUtils.isEmpty(binding.tvLink.text.toString())) {
                disableWindowAnimation = true

                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    binding.tvLink.text.toString().toUri()
                )
                startActivity(browserIntent)
            }
        }

        binding.btnReservation.setOnClickListener {
            viewModel.onClickedReservation()
        }

        binding.imNavigation.setOnClickListener { viewModel.onClickedNavigation() }
        binding.llHours.setOnClickListener { viewModel.onClickedHours() }
        binding.imFavorite.setOnClickListener {
            binding.imFavorite.isClickable = false
            binding.imFavorite.postDelayed({
                binding.imFavorite.isClickable = true
            }, 1000)

            viewModel.onClickedFavorite()
        }
        binding.imAction.setOnClickListener {
            binding.imAction.isClickable = false
            binding.imAction.postDelayed({
                binding.imAction.isClickable = true
            }, 1000)

            viewModel.onClickedAction()
        }
        binding.imRoute.setOnClickListener { viewModel.onClickedRoute() }
        binding.imShare.setOnClickListener {
            viewModel.onClickedShare()
        }
        binding.tvOwner.setOnClickListener { viewModel.onClickedOwner(binding.vpPager.currentItem) }
        binding.llDescription.setOnClickListener {
            if (binding.tvDescription.maxLines == 2) {
                binding.tvDescription.maxLines = Integer.MAX_VALUE
                binding.imDescriptionArrow.visibility = View.GONE
            } else {
                binding.tvDescription.maxLines = 2
                binding.imDescriptionArrow.visibility = View.VISIBLE
            }
        }

        binding.vpPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                viewModel.onImageChanged(position)
            }
        })
    }

    override fun setReceivers() {
        observe(viewModel.onShareUrlListener) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, it)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        observe(viewModel.onHasReservationListener) {
            binding.btnReservation.isVisible = true

            val btnTextKey =
                if (it == true) LanguageConst.CANCEL_RESERVATION else LanguageConst.MAKE_RESERVATION
            binding.btnReservation.text = getLanguageForKey(btnTextKey)
        }

        observe(viewModel.openUrlListener) {
            openCustomTabExt(it!!)
        }

//        observe(viewModel.onShowReservationPageListener) { poiDetail ->
//            val i = Intent(this, ACProvider::class.java)
//            val bundle = Bundle()
//            bundle.putSerializable("poiDetail", poiDetail)
//
//            i.putExtras(bundle)
//
//            startActivityForResult(i, 1001)
//        }

//        observe(viewModel.onCancelReservationPageListener) { confirmationUrl ->
//            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(confirmationUrl))
//            startActivity(browserIntent)
//        }

        observe(viewModel.onSetOwnerListener) {
            if (TextUtils.isEmpty(it)) {
                binding.tvOwner.visibility = View.GONE
            } else {
                binding.tvOwner.visibility = View.VISIBLE
            }

            binding.tvOwner.text = it
        }

        observe(viewModel.onSetHourListener) {
            binding.tvHours.text = it?.hour
        }

        observe(viewModel.onSetImagePageListener) {
//            tvImageCount.text = it
        }

//        observe(viewModel.onEnableLocationListener) {
//            imRoute.visibility = View.VISIBLE
//        }

//        observe(viewModel.onButterflyModeDisableListener) {
//            imRoute.visibility = View.GONE
//            imAction.visibility = View.GONE
//        }

        observe(viewModel.onSetFavoriteListener) {
            if (it!!) {
                binding.imFavorite.setImageResource(R.drawable.ic_favorite_selected2)
            } else {
                binding.imFavorite.setImageResource(R.drawable.ic_favorite2)
            }
        }

        observe(viewModel.onSetIconListener) {
            when (it!!) {
                Mode.ADD -> binding.imAction.setImageResource(R.drawable.ic_plus)
                Mode.REMOVE -> binding.imAction.setImageResource(R.drawable.ic_minus)
                Mode.CHANGE -> binding.imAction.setImageResource(R.drawable.ic_change_black)
            }

            binding.imAction.visibility = View.VISIBLE
        }

        observe(viewModel.onShowGygListener) {
            if (it.isNullOrEmpty()) return@observe

            binding.llGyg.isVisible = true
            binding.rvGetYourGuides.adapter = object : AdapterExperiencesItem(this, it) {
                override fun onClickedItem(item: ExperiencesItem) {
                    viewModel.onClickedTour(item)
                }
            }
        }

        observe(viewModel.onSetPlaceDetailListener) {
            it?.let {
                with(it) {
                    binding.mapView.postDelayed({
                        it.mapStep?.let { mapStep ->
                            binding.mapView.visibility = View.VISIBLE
                            binding.mapView.showMapIcons(arrayListOf(mapStep))
                            lifecycleScope.launch {
                                binding.mapView.moveCameraTo()
                            }
                        }
                    }, 500)

                    images?.let { ims ->
                        binding.vpPager.adapter =
                            AdapterImages(this@ACTripDetail, ims.map { it.url ?: "" })
                    }

                    binding.tvTitle.text = title
                    val gygDescText =
                        "${getLanguageForKey(LanguageConst.COVERING)} $title"
                    binding.tvGygDescription.text = gygDescText

                    if (price != -1) {
                        binding.tvPrice.visibility = View.VISIBLE
                        val str = getPriceSpannableString(
                            color = ContextCompat.getColor(
                                this@ACTripDetail,
                                R.color.head
                            ),
                            price = price
                        )
                        binding.tvPrice.text = str
                    } else {
                        binding.tvPrice.visibility = View.GONE
                    }

                    if (rating != -1f) {
                        val rateText =
                            getLanguageForKey(LanguageConst.GLOBAL_RATING) + ": (" + ratingCount + ")"
                        binding.tvRate.text = rateText
                        binding.rateBar.rating = rating

                        binding.tvRate.visibility = View.VISIBLE
                        binding.rateBar.visibility = View.VISIBLE
                    } else {
                        binding.tvRate.visibility = View.GONE
                        binding.rateBar.visibility = View.GONE
                    }

                    if (!TextUtils.isEmpty(match)) {
                        binding.tvMatch.visibility = View.VISIBLE

                        val matchText = "${match}% " + getLanguageForKey(LanguageConst.MATCH)
                        binding.tvMatch.text = matchText

                        if (partOfDay == 0) {
                            binding.tvPartOfDay.visibility = View.GONE
                        } else {
                            binding.tvPartOfDay.visibility = View.VISIBLE
                            val partOfDayText =
                                " - ${getLanguageForKey(LanguageConst.PART_OF_DAY)} $partOfDay"
                            binding.tvPartOfDay.text = partOfDayText
                        }
                    } else {
                        binding.tvMatch.visibility = View.GONE
                    }

                    if (!TextUtils.isEmpty(mustTry)) {
                        binding.tvMustTry.text = mustTry
                    } else {
                        binding.llMustTry.isVisible = false
                    }

                    if (!TextUtils.isEmpty(cuisines)) {
                        binding.tvCuisines.text = cuisines
                    } else {
                        binding.llCuisines.isVisible = false
                    }

                    if (!TextUtils.isEmpty(tags)) {
                        binding.tvTags.text = tags
                    } else {
                        binding.llTags.isVisible = false
                    }

                    if (hours.isNullOrEmpty()) {
                        binding.llHours.isVisible = false
                    }

                    if (!TextUtils.isEmpty(phone)) {
                        binding.tvPhone.text = phone
                    } else {
                        binding.llPhone.isVisible = false
                    }

                    if (!TextUtils.isEmpty(webSite)) {
                        binding.tvLink.text = webSite
                    } else {
                        binding.llLink.isVisible = false
                    }

                    if (!TextUtils.isEmpty(address)) {
                        binding.tvAddress.text = address
                    } else {
                        binding.llAddress.isVisible = false
                    }

                    if (!TextUtils.isEmpty(description)) {
                        binding.tvDescription.text = description
                    } else {
                        binding.llDescription.isVisible = false
                    }
                }
            }
        }


        observe(viewModel.onSetOffersListener) { offers ->

            if (!offers.isNullOrEmpty()) {
                binding.rvOffers.visibility = View.VISIBLE
                binding.rvOffers.adapter = object : AdapterOfferDetail(
                    context = this@ACTripDetail,
                    items = offers,
                    miscRepository = viewModel.miscRepository
                ) {
                    override fun onItemOptIn(item: Offer, claimDate: String) {
                        viewModel.onClickedOffer(item, claimDate)
                    }

                    override fun onItemRemoved(item: Offer) {
                        viewModel.onClickedRemoveOffer(item)
                    }
                }
            } else {
                binding.rvOffers.visibility = View.GONE
            }
        }
    }

    override fun getViewBinding(): AcTripDetailBinding {
        return AcTripDetailBinding.inflate(layoutInflater)
    }
}