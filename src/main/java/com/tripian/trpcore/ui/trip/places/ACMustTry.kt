package com.tripian.trpcore.ui.trip.places

import android.os.Bundle
import android.text.TextUtils
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcMustTryBinding
import com.tripian.trpcore.domain.model.PlaceItem
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe
import com.tripian.trpcore.util.extensions.toLargeUrl

/**
 * Created by semihozkoroglu on 9.11.2020.
 */
class ACMustTry : BaseActivity<AcMustTryBinding, ACMustTryVM>() {

    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        super.onCreate(savedInstanceState)

        overridePendingTransition(R.anim.anim_slide_in_up, R.anim.anim_slide_out_up)

        binding.rvList.layoutManager = layoutManager
    }

    override fun onPause() {
        super.onPause()

        overridePendingTransition(R.anim.anim_slide_in_down, R.anim.anim_slide_out_down)
    }

    override fun getViewBinding(): AcMustTryBinding {
        return AcMustTryBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.imNavigation.setOnClickListener { onBackPressed() }
        binding.tvWhereToTry.text = viewModel.getLanguageForKey(LanguageConst.WHERE_TO_TRY)
    }

    override fun setReceivers() {
        observe(viewModel.onSetTasteListener) {
            if (!TextUtils.isEmpty(it?.image?.url)) {
                Glide.with(this).load(it?.image?.url?.toLargeUrl())
                    .apply(RequestOptions().centerCrop())
                    .into(binding.imImage)
            }

            binding.tvName.text = it?.name
            binding.tvDescription.text = it?.description

            binding.rlTitle.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.rlTitle.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    binding.collapsingToolbar.post {
                        binding.collapsingToolbar.minimumHeight = binding.rlTitle.height
                    }

                    binding.tvName.post {
                        val ownerParams = binding.tvName.layoutParams as FrameLayout.LayoutParams
                        ownerParams.bottomMargin = binding.rlTitle.height
                        binding.tvName.layoutParams = ownerParams
                    }
                }
            })
        }

        observe(viewModel.onSetPoiListListener) {
            if (binding.rvList.adapter == null) {
                binding.rvList.adapter =
                    object : AdapterPlace(this, it!!, viewModel.miscRepository) {
                        override fun onClickedPlace(place: PlaceItem) {
                            viewModel.onClickedPlace(place)
                        }
                    }
            } else {
                binding.rvList.adapter?.notifyDataSetChanged()
            }
        }
    }
}