package com.tripian.trpcore.ui.profile.change_langugae

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.ui.splash.ACSplash
import javax.inject.Inject

class FRLanguageSelectVM @Inject constructor() : BaseViewModel() {

    var onSetLanguagesListener = MutableLiveData<List<Pair<String, String>>>()
    var onDismissListener = MutableLiveData<Unit>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        setLanguages()
    }

    private fun setLanguages() {
        onSetLanguagesListener.postValue(miscRepository.languageCodes)
    }

    fun setLanguage(langCode: Pair<String, String>) {
        miscRepository.changeLanguage(langCode.first)
        startActivity(
            ACSplash::class,
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )

        finishActivity()
    }
}
