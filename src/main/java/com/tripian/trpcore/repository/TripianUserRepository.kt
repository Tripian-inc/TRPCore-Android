package com.tripian.trpcore.repository

import android.app.Application
import com.google.gson.Gson
import com.tripian.one.api.users.model.EmptyResponse
import com.tripian.one.api.users.model.ForgotPasswordRequest
import com.tripian.one.api.users.model.GuestLoginRequest
import com.tripian.one.api.users.model.LightLoginRequest
import com.tripian.one.api.users.model.LoginRequest
import com.tripian.one.api.users.model.LoginResponse
import com.tripian.one.api.users.model.RegisterRequest
import com.tripian.one.api.users.model.UpdateUserRequest
import com.tripian.one.api.users.model.User
import com.tripian.one.api.users.model.UserResponse
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.repository.authorization.AwsAuthorization
import com.tripian.trpcore.util.Strings
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class TripianUserRepository @Inject constructor(
    var app: Application,
    val service: Service,
    private val pref: Preferences,
    val gson: Gson,
    val strings: Strings,
    val awsAuthorization: AwsAuthorization
) {

    var user: User? = null

    private var userEmitter = PublishSubject.create<User>()

    fun getUser(): Observable<UserResponse> {
        return if (user == null) {
            service.getUser().map {
                user = it.data

                userEmitter.onNext(user!!)

                it
            }
        } else {
            Observable.just(UserResponse().apply {
                this.data = this@TripianUserRepository.user
                this.status = 200
            })
        }
    }

    fun getUserEmitter(): Observable<User> {
        return userEmitter
    }

    fun login(email: String, password: String): Observable<LoginResponse> {
        return service.login(LoginRequest().apply {
            this.email = email
            this.password = password
        }).map {
            it
        }
    }

    fun guestLogin(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
    ): Observable<LoginResponse> {
        return service.guestLogin(GuestLoginRequest().apply {
            this.firstName = firstName
            this.lastName = lastName
            this.email = email
            this.password = password
        }).map {
            it
        }
    }

    fun lightLogin(
        uniqueId: String,
        firstName: String?,
        lastName: String?
    ): Observable<LoginResponse> {
        return service.lightLogin(LightLoginRequest().apply {
            this.firstName = firstName
            this.lastName = lastName
            this.uniqueId = uniqueId
        }).map {
            it
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        dateOfBirth: String?,
    ): Observable<LoginResponse> {
        return service.register(RegisterRequest().apply {
            this.firstName = firstName
            this.lastName = lastName
            this.email = email
            this.password = password
            this.dateOfBirth = dateOfBirth
        }).map {
            it
        }
    }

    fun update(
        firstName: String? = null,
        lastName: String? = null,
        password: String? = null,
        dateOfBirth: String? = null,
        answers: Array<Int>? = null
    ): Observable<UserResponse> {
        return service.updateUser(UpdateUserRequest().apply {
            this.firstName = firstName
            this.lastName = lastName
            this.password = password
            this.dateOfBirth = dateOfBirth
            this.answers = if (answers.isNullOrEmpty()) arrayOf(0) else answers
        }).map {
            user = it.data

            userEmitter.onNext(user!!)

            it
        }
    }

    fun updatePassword(oldPassword: String, password: String): Observable<UserResponse> {
        return service.updateUser(UpdateUserRequest().apply {
            this.oldPassword = oldPassword
            this.password = password
        })
    }

    fun logout() {
        pref.clearAllData()
        service.logout()
        user = null
        awsAuthorization.doLogout()
    }

    fun deleteUser() {
        pref.clearAllData()
//        service.deleteUser()
        user = null
        awsAuthorization.doLogout()
    }

    fun deleteAccount(): Observable<EmptyResponse> {
        return service.deleteUser()
    }

    fun sendMail(email: String): Observable<EmptyResponse> {
        return service.sendMail(ForgotPasswordRequest().apply {
            this.email = email
        })
    }

    fun resetPassword(password: String, hash: String): Observable<EmptyResponse> {
        return service.resetPassword(ForgotPasswordRequest().apply {
            this.password = password
            this.hash = hash
        })
    }

    fun socialLogin(): Observable<EmptyResponse> {
        return service.socialLogin()
    }

//    fun getPushToken(): Observable<TokenModel> {
//        return PublishSubject.create {
//            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//                if (task.isSuccessful && !TextUtils.isEmpty(task.result)) {
//                    it.onNext(TokenModel(hms = false, task.result!!))
//                } else {
//                    it.onNext(TokenModel(hms = false, getDeviceId(pref)))
//                }
//            }
//        }
//    }
}