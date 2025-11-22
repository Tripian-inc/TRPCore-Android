package com.tripian.trpcore.repository.authorization

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.tripian.aws_auth.Auth
import com.tripian.aws_auth.AuthUserSession
import com.tripian.aws_auth.handlers.AuthHandler
import com.tripian.one.TokenManager
import com.tripian.one.api.users.model.Token
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.Preferences
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class AwsAuthorization @Inject constructor(
    val applicationContext: Context,
    val preferences: Preferences
) {

    enum class Provider(val identity: String) {
        GOOGLE("Google"), FACEBOOK("Facebook")
    }

    private var auth: Auth? = null
    private var authSession: AuthUserSession? = null
        set(value) {
            TokenManager.tokenReceived(Token().apply {
                idToken = value?.idToken?.jwtToken
                expiresIn = if (value?.accessToken?.expiration?.time != null) {
                    (value.accessToken?.expiration!!.time - System.currentTimeMillis()).toInt()
                } else {
                    3600
                }
                tokenType = "Bearer"
                refreshToken = value?.refreshToken?.token
            })

            field = value
        }

    fun getAuthorizationToken(): String? {
        return authSession?.idToken?.jwtToken
    }

    fun getSession(provider: Provider): Observable<AuthUserSession> {
        return PublishSubject.create {
            val builder = getAuthBuilder(provider)
                .setAuthHandler(object : AuthHandler {
                    override fun onSuccess(session: AuthUserSession?) {
                        authSession = session

                        it.onNext(authSession!!)
                    }

                    override fun onSignout() {
                    }

                    override fun onFailure(e: Exception) {
                        it.onError(e)
                    }
                })

            auth = builder.build()

            auth?.getSessionWithoutWebUI()
        }
    }

    fun getToken(uri: Uri) {
        if (TRPCore.awsConfig.getRedirectLoginUrl().toUri().host == uri.host) {
            auth?.getTokens(uri)
        }
    }

    fun doLogin(activity: Activity, provider: Provider): Observable<AuthUserSession> {
        return PublishSubject.create {
            val builder = getAuthBuilder(provider)
                .setAuthHandler(object : AuthHandler {
                    override fun onSuccess(session: AuthUserSession?) {
                        authSession = session
                        preferences.setString(Preferences.Keys.SOCIAL_PROVIDER, provider.identity)

                        it.onNext(authSession!!)
                    }

                    override fun onSignout() {
                    }

                    override fun onFailure(e: Exception) {
                        it.onError(e)
                    }
                })

            auth = builder.build()

            auth?.getSession(activity)
        }
    }

    private fun getAuthBuilder(provider: Provider): Auth.Builder {
        val builder = Auth.Builder()
            .setAppClientId(TRPCore.awsConfig.getClientId())
            .setAppClientSecret(TRPCore.awsConfig.getAppSecret())
            .setAppCognitoWebDomain(TRPCore.awsConfig.getWebDomain())
            .setApplicationContext(applicationContext)
            .setIdentityProvider(provider.identity)
            .setUserPoolId(TRPCore.awsConfig.getPoolId())
            .setSignInRedirect(TRPCore.awsConfig.getRedirectLoginUrl())
            .setSignOutRedirect(TRPCore.awsConfig.getRedirectLogoutUrl())
            .setScopes(
                mutableSetOf(
                    "phone",
                    "email",
                    "openid",
                    "profile",
                    "aws.cognito.signin.user.admin"
                )
            )
        return builder
    }

    fun doLogout() {
        auth?.signOut()
    }
}

