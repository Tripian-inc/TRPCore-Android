package com.tripian.trpcore.ui.splash

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.CheckCognito
import com.tripian.trpcore.domain.DoGuestLogin
import com.tripian.trpcore.domain.DoLightLogin
import com.tripian.trpcore.domain.DoLogin
import com.tripian.trpcore.domain.DoRegister
import com.tripian.trpcore.domain.FetchConfigList
import com.tripian.trpcore.domain.FetchLanguages
import com.tripian.trpcore.domain.GetCities
import com.tripian.trpcore.domain.GetUser
import com.tripian.trpcore.domain.LogoutUser
import com.tripian.trpcore.domain.SaveAppLanguage
import com.tripian.trpcore.repository.FavoriteRepository
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.QuestionRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.repository.UserReactionRepository
import com.tripian.trpcore.ui.login.ACLogin
import com.tripian.trpcore.ui.mytrip.ACMyTrip
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.appLanguage
import com.tripian.trpcore.util.extensions.hideLoading
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class ACSplashVM @Inject constructor(
    val getUser: GetUser,
    val checkCognito: CheckCognito,
    val login: DoLogin,
    private val doGuestLogin: DoGuestLogin,
    private val doLightLogin: DoLightLogin,
    val register: DoRegister,
    val logoutUser: LogoutUser,
    private val saveAppLanguage: SaveAppLanguage,
    private val fetchLanguages: FetchLanguages,
    private val fetchConfigList: FetchConfigList,
    private val getCities: GetCities,
    private val tripRepository: TripRepository,
    private val questionRepository: QuestionRepository,
    val poiRepository: PoiRepository,
    private val favoriteRepository: FavoriteRepository,
    private val userReactionRepository: UserReactionRepository,
    val preferences: Preferences
) : BaseViewModel(getUser) {

    var startTime = 0L
    var endTime = 0L

    private var isCitiesFetched = false
    private var isLanguageFetched = false
    private var isLogon = false
    private var goLogin = false
    private var goHome = false

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        startTime = System.currentTimeMillis()

        val language = arguments?.getString("appLanguage")
        appLanguage = if (language.isNullOrEmpty().not()) {
            language
        } else {
            preferences.getString(
                Preferences.Keys.APP_LANGUAGE,
                "en"
            )
        }

        // SDK Initialization order:
        // 1. Fetch cities
        // 2. Fetch languages
        // 3. LightLogin with uniqueId
        fetchConfigList.on()
        fetchCities()
        fetchLanguages()
        login()
    }

    private fun fetchCities() {
        getCities.on(success = {
            isCitiesFetched = true
            checkAllMissionsCompleted()
        }, error = {
            // Even if cities fail, continue with other initialization
            isCitiesFetched = true
            checkAllMissionsCompleted()
        })
    }

    private fun login() {
        val email = arguments?.getString("email")
        val uniqueId = arguments?.getString("uniqueId")

        val processUniqueId =
            if (email.isNullOrEmpty().not()) email else uniqueId

        doLightLogin.on(DoLightLogin.Params(uniqueId = processUniqueId), success = {
            isLogon = true
            startHome()
        }, error = {
            guestLogin()
        })
    }

    private fun guestLogin() {
        doGuestLogin.on(success = {
            isLogon = true
            startHome()
        }, error = {
            hideLoading()
            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
            finishActivity()
        })
    }

    private fun checkAllMissionsCompleted() {
        endTime = System.currentTimeMillis()

        val time = ((endTime - startTime) / 1000)
        if (time <= 2) {
            viewModelScope.launch {
                delay(500L)
                checkAllMissionsCompleted()
            }
            return
        }
        // Wait for all: cities, languages, and login
        if (isLogon.not() || isLanguageFetched.not() || isCitiesFetched.not()) {
            return
        }
        if (goLogin) startLogin()
        if (goHome) startHome()
    }

    private fun startHome() {
        // Wait for all required data: cities, languages
        if (isLanguageFetched.not() || isCitiesFetched.not()) return
        val data = Bundle()
        clearCache()

        arguments?.let {
            if (it.containsKey("poiId")) {
                data.putInt("poiId", it.get("poiId").toString().toInt())

                if (it.containsKey("offerId")) {
                    data.putLong("offerId", it.get("offerId").toString().toLong())
                }
            }
        }

        startActivity(ACMyTrip::class, data)
        finishActivity()
    }

    private fun fetchLanguages() {
        saveAppLanguage.on(
            SaveAppLanguage.Params(
                appLanguage
            )
        )
        fetchLanguages.on(
            finally = {
                isLanguageFetched = true
                checkAllMissionsCompleted()
            }
        )
    }

//    private fun refreshToken() {
//        viewModelScope.launch {
//            val resToken = TokenManager.refreshToken()
//            isTokenRefreshed = true
//
//            if (resToken.isSuccess) {
//                TokenManager.token = resToken.getOrNull()?.data
//                checkAllMissionsCompleted()
//            } else {
//                logoutUser.on(
//                    finally = {
//                        goLogin = true
//                        checkAllMissionsCompleted()
//                    }
//                )
//            }
//        }
//    }

    private fun startLogin() {
        startActivity(ACLogin::class)
        finishActivity()
    }

    private fun clearCache() {
        tripRepository.clearItems()
        questionRepository.clearItems()
        poiRepository.clearItems()
        favoriteRepository.clearItems()
        userReactionRepository.clearItems()
    }
}
