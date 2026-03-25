package com.tripian.trpcore.ui.onboarding

import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.util.Preferences
import javax.inject.Inject

class OnboardingVM @Inject constructor(
    private val preferences: Preferences
) : BaseViewModel() {

    /**
     * Checks if onboarding should be shown based on user preferences.
     *
     * Rules:
     * 1. If dismissedPermanently = true → DON'T SHOW
     * 2. If hasSeenOnboarding = false → SHOW (first time)
     * 3. If continueCount < 3 → SHOW
     * 4. Otherwise → DON'T SHOW
     */
    fun shouldShowOnboarding(): Boolean {
        if (preferences.getBoolean(Preferences.Keys.ONBOARDING_DISMISSED_PERMANENTLY, false)) {
            return false
        }
        if (!preferences.getBoolean(Preferences.Keys.ONBOARDING_HAS_SEEN, false)) {
            return true
        }
        return preferences.getInt(Preferences.Keys.ONBOARDING_CONTINUE_COUNT, 0) < 3
    }

    /**
     * Called when user taps "Continuar" button.
     * Marks onboarding as seen and increments continue count.
     */
    fun didTapContinue() {
        preferences.setBoolean(Preferences.Keys.ONBOARDING_HAS_SEEN, true)
        val count = preferences.getInt(Preferences.Keys.ONBOARDING_CONTINUE_COUNT, 0)
        preferences.setInt(Preferences.Keys.ONBOARDING_CONTINUE_COUNT, count + 1)
    }

    /**
     * Called when user taps "Omitir" or close (X) button.
     * Marks onboarding as permanently dismissed.
     */
    fun didTapDismiss() {
        preferences.setBoolean(Preferences.Keys.ONBOARDING_HAS_SEEN, true)
        preferences.setBoolean(Preferences.Keys.ONBOARDING_DISMISSED_PERMANENTLY, true)
    }

    /**
     * Resets onboarding state, allowing it to be shown again.
     */
    fun resetOnboarding() {
        preferences.setBoolean(Preferences.Keys.ONBOARDING_HAS_SEEN, false)
        preferences.setInt(Preferences.Keys.ONBOARDING_CONTINUE_COUNT, 0)
        preferences.setBoolean(Preferences.Keys.ONBOARDING_DISMISSED_PERMANENTLY, false)
    }
}
