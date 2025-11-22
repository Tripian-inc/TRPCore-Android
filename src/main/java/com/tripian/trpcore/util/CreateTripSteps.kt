package com.tripian.trpcore.util

enum class CreateTripSteps {
    DESTINATION {
        override fun getNextTitle() = TRAVELER_INFO.getCurrentTitle()
        override fun getPrevTitle() = ""
        override fun getCurrentTitle() = LanguageConst.DESTINATION
        override fun isPrevArrowVisible() = false
        override fun isNextArrowVisible() = true
        override fun nextButtonTitle() = LanguageConst.CONTINUE
    },
    TRAVELER_INFO {
        override fun getNextTitle() = ITINERARY_PROFILE.getCurrentTitle()
        override fun getPrevTitle() = DESTINATION.getCurrentTitle()
        override fun getCurrentTitle() = LanguageConst.TRAVELER_INFO
        override fun isPrevArrowVisible() = true
        override fun isNextArrowVisible() = true
        override fun nextButtonTitle() = LanguageConst.CONTINUE
    },
    ITINERARY_PROFILE {
        override fun getNextTitle() = PERSONAL_INTERESTS.getCurrentTitle()
        override fun getPrevTitle() = TRAVELER_INFO.getCurrentTitle()
        override fun getCurrentTitle() = LanguageConst.ITINERARY_PROFILE
        override fun isPrevArrowVisible() = true
        override fun isNextArrowVisible() = true
        override fun nextButtonTitle() = LanguageConst.CONTINUE
    },
    PERSONAL_INTERESTS {
        override fun getNextTitle() = ""
        override fun getPrevTitle() = ITINERARY_PROFILE.getCurrentTitle()
        override fun getCurrentTitle() = LanguageConst.PERSONAL_INTERESTS
        override fun isPrevArrowVisible() = true
        override fun isNextArrowVisible() = false
        override fun nextButtonTitle() = LanguageConst.CREATE_TRIP
    };

    abstract fun getNextTitle(): String
    abstract fun getPrevTitle(): String
    abstract fun getCurrentTitle(): String
    abstract fun isPrevArrowVisible(): Boolean
    abstract fun isNextArrowVisible(): Boolean
    abstract fun nextButtonTitle(): String
}

