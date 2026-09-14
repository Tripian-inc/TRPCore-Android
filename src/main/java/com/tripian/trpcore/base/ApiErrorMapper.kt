package com.tripian.trpcore.base

import com.google.gson.Gson
import com.tripian.one.util.BaseResponse
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TripianUserRepository
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.LanguageConst
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

/**
 * Translates any Throwable thrown from the repository / service layer into
 * the domain [ErrorModel] expected by ViewModels — single funnel shared by
 * every [SuspendUseCase].
 *
 * Side-effect: a refresh-token failure logs the user out. The host is
 * expected to re-call init/start through the SDK entry points.
 */
internal object ApiErrorMapper {

    private val gson by lazy { Gson() }

    fun map(
        throwable: Throwable,
        miscRepository: MiscRepository,
        tripianUserRepository: TripianUserRepository
    ): ErrorModel {
        return when (throwable) {
            is ErrorModel -> throwable

            is ConnectException,
            is SocketTimeoutException,
            is SSLHandshakeException -> ErrorModel(
                miscRepository.getLanguageValueForKey(LanguageConst.NO_NETWORK)
            )

            is HttpException -> mapHttpException(throwable, miscRepository, tripianUserRepository)

            else -> ErrorModel(
                throwable.message?.takeIf { it.isNotEmpty() }
                    ?: miscRepository.getLanguageValueForKey(LanguageConst.COMMON_ERROR)
            )
        }
    }

    private fun mapHttpException(
        e: HttpException,
        miscRepository: MiscRepository,
        tripianUserRepository: TripianUserRepository
    ): ErrorModel {
        val isRefreshTokenFailure = e.response()?.raw()?.request?.url
            ?.toString()
            ?.contains("refresh-token") == true

        if (isRefreshTokenFailure) {
            tripianUserRepository.logout()
            return ErrorModel(miscRepository.getLanguageValueForKey(LanguageConst.COMMON_ERROR))
        }

        return ErrorModel(
            serverMessage(e) ?: miscRepository.getLanguageValueForKey(LanguageConst.COMMON_ERROR)
        )
    }

    /**
     * The `message` the API put in an error body — the only text that says *why*
     * a call was rejected. Null when the throwable carries no parsable body.
     */
    fun serverMessage(throwable: Throwable): String? {
        (throwable as? ErrorModel)?.errorDesc?.takeIf { it.isNotEmpty() }?.let { return it }
        val http = throwable as? HttpException ?: return null
        return runCatching {
            gson.fromJson(http.response()?.errorBody()?.string(), BaseResponse::class.java)?.message
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }
}
