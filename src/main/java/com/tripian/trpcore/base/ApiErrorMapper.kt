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
 * Translates any Throwable thrown from the repository / service layer into the
 * domain [ErrorModel] expected by ViewModels. Mirrors the error funnel that
 * used to live inside the RxJava-based BaseUseCase.getResponseListener();
 * extracted so both the legacy and the suspend use-case bases can share it.
 *
 * Side-effect: a refresh-token failure logs the user out (legacy behaviour).
 * The host is then expected to re-call init/start through the SDK entry points.
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

        val parsedMessage = runCatching {
            gson.fromJson(e.response()?.errorBody()?.string(), BaseResponse::class.java)?.message
        }.getOrNull()

        return ErrorModel(
            parsedMessage?.takeIf { it.isNotEmpty() }
                ?: miscRepository.getLanguageValueForKey(LanguageConst.COMMON_ERROR)
        )
    }
}
