package com.tripian.trpcore.ui.splash

import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcSplashBinding
import com.tripian.trpcore.ui.onboarding.OnboardingBottomSheet

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
        android.util.Log.d("ONBOARDING_DEBUG", "ACSplash.setReceivers() called, setting up observer")
        viewModel.showOnboarding.observe(this) { shouldShow ->
            android.util.Log.d("ONBOARDING_DEBUG", "ACSplash observer triggered, shouldShow=$shouldShow")
            if (shouldShow) {
                showOnboardingBottomSheet()
            }
        }
    }

    private fun showOnboardingBottomSheet() {
        android.util.Log.d("ONBOARDING_DEBUG", "showOnboardingBottomSheet() called")
        val bottomSheet = OnboardingBottomSheet.newInstance()
        bottomSheet.setOnCompleteListener {
            android.util.Log.d("ONBOARDING_DEBUG", "Onboarding completed, navigating to MyTrip")
            viewModel.onOnboardingComplete()
        }
        bottomSheet.show(supportFragmentManager, "onboarding")
        android.util.Log.d("ONBOARDING_DEBUG", "BottomSheet.show() called")
    }
}