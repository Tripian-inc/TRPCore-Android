package com.tripian.trpcore.ui.splash

import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcSplashBinding

/**
 * Created by semihozkoroglu on 18.04.2021.
 */
class ACSplash : BaseActivity<AcSplashBinding, ACSplashVM>() {

    override fun getViewBinding(): AcSplashBinding {
        return AcSplashBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.tvTradeMark.text = "Feel Like A Local Wherever You Go" //getLanguageForKey(LanguageConst.TRADEMARK)
    }

    override fun setReceivers() {
    }
}