package com.tripian.trpcore.repository

import android.app.Application
import com.google.gson.Gson
import com.tripian.one.api.users.model.LightLoginRequest
import com.tripian.one.api.users.model.LoginResponse
import com.tripian.one.api.users.model.User
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.base.awaitCallback
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.Strings
import javax.inject.Inject

class TripianUserRepository @Inject constructor(
    var app: Application,
    private val pref: Preferences,
    val gson: Gson,
    val strings: Strings
) {

    var user: User? = null

    /**
     * Light-login via TRPRest's callback API. Used by [DoLightLogin] to grab
     * a guest-style session for the timeline flow.
     */
    suspend fun lightLoginAsync(
        uniqueId: String,
        firstName: String?,
        lastName: String?
    ): LoginResponse = awaitCallback { ok, fail ->
        TRPCore.core.trpRest.lightLogin(
            request = LightLoginRequest().apply {
                this.uniqueId = uniqueId
                this.firstName = firstName
                this.lastName = lastName
            },
            success = ok,
            error = fail
        )
    }

    /**
     * Local-only logout: clears persisted SDK state. Wired up from
     * [com.tripian.trpcore.base.ApiErrorMapper] on a refresh-token failure so
     * the host can re-init the SDK and grab a fresh session.
     */
    fun logout() {
        pref.clearAllData()
        user = null
    }
}
